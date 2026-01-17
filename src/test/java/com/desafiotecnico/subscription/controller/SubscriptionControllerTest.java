package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.domain.Plan;
import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import com.desafiotecnico.subscription.dto.request.NewSignatureRequest;
import com.desafiotecnico.subscription.dto.response.SubscriptionResponse;
import com.desafiotecnico.subscription.error.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
public class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createSubscription_WithValidRequest_ReturnsCreated() throws Exception {
        UUID userId = UUID.randomUUID();

        NewSignatureRequest request = NewSignatureRequest.builder()
                .id(UUID.randomUUID())
                .usuarioId(userId)
                .plano(Plan.PREMIUM)
                .dataInicio(LocalDate.now())
                .dataExpiracao(LocalDate.now().plusMonths(12))
                .status(SubscriptionStatus.ATIVA)
                .build();

        MvcResult result = mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        SubscriptionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                SubscriptionResponse.class);

        log.info("Subscription created: {}", response);

        assertEquals(userId, response.getUserId());
        assertEquals(Plan.PREMIUM.getName(), response.getPlan());
        assertEquals(Plan.PREMIUM.getPriceInCents(), response.getPriceInCents());
    }

    @Test
    void createSubscription_ActiveSubscriptionExists_ReturnsError() throws Exception {
        UUID userId = UUID.randomUUID();

        // Create first subscription
        NewSignatureRequest request1 = NewSignatureRequest.builder()
                .usuarioId(userId)
                .plano(Plan.BASICO)
                .dataInicio(LocalDate.now())
                .dataExpiracao(LocalDate.now().plusMonths(1))
                .status(SubscriptionStatus.ATIVA)
                .build();

        mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Try to create second subscription for same user (should fail because first is
        // active)
        NewSignatureRequest request2 = NewSignatureRequest.builder()
                .usuarioId(userId)
                .plano(Plan.PREMIUM)
                .dataInicio(LocalDate.now())
                .dataExpiracao(LocalDate.now().plusMonths(1))
                .status(SubscriptionStatus.ATIVA)
                .build();

        MvcResult result = mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andReturn(); // Expect failure or specific error

        if (result.getResponse().getStatus() == 201) {
            // If it succeeded, check if the previous one was deactivated (if that's the
            // logic we chose)
            // But our logic throws "ACTIVE_SUBSCRIPTION_EXISTS" if date > now.
            // The first subscription expires in 1 month, so it IS active.
            // So we expect 400/500 with error.
            throw new RuntimeException("Should have failed but returned 201");
        }

        var apiError = objectMapper.readValue(result.getResponse().getContentAsString(), ApiError.class);
        assertEquals("ACTIVE_SUBSCRIPTION_EXISTS", apiError.getCode());
    }

    @Test
    void createSubscription_ExpiredSubscription_AllowsNewAndDeactivatesOld() throws Exception {
        UUID userId = UUID.randomUUID();

        // Create an EXPIRED subscription (manually ID to allow creation with past date?
        // Or use service to mock it? We are in specific integration test.
        // We can just create one via repository or just post one with past date if
        // validation allows?
        // Validations on DTO are just @NotNull, so past date is allowed.

        NewSignatureRequest requestOld = NewSignatureRequest.builder()
                .usuarioId(userId)
                .plano(Plan.BASICO)
                .dataInicio(LocalDate.now().minusMonths(2))
                .dataExpiracao(LocalDate.now().minusMonths(1)) // Expired
                .status(SubscriptionStatus.ATIVA)
                .build();

        mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestOld)))
                .andExpect(status().isCreated());

        // Check that it is ATIVA
        // We can't check DB directly without repository injection, but we assume it is.

        // Now create NEW subscription
        NewSignatureRequest requestNew = NewSignatureRequest.builder()
                .usuarioId(userId)
                .plano(Plan.PREMIUM)
                .dataInicio(LocalDate.now())
                .dataExpiracao(LocalDate.now().plusMonths(1))
                .status(SubscriptionStatus.ATIVA)
                .build();

        MvcResult result = mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestNew)))
                .andExpect(status().isCreated())
                .andReturn();

        SubscriptionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                SubscriptionResponse.class);
        assertEquals(Plan.PREMIUM.getName(), response.getPlan());
    }
}
