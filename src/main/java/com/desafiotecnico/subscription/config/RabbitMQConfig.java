package com.desafiotecnico.subscription.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_SUBSCRIPTION = "subscription.exchange";
    public static final String QUEUE_SUBSCRIPTION_RENEWAL_START = "subscription.renewal.start";
    public static final String QUEUE_SUBSCRIPTION_CANCEL = "subscription.cancel";
    public static final String QUEUE_PAYMENT_TRANSACTION_CANCEL = "payment.transaction.cancel";
    public static final String QUEUE_PAYMENT_GATEWAY_RESPONSE = "payment.gateway.response";

    @Bean
    public CustomExchange subscriptionExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(EXCHANGE_SUBSCRIPTION, "x-delayed-message", true, false, args);
    }

    @Bean
    public Queue renewalStartQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_SUBSCRIPTION);
        args.put("x-dead-letter-routing-key", QUEUE_PAYMENT_TRANSACTION_CANCEL);
        return new Queue(QUEUE_SUBSCRIPTION_RENEWAL_START, true, false, false, args);
    }

    @Bean
    public Queue cancelQueue() {
        return new Queue(QUEUE_SUBSCRIPTION_CANCEL, true);
    }

    @Bean
    public Queue transactionCancelQueue() {
        return new Queue(QUEUE_PAYMENT_TRANSACTION_CANCEL, true);
    }

    @Bean
    public Queue paymentResponseQueue() {
        return new Queue(QUEUE_PAYMENT_GATEWAY_RESPONSE, true);
    }

    @Bean
    public Binding renewalStartBinding(Queue renewalStartQueue, CustomExchange subscriptionExchange) {
        return BindingBuilder.bind(renewalStartQueue).to(subscriptionExchange).with(QUEUE_SUBSCRIPTION_RENEWAL_START)
                .noargs();
    }

    @Bean
    public Binding cancelBinding(Queue cancelQueue, CustomExchange subscriptionExchange) {
        return BindingBuilder.bind(cancelQueue).to(subscriptionExchange).with(QUEUE_SUBSCRIPTION_CANCEL).noargs();
    }

    @Bean
    public Binding transactionCancelBinding(Queue transactionCancelQueue, CustomExchange subscriptionExchange) {
        return BindingBuilder.bind(transactionCancelQueue).to(subscriptionExchange)
                .with(QUEUE_PAYMENT_TRANSACTION_CANCEL)
                .noargs();
    }

    @Bean
    public Binding paymentResponseBinding(Queue paymentResponseQueue, CustomExchange subscriptionExchange) {
        return BindingBuilder.bind(paymentResponseQueue).to(subscriptionExchange).with(QUEUE_PAYMENT_GATEWAY_RESPONSE)
                .noargs();
    }

}
