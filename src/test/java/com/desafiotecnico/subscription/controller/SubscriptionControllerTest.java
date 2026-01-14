package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.domain.Plan;
import com.desafiotecnico.subscription.domain.User;
import com.desafiotecnico.subscription.dto.request.SubscriptionRequest;
import com.desafiotecnico.subscription.dto.request.UserCreationRequest;
import com.desafiotecnico.subscription.dto.response.SubscriptionResponse;
import com.desafiotecnico.subscription.error.ApiError;
import com.desafiotecnico.subscription.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
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

        private final Faker faker = new Faker();

        public MvcResult postUser(UserCreationRequest request) throws JsonProcessingException, Exception {

                return mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();
        }

        @Test
        void createSubscription_WithValidUser() throws Exception {

                var request = UserCreationRequest.builder()
                                .name(faker.name().fullName())
                                .email(faker.internet().emailAddress())
                                .build();

                var createdUserResult = postUser(request);

                User createdUser = objectMapper.readValue(createdUserResult.getResponse().getContentAsString(),
                                User.class);

                SubscriptionRequest subscriptionRequest = SubscriptionRequest.builder()
                                .userId(createdUser.getId())
                                .plan(Plan.BASICO.getName())
                                .build();

                MvcResult result = mockMvc.perform(post("/subscriptions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(subscriptionRequest)))
                                .andExpect(status().isCreated())
                                .andReturn();

                SubscriptionResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                                SubscriptionResponse.class);

                log.info("Subscription created: {}", response);

                assertNotNull(response.getId());
                assertEquals(createdUser.getId(), response.getUserId());
                assertEquals(Plan.BASICO.getName(), response.getPlan());

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
                                .name(faker.name().fullName())
                                .email(faker.internet().emailAddress())
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
                                .andReturn();

                var apiError = objectMapper.readValue(result.getResponse().getContentAsString(), ApiError.class);
                assertEquals("ACTIVE_SUBSCRIPTION_EXISTS", apiError.getCode());
        }

        @Test
        void createSubscription_WithInvalidPlan_ReturnsBadRequest() throws Exception {
                // Given a user
                User user = userRepository.save(User.builder()
                                .name(faker.name().fullName())
                                .email(faker.internet().emailAddress())
                                .build());

                // Try to create subscription with invalid plan
                var request = SubscriptionRequest.builder()
                                .userId(user.getId())
                                .plan("INVALID_PLAN_NAME")
                                .build();

                MvcResult result = mockMvc.perform(post("/subscriptions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                var apiError = objectMapper.readValue(result.getResponse().getContentAsString(), ApiError.class);
                assertEquals("INVALID_PLAN", apiError.getCode());
        }
}
