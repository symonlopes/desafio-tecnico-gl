package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.SubscriptionStatus;

import com.desafiotecnico.subscription.dto.request.SubscriptionRequest;
import com.desafiotecnico.subscription.domain.Plan;
import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.error.CodedException;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import com.desafiotecnico.subscription.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
                .autoRenew(true)
                .priceInCents(planEnum.getPriceInCents())
                .startDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusMonths(1))
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void cancelSubscription(UUID subscriptionId, String reason) {
        try {

            log.info("Cencelando inscrição {}", subscriptionId);

            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

            subscription.setStatus(SubscriptionStatus.CANCELADA);
            subscription.setCancelReason(reason);
            subscriptionRepository.save(subscription);
        } catch (Exception e) {
            log.error("Erro ao processar cancelamento de inscrição: ", e);
        }
    }

    @Transactional
    public void renewSubscription(UUID subscriptionId) {
        log.info("Renovando assinatura {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        LocalDate newExpirationDate;
        if (subscription.getExpirationDate().isBefore(LocalDate.now())) {
            newExpirationDate = LocalDate.now().plusMonths(1);
        } else {
            newExpirationDate = subscription.getExpirationDate().plusMonths(1);
        }

        subscription.setExpirationDate(newExpirationDate);
        subscription.setLastRenewalDate(LocalDate.now());
        subscription.setStatus(SubscriptionStatus.ATIVA);

        subscriptionRepository.save(subscription);
        log.info("Assinatura {} renovada com sucesso. Nova validade: {}", subscriptionId, newExpirationDate);
    }
}
