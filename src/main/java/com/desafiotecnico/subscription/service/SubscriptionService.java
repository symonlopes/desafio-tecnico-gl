package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse;
import com.desafiotecnico.subscription.dto.request.SubscriptionRequest;
import com.desafiotecnico.subscription.domain.Plan;
import com.desafiotecnico.subscription.domain.RenewalTransaction;
import com.desafiotecnico.subscription.domain.PaymentTransactionStatus;

import com.desafiotecnico.subscription.dto.event.SubscriptionRenewalStartEvent;
import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.error.CodedException;
import com.desafiotecnico.subscription.error.FatalProcessingException;
import com.desafiotecnico.subscription.error.UnavailableGatewayException;
import com.desafiotecnico.subscription.producers.SubscriptionRenewalProducer;
import com.desafiotecnico.subscription.repository.PaymentTransactionRepository;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import com.desafiotecnico.subscription.repository.UserRepository;
import com.desafiotecnico.subscription.dto.event.TransactionCancelEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import com.desafiotecnico.subscription.dto.gateway.PaymentGatewayInitialResponse;
import com.desafiotecnico.subscription.dto.gateway.PaymentGatewayRequest;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionRenewalProducer subscriptionRenewalProducer;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RestTemplate restTemplate;

    @Value("${integration.payments.url}")
    private String paymentUrl;

    @Value("${declined.payment.retry.interval.in.seconds}")
    private Long declinedPaymentRetryIntervalInSeconds;

    @Transactional
    public Subscription createSubscription(SubscriptionRequest request) {
        log.info("Creating subscription for user {}", request.getUserId());

        if (!userRepository.existsById(request.getUserId())) {
            throw new CodedException("USER_NOT_FOUND", "Usuário não encontrado.");
        }

        // Simple validation: check if user already has an active subscription
        // (expiration date > today)
        if (subscriptionRepository.findFirstByUserIdAndExpirationDateAfter(request.getUserId(), LocalDate.now())
                .isPresent()) {
            throw new CodedException("ACTIVE_SUBSCRIPTION_EXISTS", "Usuário já possui uma assinatura ativa.");
        }

        Plan planEnum;
        try {
            planEnum = Plan.fromName(request.getPlan());
        } catch (IllegalArgumentException e) {
            throw new CodedException("INVALID_PLAN", "Plano inválido.");
        }

        Subscription subscription = Subscription.builder()
                .userId(request.getUserId())
                .status(SubscriptionStatus.ATIVA)
                .plan(request.getPlan())
                .priceInCents(planEnum.getPriceInCents())
                .startDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusMonths(1))
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void cancelSubscription(UUID subscriptionId) {
        try {

            log.info("Cencelando inscrição {}", subscriptionId);
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

            subscription.setStatus(SubscriptionStatus.CANCELADA);
            subscriptionRepository.save(subscription);
        } catch (Exception e) {
            log.error("Erro ao processar cancelamento de inscrição: ", e);
        }
    }

    @Transactional
    public void processPaymentCallback(PaymentGatewayResponse event) {
        log.info("Processing payment callback event for transaction {}", event.getTransactionId());

        // Se a transação
        var transaction = paymentTransactionRepository.findById(event.getTransactionId())
                .orElseThrow(() -> new CodedException("TRANSACTION_NOT_FOUND", "Transaction not found"));

        if (event.isSuccess()) {
            log.info("Payment successful for transaction {}", event.getTransactionId());
            transaction.setStatus(PaymentTransactionStatus.APPROVED.name());
            transaction.setDataFinalizacao(java.time.LocalDateTime.now());
            paymentTransactionRepository.save(transaction);

            // Deve atualar a Subscription para a próxima data de vencimento
            subscriptionRepository.findById(transaction.getSubscription().getId())
                    .ifPresent(sub -> {
                        sub.setExpirationDate(sub.getExpirationDate().plusMonths(1));
                        sub.setLastRenewalDate(LocalDate.now());
                        subscriptionRepository.save(sub);
                    });

        }

        if (!event.isSuccess()) {

            log.warn("Falha no processamento do pagamento para a transação com ID {} na inscrção {}, erro: {}] ",
                    event.getTransactionId(), event.getTransactionId(), event.getMessage());

            int attempts = transaction.getPaymentErrorCount() + 1;
            transaction.setPaymentErrorCount(attempts);

            if (attempts < 3) {
                log.info("Retrying payment for subscription {}. Attempt {}/3", transaction.getSubscription().getId(),
                        attempts);
                transaction.setStatus(PaymentTransactionStatus.PENDING_RETRY.name());
                paymentTransactionRepository.save(transaction);
                subscriptionRenewalProducer.sendRenewalStart(SubscriptionRenewalStartEvent.builder()
                        .subscriptionId(transaction.getSubscription().getId())
                        .transactionId(transaction.getId())
                        .priceInCents(transaction.getSubscription().getPriceInCents())
                        .build(), declinedPaymentRetryIntervalInSeconds * 1000);
            }

            if (attempts >= 3) {
                log.error("Max payment attempts reached for subscription {}. Cancelling.",
                        transaction.getSubscription().getId());
                transaction.setStatus(PaymentTransactionStatus.DECLINED.name());
                transaction.setDataFinalizacao(LocalDateTime.now());
                paymentTransactionRepository.save(transaction);

                subscriptionRenewalProducer.sendCancelSubscription(
                        com.desafiotecnico.subscription.dto.event.SubscriptionCancelEvent.builder()
                                .subscriptionId(transaction.getSubscription().getId())
                                .reason("Máximo de tentativas de processamento de pagamento foi atingido.")
                                .build());

                subscriptionRenewalProducer.sendCancelTransaction(TransactionCancelEvent.builder()
                        .transactionId(transaction.getId())
                        .subscriptionId(transaction.getSubscription().getId())
                        .reason("Máximo de tentativas de processamento de pagamento foi atingido.")
                        .occurredAt(java.time.LocalDateTime.now())
                        .build());
            }
        }
    }

    public void startPaymentTransaction(SubscriptionRenewalStartEvent event) {
        log.info("Processando renovação. Subscription: {}, Transaction: {}",
                event.getSubscriptionId(), event.getTransactionId());

        // 1. Recuperar a transação e garantir Idempotência
        var transaction = paymentTransactionRepository.findById(event.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transação não encontrada no banco."));

        // Se já estiver Aprovada ou Recusada, não processa de novo (Proteção contra
        // duplicidade do RabbitMQ)
        if (isFinalStatus(transaction.getStatus())) {
            log.warn("Transação {} já processada com status {}. Ignorando.", transaction.getId(),
                    transaction.getStatus());
            return;
        }

        try {
            updatePaymentTransactionStatus(transaction, PaymentTransactionStatus.PROCESSING);

            var gatewayRequest = PaymentGatewayRequest.builder()
                    .amount(event.getPriceInCents())
                    .customId(event.getTransactionId())
                    .build();

            log.info("Chamando Gateway de Pagamento...");
            var response = restTemplate.postForObject(paymentUrl, new HttpEntity<>(gatewayRequest),
                    PaymentGatewayInitialResponse.class);

            if (response != null && response.getCustomId() != null) {
                log.info("Pagamento aceito pelo Gateway. External ID: {}", response.getCustomId());
                updatePaymentTransactionStatus(transaction, PaymentTransactionStatus.WAITING_PAYMENT_PROCESS_RESPONSE);
            }

        } catch (HttpClientErrorException e) {
            // 5. ERROS DE NEGÓCIO (4xx - Cartão sem saldo, Dados inválidos)
            // NÃO RETENTAR. O Gateway já disse que não vai passar.
            log.error("Dados de pagamento recusados pelo gateway (4xx). Finalizando.", e);
            updatePaymentTransactionStatus(transaction, PaymentTransactionStatus.ABORTED);
        } catch (HttpServerErrorException | ResourceAccessException e) {
            // 6. ERROS TÉCNICOS (5xx, Timeout, Rede)
            // RETENTAR. O Gateway está fora ou instável.
            log.warn("Gateway instável. Marcando para Retry.", e);
            throw new UnavailableGatewayException("Gateway indisponível temporariamente", e);
        } catch (Exception e) {
            log.error("Erro fatal/interno no processamento.", e);
            updatePaymentTransactionStatus(transaction, PaymentTransactionStatus.ABORTED);
            throw new FatalProcessingException("Erro irrecuperável: " + e.getMessage(), e);
        }
    }

    private boolean isFinalStatus(String status) {
        return PaymentTransactionStatus.APPROVED.name().equals(status)
                || PaymentTransactionStatus.DECLINED.name().equals(status)
                || PaymentTransactionStatus.ABORTED.name().equals(status);
    }

    private void updatePaymentTransactionStatus(RenewalTransaction transaction, PaymentTransactionStatus processing) {
        transaction.setStatus(processing.name());
        paymentTransactionRepository.save(transaction);
    }
}
