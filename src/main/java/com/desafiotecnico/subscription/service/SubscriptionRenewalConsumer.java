package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.dto.event.SubscriptionRenewalStartEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalConsumer {

    private final SubscriptionService subscriptionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SUBSCRIPTION_RENEWAL_START)
    public void consumeRenovationStart(SubscriptionRenewalStartEvent event) {
        log.info("Mensagem de QUEUE_SUBSCRIPTION_RENEWAL_START recebida via RabbitMQ: {}", event);
        subscriptionService.processSubscriptionStartRenewal(event);
    }
}
