package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.controller.SubscriptionRequest;
import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public Subscription createSubscription(SubscriptionRequest request) {
        log.info("Creating subscription for user {}", request.getUserId());

        // Simple validation: check if user already has an active subscription
        if (subscriptionRepository.findByUserIdAndStatus(request.getUserId(), SubscriptionStatus.ATIVA).isPresent()) {
            throw new IllegalStateException("User already has an active subscription");
        }

        Subscription subscription = Subscription.builder()
                .userId(request.getUserId())
                .plan(request.getPlan())
                .startDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusMonths(1))
                .status(SubscriptionStatus.ATIVA)
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

    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    public void checkRenewals() {
        log.info("Checking for subscriptions to renew...");
        // TODO: Implement renewal logic
    }
}
