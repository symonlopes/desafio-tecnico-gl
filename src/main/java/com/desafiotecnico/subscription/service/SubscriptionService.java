package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import com.desafiotecnico.subscription.dto.request.NewSignatureRequest;
import com.desafiotecnico.subscription.error.CodedException;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
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

    @Transactional
    public Subscription createSubscription(NewSignatureRequest request) {
        log.info("Creating subscription for user {}", request.getUsuarioId());

        var activeSubscriptions = subscriptionRepository.findActiveValidSubscriptions(request.getUsuarioId());

        if (!activeSubscriptions.isEmpty()) {
            throw new CodedException("ACTIVE_SUBSCRIPTION_EXISTS", "Usuário já possui uma assinatura ativa.");
        }

        var subscription = Subscription.builder()
                .userId(request.getUsuarioId())
                .status(request.getStatus())
                .plan(request.getPlano().getName())
                .autoRenew(true)
                .priceInCents(request.getPlano().getPriceInCents())
                .startDate(request.getDataInicio())
                .expirationDate(request.getDataExpiracao())
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void cancelSubscription(UUID subscriptionId, String reason) {
        try {
            log.info("Cancelando inscrição {}", subscriptionId);

            var subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

            subscription.setAutoRenew(false);
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
