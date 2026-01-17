package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.PaymentTransactionStatus;
import com.desafiotecnico.subscription.domain.PaymentTransaction;
import com.desafiotecnico.subscription.dto.event.PaymentTransactionEvent;
import com.desafiotecnico.subscription.dto.event.SubscriptionCancelEvent;
import com.desafiotecnico.subscription.dto.gateway.PaymentGatewayRequest;
import com.desafiotecnico.subscription.dto.gateway.PaymentGatewayResponse;

import com.desafiotecnico.subscription.repository.PaymentTransactionRepository;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final SubscriptionService subscriptionService;

    @Value("${integration.payments.url}")
    private String paymentUrl;

    @Value("${declined.payment.retry.interval.in.seconds}")
    private Integer declinedPaymentRetryIntervalInSeconds;

    @Transactional
    public void cancelTransaction(UUID transactionId, String reason, PaymentTransactionStatus status) {
        // Chamado pelo PaymentTransactionCancelConsumer
        log.info("Cancelando transação {} com motivo: {}", transactionId, reason);

        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada com ID: " + transactionId));

        transaction.setStatus(status.name());
        transaction.setCancellationReason(reason);
        transaction.setDataFinalizacao(LocalDateTime.now());

        paymentTransactionRepository.save(transaction);
        log.info("Transação {} cancelada com sucesso", transactionId);
    }

    protected Optional<PaymentTransaction> isValidForProcessing(PaymentTransactionEvent event) {
        var transactionOpt = paymentTransactionRepository.findById(event.getTransactionId());

        if (transactionOpt.isEmpty()) {
            log.warn("Transação {} não encontrada no banco. Descartando mensagem.", event.getTransactionId());
            return Optional.empty();
        }

        var transaction = transactionOpt.get();

        return Optional.of(transaction);
    }

    public void startPaymentTransaction(PaymentTransactionEvent event) {

        log.info("Processando renovação. Subscription: {}, Transaction: {}",
                event.getSubscriptionId(), event.getTransactionId());

        var transactionOpt = isValidForProcessing(event);

        if (transactionOpt.isEmpty()) {
            return;
        }

        try {

            updateStatus(transactionOpt.get(), PaymentTransactionStatus.PROCESSING);

            // Aqui teríamos outras informações como produto, cpf, etc.
            var gatewayRequest = PaymentGatewayRequest.builder()
                    .amount(event.getPriceInCents())
                    .customId(event.getTransactionId())
                    .build();

            var response = restTemplate.postForObject(paymentUrl, new HttpEntity<>(gatewayRequest),
                    PaymentGatewayResponse.class);

            if (response != null && response.getCustomId() != null) {
                handleSuccess(transactionOpt.get(), event.getSubscriptionId());
            }

        } catch (HttpClientErrorException e) {
            log.warn("Pagamento RECUSADO pelo gateway (4xx). {}", e.getMessage());

            // 4. Tratamento de Erro de Cliente (Lógica de Retry ou Falha Final)
            handleClientError(event, e.getMessage());

        } catch (HttpServerErrorException | ResourceAccessException e) {
            log.warn("Gateway instável. Marcando para Retry.", e);
            reenqueWithDelay(event, 10);
        } catch (Exception e) {
            log.error("Erro fatal/interno no processamento.", e);
            updateStatus(transactionOpt.get(), PaymentTransactionStatus.ABORTED);
            throw new AmqpRejectAndDontRequeueException("Erro desconhecido: " + e.getMessage(), e);
        }
    }

    @Transactional
    protected void handleSuccess(PaymentTransaction transaction, UUID subscriptionId) {
        updateStatus(transaction, PaymentTransactionStatus.APPROVED);
        subscriptionService.renewSubscription(subscriptionId);
        log.info("Transação {} aprovada e assinatura renovada.", transaction.getId());
    }

    @Transactional
    protected void handleClientError(PaymentTransactionEvent event, String errorMessage) {
        var transaction = paymentTransactionRepository.findById(event.getTransactionId())
                .orElseThrow();

        if (event.getRejectedPaymentCount() < 3) {
            // log.warn("Tentando novamente... tentativa {}/3",
            // event.getRejectedPaymentCount() + 1);
            updateStatus(transaction, PaymentTransactionStatus.PENDING_RETRY);
            event.setRejectedPaymentCount(event.getRejectedPaymentCount() + 1);
            reenqueWithDelay(event, declinedPaymentRetryIntervalInSeconds);
        } else {
            log.warn("Tentativas esgotadas para transação {}.", event.getTransactionId());
            updateStatus(transaction, PaymentTransactionStatus.DECLINED, errorMessage);
            SubscriptionCancelEvent cancelEvent = SubscriptionCancelEvent.builder()
                    .subscriptionId(event.getSubscriptionId())
                    .reason("Pagamento recusado após máximo de tentativas: " + errorMessage)
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SUBSCRIPTION,
                    RabbitMQConfig.QUEUE_SUBSCRIPTION_CANCEL, cancelEvent);
        }
    }

    private void reenqueWithDelay(PaymentTransactionEvent event, Integer delayInSeconds) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SUBSCRIPTION,
                RabbitMQConfig.QUEUE_SUBSCRIPTION_RENEWAL_START, event, message -> {
                    message.getMessageProperties().setHeader("x-delay",
                            delayInSeconds * 1000);
                    return message;
                });
    }

    private boolean isFinalStatus(String status) {
        return PaymentTransactionStatus.APPROVED.name().equals(status)
                || PaymentTransactionStatus.DECLINED.name().equals(status)
                || PaymentTransactionStatus.ABORTED.name().equals(status);
    }

    @Transactional
    protected void updateStatus(PaymentTransaction transaction, PaymentTransactionStatus processing) {
        transaction.setStatus(processing.name());
        if (isFinalStatus(transaction.getStatus())) {
            transaction.setDataFinalizacao(LocalDateTime.now());
        }
        paymentTransactionRepository.save(transaction);
    }

    private void updateStatus(PaymentTransaction transaction, PaymentTransactionStatus status,
            String cancellationReason) {
        transaction.setStatus(status.name());
        transaction.setCancellationReason(cancellationReason);
        if (isFinalStatus(transaction.getStatus())) {
            transaction.setDataFinalizacao(LocalDateTime.now());
        }
        paymentTransactionRepository.save(transaction);
    }
}
