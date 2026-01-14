package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.dto.request.PaymentCallbackRequest;
import com.desafiotecnico.subscription.service.SubscriptionRenewalProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("kafka")
public class CallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionRenewalProducer renovationProducer;

    @Test
    void processCallback_ShouldSendPaymentResponse() throws Exception {
        // Given
        UUID transactionId = UUID.randomUUID();
        PaymentCallbackRequest request = PaymentCallbackRequest.builder()
                .transactionId(transactionId)
                .success(true)
                .message("Success")
                .build();

        // When
        mockMvc.perform(post("/payments/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then
        verify(renovationProducer)
                .sendPaymentResponse(any(com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse.class));
    }
}
