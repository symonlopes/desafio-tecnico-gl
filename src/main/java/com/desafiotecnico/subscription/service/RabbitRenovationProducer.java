package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.dto.event.RenovationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("rabbit")
public class RabbitRenovationProducer implements RenovationProducer {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendRenovationStart(RenovationEvent event) {
        log.info("Sending renovation start message for subscription {} via RabbitMQ", event.getSubscriptionId());

        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_RENOVATION, event);
    }
}
