package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import com.desafiotecnico.subscription.dto.request.SubscriptionRequest;
import com.desafiotecnico.subscription.domain.RenewalStatus;
import com.desafiotecnico.subscription.domain.RenewalTransaction;
import com.desafiotecnico.subscription.dto.event.RenovationEvent;
import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.error.CodedException;
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
    private final RenovationProducer renovationProducer;
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

        Subscription subscription = Subscription.builder()
                .userId(request.getUserId())
                .status(SubscriptionStatus.ATIVA)
                .plan(request.getPlan())
                .startDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusMonths(1))
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void cancelSubscription(UUID subscriptionId) {
        log.info("Canceling subscription {}", subscriptionId);
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        subscription.setStatus(SubscriptionStatus.CANCELADA);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void triggerRenovation(int batchSize, LocalDate dateToProccess) {
        log.info("Disparando processo de renovação para no máximo {} registros, considerando a data {}.",
                batchSize, dateToProccess);

        var subscriptions = subscriptionRepository.findPendingRenovations(
                dateToProccess,
                PageRequest.of(0, batchSize));

        log.info("Foram encontrados {} registros de incrições que serão renovadas.", subscriptions.size());

        subscriptions.forEach(sub -> {
            var transaction = RenewalTransaction.builder()
                    .subscription(sub)
                    .status(RenewalStatus.NEW.getName())
                    .dataInicio(java.time.LocalDateTime.now())
                    .paymentAttempts(0)
                    .build();
            renewalTransactionRepository.save(transaction);

            // Deve enviar somente o subscriptionId e o transactionId
            renovationProducer.sendRenovationStart(RenovationEvent.builder()
                    .subscriptionId(sub.getId())
                    .transactionId(transaction.getId())
                    .build());
        });

    }

}
