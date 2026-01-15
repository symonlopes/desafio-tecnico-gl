package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse;
import com.desafiotecnico.subscription.dto.request.SubscriptionRequest;
import com.desafiotecnico.subscription.domain.Plan;
import com.desafiotecnico.subscription.domain.RenewalStatus;
import com.desafiotecnico.subscription.domain.RenewalTransaction;
import com.desafiotecnico.subscription.dto.event.SubscriptionRenewalStartEvent;
import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.error.CodedException;
import com.desafiotecnico.subscription.error.UnavailableGatewayException;
import com.desafiotecnico.subscription.producers.SubscriptionRenewalProducer;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import com.desafiotecnico.subscription.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionRenewalProducer subscriptionRenewalProducer;
    private final com.desafiotecnico.subscription.repository.RenewalTransactionRepository renewalTransactionRepository;

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
    public void triggerRenovation(int batchSize, LocalDate dateToProccess) {
        log.info("Disparando processo de renovação para no máximo {} registros, considerando a data {}.",
                batchSize, dateToProccess);

        var subscriptions = subscriptionRepository.findSubscriptionToProccessPayment(
                dateToProccess,
                PageRequest.of(0, batchSize));

        log.info("Foram encontrados {} registros de incrições que serão renovadas.", subscriptions.size());

        subscriptions.forEach(sub -> {

            var transaction = RenewalTransaction.builder()
                    .subscription(sub)
                    .status(RenewalStatus.NEW.getName())
                    .dataInicio(java.time.LocalDateTime.now())
                    .paymentAttempts(0)
                    .priceInCents(sub.getPriceInCents())
                    .build();

            renewalTransactionRepository.save(transaction);

            subscriptionRenewalProducer.sendRenewalStart(SubscriptionRenewalStartEvent.builder()
                    .subscriptionId(sub.getId())
                    .transactionId(transaction.getId())
                    .priceInCents(sub.getPriceInCents())
                    .build());
        });

    }

    @Transactional
    public void processPaymentCallback(PaymentGatewayResponse event) {
        log.info("Processing payment callback event for transaction {}", event.getTransactionId());

        // Se a transação
        var transaction = renewalTransactionRepository.findById(event.getTransactionId())
                .orElseThrow(() -> new CodedException("TRANSACTION_NOT_FOUND", "Transaction not found"));

        if (event.isSuccess()) {
            log.info("Payment successful for transaction {}", event.getTransactionId());
            transaction.setStatus(RenewalStatus.RENEWED.getName());
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
                transaction.setStatus(RenewalStatus.WAITING_RETRY.getName());
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
            }
        }
    }

    public void processSubscriptionStartRenewal(SubscriptionRenewalStartEvent event) {
        log.info("Processando mensagem de renovação da inscrição {}. Id de transação: {}", event.getSubscriptionId(),
                event.getTransactionId());
        // throw new UnavailableGatewayException("Simulando gateway fora do ar...");
    }
}
