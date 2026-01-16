package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.domain.PaymentTransactionStatus;
import com.desafiotecnico.subscription.domain.PaymentTransaction;
import com.desafiotecnico.subscription.dto.event.PaymentTransactionEvent;
import com.desafiotecnico.subscription.dto.event.SubscriptionCancelEvent;
import com.desafiotecnico.subscription.dto.gateway.PaymentGatewayResponse;
import com.desafiotecnico.subscription.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

        @Mock
        private PaymentTransactionRepository paymentTransactionRepository;

        @Mock
        private RestTemplate restTemplate;

        @Mock
        private RabbitTemplate rabbitTemplate;

        @InjectMocks
        private PaymentTransactionService paymentTransactionService;

        @Mock
        private SubscriptionService subscriptionService;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(paymentTransactionService, "paymentUrl", "http://localhost:8080/payment");
                ReflectionTestUtils.setField(paymentTransactionService, "declinedPaymentRetryIntervalInSeconds", 10L);
        }

        @Test
        void shouldRetryPaymentWhenGatewayReturns4xxAndRetryCountIsEffective() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID subscriptionId = UUID.randomUUID();
                PaymentTransactionEvent event = PaymentTransactionEvent.builder()
                                .transactionId(transactionId)
                                .subscriptionId(subscriptionId)
                                .priceInCents(1000)
                                .rejectedPaymentCount(0)
                                .build();

                PaymentTransaction transaction = new PaymentTransaction();
                transaction.setId(transactionId);
                transaction.setStatus(PaymentTransactionStatus.CREATED.name());

                when(paymentTransactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
                when(restTemplate.postForObject(anyString(), any(), any()))
                                .thenThrow(new HttpClientErrorException(
                                                org.springframework.http.HttpStatus.BAD_REQUEST));

                // Act
                paymentTransactionService.startPaymentTransaction(event);

                // Assert
                verify(paymentTransactionRepository, times(2)).save(transaction);
                assertEquals(PaymentTransactionStatus.PENDING_RETRY.name(), transaction.getStatus());

                ArgumentCaptor<PaymentTransactionEvent> eventCaptor = ArgumentCaptor
                                .forClass(PaymentTransactionEvent.class);
                verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_SUBSCRIPTION),
                                eq(RabbitMQConfig.QUEUE_SUBSCRIPTION_RENEWAL_START), eventCaptor.capture(),
                                any(MessagePostProcessor.class));

                assertEquals(1, eventCaptor.getValue().getRejectedPaymentCount());
        }

        @Test
        void shouldNotRetryWhenMaxRetriesExceeded() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID subscriptionId = UUID.randomUUID();
                PaymentTransactionEvent event = PaymentTransactionEvent.builder()
                                .transactionId(transactionId)
                                .subscriptionId(subscriptionId)
                                .priceInCents(1000)
                                .rejectedPaymentCount(3)
                                .build();

                PaymentTransaction transaction = new PaymentTransaction();
                transaction.setId(transactionId);
                transaction.setStatus(PaymentTransactionStatus.CREATED.name());

                when(paymentTransactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
                when(restTemplate.postForObject(anyString(), any(), any()))
                                .thenThrow(new HttpClientErrorException(
                                                org.springframework.http.HttpStatus.BAD_REQUEST));

                // Act
                paymentTransactionService.startPaymentTransaction(event);

                // Assert
                verify(paymentTransactionRepository, times(2)).save(transaction);
                assertEquals(PaymentTransactionStatus.DECLINED.name(), transaction.getStatus());
                verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(),
                                any(PaymentTransactionEvent.class), any(MessagePostProcessor.class));

                verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_SUBSCRIPTION),
                                eq(RabbitMQConfig.QUEUE_SUBSCRIPTION_CANCEL), any(SubscriptionCancelEvent.class));
        }

        @Test
        void shouldRenewSubscriptionOnApprovedPayment() {
                // Arrange
                UUID transactionId = UUID.randomUUID();
                UUID subscriptionId = UUID.randomUUID();
                PaymentTransactionEvent event = PaymentTransactionEvent.builder()
                                .transactionId(transactionId)
                                .subscriptionId(subscriptionId)
                                .priceInCents(1000)
                                .build();

                PaymentTransaction transaction = new PaymentTransaction();
                transaction.setId(transactionId);
                transaction.setStatus(PaymentTransactionStatus.CREATED.name());

                PaymentGatewayResponse response = new PaymentGatewayResponse();
                response.setCustomId(transactionId.toString());

                when(paymentTransactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
                when(restTemplate.postForObject(anyString(), any(), any())).thenReturn(response);

                // Act
                paymentTransactionService.startPaymentTransaction(event);

                // Assert
                verify(paymentTransactionRepository, times(2)).save(transaction);
                assertEquals(PaymentTransactionStatus.APPROVED.name(), transaction.getStatus());
                verify(subscriptionService).renewSubscription(subscriptionId);
        }
}
