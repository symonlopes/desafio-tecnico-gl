package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.controller.dto.ApiError;
import com.desafiotecnico.subscription.controller.dto.UserRequest;
import com.desafiotecnico.subscription.domain.User;
import com.desafiotecnico.subscription.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_WithValidData_ReturnsCreated() throws Exception {
        var request = UserRequest.builder()
                .name("John Doe")
                .email("john@example.com")
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

        assertEquals(1, userRepository.count());
    }

    @Test
    void createUser_WithInvalidEmail_ReturnsBadRequest() throws Exception {
        var request = UserRequest.builder()
                .name("John Doe")
                .email("invalid-email")
                .build();

        MvcResult result = mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ApiError apiError = objectMapper.readValue(content, ApiError.class);

        assertEquals("VALIDATION_ERROR", apiError.getCode());
        assertNotNull(apiError.getDetails());

        assertEquals(0, userRepository.count());
    }

    @Test
    void createUser_WithEmptyName_ReturnsBadRequest() throws Exception {
        var request = UserRequest.builder()
                .name("")
                .email("john@example.com")
                .build();

        MvcResult result = mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ApiError apiError = objectMapper.readValue(content, ApiError.class);

        assertEquals("VALIDATION_ERROR", apiError.getCode());
        assertNotNull(apiError.getDetails());

        assertEquals(0, userRepository.count());
    }

    @Test
    void createUser_WithDuplicateEmail_ReturnsBadRequest() throws Exception {
        // Create first user
        var request1 = UserRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .build();

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Try to create second user with same email
        var request2 = UserRequest.builder()
                .name("Jane Doe")
                .email("john@example.com")
                .build();

        MvcResult result = mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andReturn();

        var content = result.getResponse().getContentAsString();
        var apiError = objectMapper.readValue(content, ApiError.class);

        assertEquals("INVALID_ARGUMENT", apiError.getCode());

        assertEquals(1, userRepository.count());
    }
}
