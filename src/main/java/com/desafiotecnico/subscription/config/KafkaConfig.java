package com.desafiotecnico.subscription.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("kafka")
public class KafkaConfig {

    public static final String TOPIC_RENOVATION = "subscription.renovation.start";

    @Bean
    public NewTopic renovationTopic() {
        return TopicBuilder.name(TOPIC_RENOVATION)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
