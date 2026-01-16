package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.PaymentTransactionStatus;
import com.desafiotecnico.subscription.domain.PaymentTransaction;
import com.desafiotecnico.subscription.dto.event.PaymentTransactionEvent;
import com.desafiotecnico.subscription.dto.event.SubscriptionCancelEvent;
import com.desafiotecnico.subscription.dto.gateway.PaymentGatewayRequest;
import com.desafiotecnico.subscription.dto.gateway.PaymentGatewayResponse;

import com.desafiotecnico.subscription.repository.PaymentTransactionRepository;

import jakarta.persistence.EntityNotFoundException;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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

    // Usado pelo PaymentTransactionCancelConsumer
    @Transactional
    public void cancelTransaction(UUID transactionId, String reason, PaymentTransactionStatus status) {
        log.info("Cancelando transação {} com motivo: {}", transactionId, reason);

        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada com ID: " + transactionId));

        transaction.setStatus(status.name());
        transaction.setCancellationReason(reason);
        transaction.setDataFinalizacao(LocalDateTime.now());

        paymentTransactionRepository.save(transaction);
        log.info("Transação {} cancelada com sucesso", transactionId);
    }

    @Transactional
    // @Retryable(retryFor = EntityNotFoundException.class, maxAttempts = 3, backoff
    // = @Backoff(delay = 200, multiplier = 2))
    public void startPaymentTransaction(PaymentTransactionEvent event) {

        log.info("Processando renovação. Subscription: {}, Transaction: {}",
                event.getSubscriptionId(), event.getTransactionId());

        var transaction = paymentTransactionRepository.findById(event.getTransactionId());

        if (transaction.isEmpty()) {
            log.warn("Transação {} não encontrada no banco.", event.getTransactionId());
            throw new EntityNotFoundException("Transação " + event.getTransactionId() + " não encontrada no banco.");
        }

        if (isFinalStatus(transaction.get().getStatus())) {
            log.warn("Transação {} já processada com status {}. Ignorando.", transaction.get().getId(),
                    transaction.get().getStatus());
            return;
        }

        try {
            updateStatus(transaction.get(), PaymentTransactionStatus.PROCESSING);

            // Aqui teríamos outras informações como produto, cpf, etc.
            var gatewayRequest = PaymentGatewayRequest.builder()
                    .amount(event.getPriceInCents())
                    .customId(event.getTransactionId())
                    .build();

            var response = restTemplate.postForObject(paymentUrl, new HttpEntity<>(gatewayRequest),
                    PaymentGatewayResponse.class);

            if (response != null && response.getCustomId() != null) {
                updateStatus(transaction.get(), PaymentTransactionStatus.APPROVED);
                // Atualizar a assinatura:
                subscriptionService.renewSubscription(event.getSubscriptionId());
            }

        } catch (HttpClientErrorException e) {
            log.warn("Pagamento RECUSADO pelo gateway (4xx).", e.getMessage());

            if (event.getRejectedPaymentCount() < 3) {
                log.info("Tentando novamente... tentativa {}/3", event.getRejectedPaymentCount() + 1);
                updateStatus(transaction.get(), PaymentTransactionStatus.PENDING_RETRY);
                event.setRejectedPaymentCount(event.getRejectedPaymentCount() + 1);
                reenqueWithDelay(event, declinedPaymentRetryIntervalInSeconds);
            } else {
                log.error("Tentativas esgotadas de pagamento esgotadas para transação {}.", event.getTransactionId());

                // Publica evento de cancelamento da assinatura
                SubscriptionCancelEvent cancelEvent = SubscriptionCancelEvent.builder()
                        .subscriptionId(event.getSubscriptionId())
                        .reason("Pagamento recusado após máximo de tentativas: " + e.getMessage())
                        .build();

                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SUBSCRIPTION,
                        RabbitMQConfig.QUEUE_SUBSCRIPTION_CANCEL, cancelEvent);

                updateStatus(transaction.get(), PaymentTransactionStatus.DECLINED, e.getMessage());
            }

        } catch (HttpServerErrorException | ResourceAccessException e) {
            log.warn("Gateway instável. Marcando para Retry.", e);
            reenqueWithDelay(event, 10);
        } catch (Exception e) {
            log.error("Erro fatal/interno no processamento.", e);
            updateStatus(transaction.get(), PaymentTransactionStatus.ABORTED);
            throw new AmqpRejectAndDontRequeueException("Erro desconhecido: " + e.getMessage(), e);
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

    private void updateStatus(PaymentTransaction transaction, PaymentTransactionStatus processing) {
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
