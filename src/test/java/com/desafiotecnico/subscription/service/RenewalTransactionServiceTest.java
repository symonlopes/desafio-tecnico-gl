package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.TransactionStatus;
import com.desafiotecnico.subscription.domain.RenewalTransaction;
import com.desafiotecnico.subscription.repository.RenewalTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class RenewalTransactionServiceTest {

    @Mock
    private RenewalTransactionRepository renewalTransactionRepository;

    @InjectMocks
    private RenewalTransactionService renewalTransactionService;

    @Test
    void cancelTransaction_ShouldUpdateStatusAndReason() {
        // Given
        UUID transactionId = UUID.randomUUID();
        String reason = "Payment failed too many times";
        RenewalTransaction transaction = new RenewalTransaction();
        transaction.setId(transactionId);
        transaction.setStatus(TransactionStatus.PROCESSING.name());

        when(renewalTransactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        // When
        renewalTransactionService.cancelTransaction(transactionId, reason);

        // Then
        verify(renewalTransactionRepository).save(any(RenewalTransaction.class));
        assertEquals(TransactionStatus.CANCELLED.name(), transaction.getStatus());
        assertEquals(reason, transaction.getCancellationReason());
        assertNotNull(transaction.getDataFinalizacao());
    }
}
