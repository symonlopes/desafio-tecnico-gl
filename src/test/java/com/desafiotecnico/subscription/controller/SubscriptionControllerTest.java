package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.domain.Plan;
import com.desafiotecnico.subscription.domain.User;
import com.desafiotecnico.subscription.dto.request.SubscriptionRequest;
import com.desafiotecnico.subscription.dto.response.SubscriptionResponse;
import com.desafiotecnico.subscription.error.ApiError;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import com.desafiotecnico.subscription.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createSubscription_WithValidUser_ReturnsCreated() throws Exception {
        // Given a user
        User user = userRepository.save(User.builder()
                .name("John Subscription")
                .email("sub@example.com")
                .build());

        var request = SubscriptionRequest.builder()
                .userId(user.getId())
                .plan(Plan.PREMIUM.getName())
                .build();

        // When/Then
        MvcResult result = mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        SubscriptionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                SubscriptionResponse.class);

        log.info("Subscription created: {}", response);

        assertNotNull(response.getId());
        assertEquals(user.getId(), response.getUserId());
        assertEquals(Plan.PREMIUM, response.getPlan());
        assertEquals(com.desafiotecnico.subscription.domain.SubscriptionStatus.ATIVA, response.getStatus());

    }

    @Test
    void createSubscription_UserNotFound_ReturnsError() throws Exception {
        var request = SubscriptionRequest.builder()
                .userId(UUID.randomUUID())
                .plan(Plan.BASICO.getName())
                .build();

        MvcResult result = mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        var apiError = objectMapper.readValue(result.getResponse().getContentAsString(), ApiError.class);
        assertEquals("USER_NOT_FOUND", apiError.getCode());
    }

    @Test
    void createSubscription_ActiveSubscriptionExists_ReturnsError() throws Exception {
        // Given a user with active subscription
        User user = userRepository.save(User.builder()
                .name("Jane Active")
                .email("active@example.com")
                .build());

        // Create first subscription
        var request1 = SubscriptionRequest.builder()
                .userId(user.getId())
                .plan(Plan.BASICO.getName())
                .build();

        mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Try to create second subscription
        var request2 = SubscriptionRequest.builder()
                .userId(user.getId())
                .plan(Plan.PREMIUM.getName())
                .build();

        MvcResult result = mockMvc.perform(post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andReturn();

        var apiError = objectMapper.readValue(result.getResponse().getContentAsString(), ApiError.class);
        assertEquals("ACTIVE_SUBSCRIPTION_EXISTS", apiError.getCode());
    }
}
