package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.dto.event.SubscriptionRenewalStartEvent;
import com.desafiotecnico.subscription.repository.RenewalTransactionRepository;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import com.desafiotecnico.subscription.repository.UserRepository;
import com.desafiotecnico.subscription.producers.SubscriptionRenewalProducer;
import com.desafiotecnico.subscription.dto.gateway.PaymentGatewayRequest;
import com.desafiotecnico.subscription.dto.gateway.PaymentGatewayInitialResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpEntity;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRenewalProducer subscriptionRenewalProducer;

    @Mock
    private RenewalTransactionRepository renewalTransactionRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "paymentUrl", "http://test-url.com");
    }

    @Test
    void processSubscriptionStartRenewal_ShouldPostToExternalUrl() {
        // Given
        SubscriptionRenewalStartEvent event = new SubscriptionRenewalStartEvent();
        event.setSubscriptionId(UUID.randomUUID());
        event.setTransactionId(UUID.randomUUID());
        event.setPriceInCents(1000);

        // When
        subscriptionService.processSubscriptionStartRenewal(event);

        // Then
        ArgumentCaptor<HttpEntity<PaymentGatewayRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq("http://test-url.com"), captor.capture(),
                eq(PaymentGatewayInitialResponse.class));

        PaymentGatewayRequest capturedRequest = captor.getValue().getBody();
        assert capturedRequest != null;
        assert capturedRequest.getAmount().equals(1000);
        assert capturedRequest.getCustomId().equals(event.getTransactionId());
    }

    @Test
    void processPaymentCallback_ShouldSendCancelTransaction_WhenMaxRetriesReached() {
        // Given
        UUID transactionId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        com.desafiotecnico.subscription.domain.Subscription subscription = com.desafiotecnico.subscription.domain.Subscription
                .builder()
                .id(subscriptionId)
                .priceInCents(1000)
                .build();

        com.desafiotecnico.subscription.domain.RenewalTransaction transaction = com.desafiotecnico.subscription.domain.RenewalTransaction
                .builder()
                .id(transactionId)
                .paymentAttempts(2) // Will increment to 3
                .subscription(subscription)
                .build();

        com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse event = com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse
                .builder()
                .transactionId(transactionId)
                .success(false)
                .build();

        org.mockito.Mockito.when(renewalTransactionRepository.findById(transactionId))
                .thenReturn(java.util.Optional.of(transaction));

        // When
        subscriptionService.processPaymentCallback(event);

        // Then
        verify(subscriptionRenewalProducer).sendCancelSubscription(org.mockito.ArgumentMatchers
                .any(com.desafiotecnico.subscription.dto.event.SubscriptionCancelEvent.class));
        verify(subscriptionRenewalProducer).sendCancelTransaction(org.mockito.ArgumentMatchers
                .any(com.desafiotecnico.subscription.dto.event.TransactionCancelEvent.class));
    }
}
