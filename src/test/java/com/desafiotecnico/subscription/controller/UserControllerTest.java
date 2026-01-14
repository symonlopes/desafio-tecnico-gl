package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.error.ApiError;
import com.desafiotecnico.subscription.dto.request.UserCreationRequest;
import com.desafiotecnico.subscription.domain.User;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
public class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        private final Faker faker = new Faker();

        @Test
        void createUser_WithValidData_ReturnsCreated() throws Exception {
                var request = UserCreationRequest.builder()
                                .name(faker.name().fullName())
                                .email(faker.internet().emailAddress())
                                .build();

                var result = mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                User createdUser = objectMapper.readValue(result.getResponse().getContentAsString(), User.class);

                assertNotNull(createdUser.getId());
                assertEquals(request.getName(), createdUser.getName());
                assertEquals(request.getEmail(), createdUser.getEmail());

        }

        @Test
        void createUser_WithInvalidEmail_ReturnsBadRequest() throws Exception {
                var request = UserCreationRequest.builder()
                                .name(faker.name().fullName())
                                .email("invalid-email")
                                .build();

                MvcResult result = mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                var content = result.getResponse().getContentAsString();
                var apiError = objectMapper.readValue(content, ApiError.class);

                log.info("ApiError: {}", apiError);

                assertEquals("VALIDATION_ERROR", apiError.getCode());
                assertNotNull(apiError.getDetails());

        }

        @Test
        void createUser_WithEmptyName_ReturnsBadRequest() throws Exception {
                var request = UserCreationRequest.builder()
                                .name("")
                                .email(faker.internet().emailAddress())
                                .build();

                MvcResult result = mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                var content = result.getResponse().getContentAsString();
                var apiError = objectMapper.readValue(content, ApiError.class);

                assertEquals("VALIDATION_ERROR", apiError.getCode());
                assertNotNull(apiError.getDetails());
        }

        @Test
        void createUser_WithDuplicateEmail_ReturnsBadRequest() throws Exception {
                String existingEmail = faker.internet().emailAddress();
                // Create first user
                var request1 = UserCreationRequest.builder()
                                .name(faker.name().fullName())
                                .email(existingEmail)
                                .build();

                mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request1)))
                                .andExpect(status().isCreated());

                // Try to create second user with same email
                var request2 = UserCreationRequest.builder()
                                .name(faker.name().fullName())
                                .email(existingEmail)
                                .build();

                MvcResult result = mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request2)))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                var content = result.getResponse().getContentAsString();
                var apiError = objectMapper.readValue(content, ApiError.class);

                assertEquals("EMAIL_ALREADY_EXIST", apiError.getCode());

        }
}
