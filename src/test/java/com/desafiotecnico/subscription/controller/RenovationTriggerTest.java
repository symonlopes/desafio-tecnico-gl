package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.domain.Plan;
import com.desafiotecnico.subscription.domain.RenewalStatus;
import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.dto.request.SubscriptionRenewalTrigger;
import com.desafiotecnico.subscription.producers.SubscriptionRenewalProducer;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
        private SubscriptionRenewalProducer renovationProducer;

        private final Faker faker = new Faker();

        @Test
        void triggerRenovation_UpdatesStatusAndSendsMessage() throws Exception {
                // Given a subscription needing renovation
                var sub = Subscription.builder()
                                .userId(UUID.randomUUID())
                                .plan(Plan.BASICO.getName())
                                .priceInCents(Plan.BASICO.getPriceInCents())
                                .status(com.desafiotecnico.subscription.domain.SubscriptionStatus.ATIVA)
                                .startDate(LocalDate.now().minusMonths(1))
                                .expirationDate(LocalDate.now().plusDays(2)) // Expires in 2 days from now
                                .build();

                subscriptionRepository.save(sub);

                var request = com.desafiotecnico.subscription.dto.request.SubscriptionRenewalTrigger.builder()
                                .maxSubscriptions(10)
                                .dateToProcess(LocalDate.now().plusDays(2))
                                .build();

                // When
                mockMvc.perform(post("/triggers/renewal")
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
                assertEquals(1990, transaction.getPriceInCents());

                verify(renovationProducer, timeout(1000).atLeastOnce())
                                .sendRenewalStart(Mockito
                                                .any(com.desafiotecnico.subscription.dto.event.SubscriptionRenewalStartEvent.class));
        }
}
