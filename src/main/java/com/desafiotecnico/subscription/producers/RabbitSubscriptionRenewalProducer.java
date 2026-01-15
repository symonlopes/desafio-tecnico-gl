package com.desafiotecnico.subscription.producers;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse;
import com.desafiotecnico.subscription.dto.event.SubscriptionCancelEvent;
import com.desafiotecnico.subscription.dto.event.SubscriptionRenewalStartEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitSubscriptionRenewalProducer implements SubscriptionRenewalProducer {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendRenewalStart(SubscriptionRenewalStartEvent event) {
        log.info("Sending renewal start message for subscription {} via RabbitMQ", event.getSubscriptionId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SUBSCRIPTION,
                RabbitMQConfig.QUEUE_SUBSCRIPTION_RENEWAL_START, event);
    }

    @Override
    public void sendRenewalStart(SubscriptionRenewalStartEvent event, long delayMs) {
        log.info("Sending renewal start message for subscription {} via RabbitMQ with delay {}ms",
                event.getSubscriptionId(), delayMs);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SUBSCRIPTION,
                RabbitMQConfig.QUEUE_SUBSCRIPTION_RENEWAL_START, event, message -> {
                    message.getMessageProperties().setHeader("x-delay", delayMs);
                    return message;
                });
    }

    @Override
    public void sendCancelSubscription(SubscriptionCancelEvent event) {
        log.info("Sending cancel subscription message for subscription {} via RabbitMQ", event.getSubscriptionId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SUBSCRIPTION, RabbitMQConfig.QUEUE_SUBSCRIPTION_CANCEL,
                event);
    }

    @Override
    public void sendPaymentResponse(PaymentGatewayResponse event) {
        log.info("Sending payment response message for transaction {} via RabbitMQ", event.getTransactionId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SUBSCRIPTION,
                RabbitMQConfig.QUEUE_PAYMENT_GATEWAY_RESPONSE, event);
    }

    @Override
    public void sendCancelTransaction(com.desafiotecnico.subscription.dto.event.TransactionCancelEvent event) {
        log.info("Sending cancel transaction message for transaction {} via RabbitMQ", event.getTransactionId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SUBSCRIPTION,
                RabbitMQConfig.QUEUE_TRANSACTION_CANCEL, event);
    }
}
