package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse;
import com.desafiotecnico.subscription.dto.request.SubscriptionRequest;
import com.desafiotecnico.subscription.domain.Plan;
import com.desafiotecnico.subscription.domain.TransactionStatus;

import com.desafiotecnico.subscription.dto.event.SubscriptionRenewalStartEvent;
import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.error.CodedException;
import com.desafiotecnico.subscription.error.UnavailableGatewayException;
import com.desafiotecnico.subscription.producers.SubscriptionRenewalProducer;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import com.desafiotecnico.subscription.repository.UserRepository;
import com.desafiotecnico.subscription.dto.event.TransactionCancelEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import com.desafiotecnico.subscription.dto.gateway.PaymentGatewayInitialResponse;
import com.desafiotecnico.subscription.dto.gateway.PaymentGatewayRequest;
import org.springframework.web.client.HttpClientErrorException.BadRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionRenewalProducer subscriptionRenewalProducer;
    private final com.desafiotecnico.subscription.repository.RenewalTransactionRepository renewalTransactionRepository;
    private final RestTemplate restTemplate;

    @Value("${integration.payments.url}")
    private String paymentUrl;

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
        var transaction = renewalTransactionRepository.findById(event.getTransactionId())
                .orElseThrow(() -> new CodedException("TRANSACTION_NOT_FOUND", "Transaction not found"));

        if (event.isSuccess()) {
            log.info("Payment successful for transaction {}", event.getTransactionId());
            transaction.setStatus(TransactionStatus.RENEWED.name());
            transaction.setDataFinalizacao(java.time.LocalDateTime.now());
            renewalTransactionRepository.save(transaction);

            // Deve atualar a Subscription para a próxima data de vencimento
            subscriptionRepository.findById(transaction.getSubscription().getId())
                    .ifPresent(sub -> {
                        sub.setExpirationDate(sub.getExpirationDate().plusMonths(1));
                        sub.setLastRenewalDate(LocalDate.now());
                        subscriptionRepository.save(sub);
                    });

        }

        if (!event.isSuccess()) {

            log.warn("Falha no processamento do pagamento para a transação com ID {} na inscrção {} ",
                    event.getTransactionId(), event.getTransactionId());

            int attempts = transaction.getPaymentAttempts() + 1;
            transaction.setPaymentAttempts(attempts);

            if (attempts < 3) {
                log.info("Retrying payment for subscription {}. Attempt {}/3", transaction.getSubscription().getId(),
                        attempts);
                transaction.setStatus(TransactionStatus.WAITING_RETRY.name());
                renewalTransactionRepository.save(transaction);

                // Nesse ponto, essa mensagem deve ser enviada para ser consumida em 10
                // segundos.
                subscriptionRenewalProducer.sendRenewalStart(SubscriptionRenewalStartEvent.builder()
                        .subscriptionId(transaction.getSubscription().getId())
                        .transactionId(transaction.getId())
                        .priceInCents(transaction.getSubscription().getPriceInCents())
                        .build(), 10_000L);
            }

            if (attempts >= 3) {
                log.error("Max payment attempts reached for subscription {}. Cancelling.",
                        transaction.getSubscription().getId());
                transaction.setStatus("FAILED"); // Adding FAILED status
                transaction.setDataFinalizacao(java.time.LocalDateTime.now());
                renewalTransactionRepository.save(transaction);

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

    public void processSubscriptionStartRenewal(SubscriptionRenewalStartEvent event) {
        log.info("Processando mensagem de renovação da inscrição {}. Id de transação: {}", event.getSubscriptionId(),
                event.getTransactionId());

        try {
            var gatewayRequest = PaymentGatewayRequest.builder()
                    .amount(event.getPriceInCents())
                    .customId(event.getTransactionId())
                    .build();

            var request = new HttpEntity<>(gatewayRequest);
            log.info("Enviando request inicial para processamento de pagamento: {}", paymentUrl);
            var response = restTemplate.postForObject(paymentUrl, request,
                    PaymentGatewayInitialResponse.class);

            if (response != null && response.getExternalId() != null) {
                log.info("Pedido inicial de processamento de pagamento enviado com sucesso. External ID: {}",
                        response.getExternalId());
            }

        } catch (BadRequest e) {
            var errorResponse = e.getResponseBodyAs(PaymentGatewayInitialResponse.class);
            if (errorResponse != null) {
                log.error("Requisição inicial de pagamento falhou com código de erro: {} e descrição: {}",
                        errorResponse.getErrorCode(),
                        errorResponse.getDescription());
            } else {
                log.error("Requisição inicial de pagamento falhou com estrutura de erro desconhecida");
            }
        } catch (Exception e) {
            log.error("Erro ao processar requisição inicial de pagamento", e);
            throw new UnavailableGatewayException("Gateway indisponível: " + e.getMessage());
        }
    }
}
