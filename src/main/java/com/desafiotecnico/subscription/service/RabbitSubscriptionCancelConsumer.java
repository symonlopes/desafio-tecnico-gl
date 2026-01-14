package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.dto.event.SubscriptionCancelEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitSubscriptionCancelConsumer {

    private final SubscriptionService subscriptionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SUBSCRIPTION_CANCEL)
    public void consumeSubscriptionCancel(SubscriptionCancelEvent event) {
        log.info("Mensagem de cancelamento de subscription cancel message via RabbitMQ: {} Reason: {}", event.getSubscriptionId(),
                event.getReason());
        try {
            subscriptionService.cancelSubscription(event.getSubscriptionId());
        } catch (Exception e) {
            log.error("Error processing subscription cancel for {}: {}", event.getSubscriptionId(), e.getMessage());
            // Depending on the requirement, we might want to throw to retry or just log.
            // For cancellation, infinite retry might not be desired if it's a logic error,
            // but transient error should retry.
            // Spring AMQP default is to retry if exception is thrown.
            throw e;
        }
    }
}
