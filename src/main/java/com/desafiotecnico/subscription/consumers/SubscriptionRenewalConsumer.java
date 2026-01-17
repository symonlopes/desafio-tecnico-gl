package com.desafiotecnico.subscription.consumers;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.dto.event.PaymentTransactionEvent;
import com.desafiotecnico.subscription.service.PaymentTransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalConsumer {

    private final PaymentTransactionService paymentTransactionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SUBSCRIPTION_RENEWAL_START, concurrency = "${subscription.renewal.consumer.concurrency}")
    public void consumePaymentTransactionStartationStart(PaymentTransactionEvent event) {
        paymentTransactionService.startPaymentTransaction(event);
    }
}
