package com.desafiotecnico.subscription.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Profile;

@Configuration
@Profile("rabbit")
public class RabbitMQConfig {

    public static final String QUEUE_RENOVATION = "subscription.renovation.start";

    @Bean
    public Queue renovationQueue() {
        return new Queue(QUEUE_RENOVATION, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
