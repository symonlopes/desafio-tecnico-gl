package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse;
import com.desafiotecnico.subscription.error.UnavailableGatewayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackConsumer {

    private final SubscriptionService subscriptionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_GATEWAY_RESPONSE)
    public void receivePaymentResponse(PaymentGatewayResponse event) {
        log.info("Payment response received via RabbitMQ for transaction: {}", event.getTransactionId());
        try {
            subscriptionService.processPaymentCallback(event);
        } catch (UnavailableGatewayException e) {
            log.warn("Gateway unavailable during payment callback processing for transaction {}. Message will be retried.", event.getTransactionId());
            throw e; // Spring AMQP retry mechanism will handle this
        }
    }
}
