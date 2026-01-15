package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.TransactionStatus;
import com.desafiotecnico.subscription.domain.RenewalTransaction;
import com.desafiotecnico.subscription.repository.RenewalTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RenewalTransactionService {

    private final RenewalTransactionRepository renewalTransactionRepository;

    @Transactional
    public void cancelTransaction(UUID transactionId, String reason) {
        log.info("Cancelando transação {} com motivo: {}", transactionId, reason);

        RenewalTransaction transaction = renewalTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada com ID: " + transactionId));

        transaction.setStatus(TransactionStatus.CANCELLED.name());
        transaction.setCancellationReason(reason);
        transaction.setDataFinalizacao(LocalDateTime.now());

        renewalTransactionRepository.save(transaction);
        log.info("Transação {} cancelada com sucesso", transactionId);
    }
}
