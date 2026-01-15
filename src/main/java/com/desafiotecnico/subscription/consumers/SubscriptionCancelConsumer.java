package com.desafiotecnico.subscription.consumers;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.dto.event.SubscriptionCancelEvent;
import com.desafiotecnico.subscription.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionCancelConsumer {

    private final SubscriptionService subscriptionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SUBSCRIPTION_CANCEL)
    public void consumeSubscriptionCancel(SubscriptionCancelEvent event) {
        log.info("Mensagem de cancelamento de inscrição recebida: {} Reason: {}",
                event.getSubscriptionId(),
                event.getReason());

        subscriptionService.cancelSubscription(event.getSubscriptionId());

    }
}
