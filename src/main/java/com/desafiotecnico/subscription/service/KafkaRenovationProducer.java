package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.config.KafkaConfig;
import com.desafiotecnico.subscription.dto.event.RenovationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("kafka")
public class KafkaRenovationProducer implements RenovationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void sendRenovationStart(RenovationEvent event) {
        log.info("Sending renovation start message for subscription {} via Kafka", event.getSubscriptionId());

        kafkaTemplate.send(KafkaConfig.TOPIC_RENOVATION, event);
    }
}
