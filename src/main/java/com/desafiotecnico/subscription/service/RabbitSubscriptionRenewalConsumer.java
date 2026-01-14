package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.dto.event.SubscriptionRenewalStartEvent;
import com.desafiotecnico.subscription.error.UnavailableGatewayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitSubscriptionRenewalConsumer {

    private final SubscriptionService subscriptionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SUBSCRIPTION_RENEWAL_START)
    public void consumeRenovationStart(SubscriptionRenewalStartEvent event) {
        log.info("Received renovation start message via RabbitMQ: {}", event);
        try {
            subscriptionService.processSubscriptionStartRenewal(event);
        } catch (UnavailableGatewayException e) {
            log.warn("Gateway unavailable during renovation processing for subscription {}. Message will be retried.",
                    event.getSubscriptionId());
            throw e; // Spring AMQP retry mechanism will handle this
        }
    }
}
