package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.domain.Plan;
import com.desafiotecnico.subscription.domain.RenewalStatus;
import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.dto.request.TriggerRenovationRequest;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.desafiotecnico.subscription.service.RenovationProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("kafka")
public class RenovationTriggerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private SubscriptionRepository subscriptionRepository;

        @Autowired
        private com.desafiotecnico.subscription.repository.RenewalTransactionRepository renewalTransactionRepository;

        @MockitoBean
        private RenovationProducer renovationProducer;

        private final Faker faker = new Faker();

        @Test
        void triggerRenovation_UpdatesStatusAndSendsMessage() throws Exception {
                // Given a subscription needing renovation
                var sub = Subscription.builder()
                                .userId(UUID.randomUUID())
                                .plan(Plan.BASICO.getName())
                                .status(com.desafiotecnico.subscription.domain.SubscriptionStatus.ATIVA)
                                .startDate(LocalDate.now().minusMonths(1))
                                .expirationDate(LocalDate.now().plusDays(2)) // Expires in 2 days from now
                                .build();

                subscriptionRepository.save(sub);

                var request = com.desafiotecnico.subscription.dto.request.TriggerRenovationRequest.builder()
                                .amount(10)
                                .dateToProccess(LocalDate.now().plusDays(2))
                                .build();

                // When
                mockMvc.perform(post("/subscriptions/renovation/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // Then
                var transaction = renewalTransactionRepository
                                .findAll().stream()
                                .filter(t -> t.getSubscription().getId().equals(sub.getId()))
                                .findFirst()
                                .orElseThrow();

                assertEquals(RenewalStatus.NEW.getName(), transaction.getStatus());

                verify(renovationProducer, timeout(1000).atLeastOnce())
                                .sendRenovationStart(Mockito
                                                .any(com.desafiotecnico.subscription.dto.event.RenovationEvent.class));
        }
}
