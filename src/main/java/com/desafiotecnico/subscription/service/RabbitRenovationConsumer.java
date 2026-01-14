package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.desafiotecnico.subscription.dto.event.RenovationEvent;

@Service
@Slf4j
@Profile("rabbit")
public class RabbitRenovationConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_RENOVATION)
    public void consumeRenovationStart(RenovationEvent event) {
        log.info("Received renovation start message via RabbitMQ: {}", event);
        log.info("Processing renovation for subscriptionId: {}", event.getSubscriptionId());
    }
}
