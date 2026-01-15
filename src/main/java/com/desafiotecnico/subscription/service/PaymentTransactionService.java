package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.PaymentTransactionStatus;
import com.desafiotecnico.subscription.domain.RenewalTransaction;
import com.desafiotecnico.subscription.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Transactional
    public void cancelTransaction(UUID transactionId, String reason, PaymentTransactionStatus status) {
        log.info("Cancelando transação {} com motivo: {}", transactionId, reason);

        RenewalTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada com ID: " + transactionId));

        transaction.setStatus(status.name());
        transaction.setCancellationReason(reason);
        transaction.setDataFinalizacao(LocalDateTime.now());

        paymentTransactionRepository.save(transaction);
        log.info("Transação {} cancelada com sucesso", transactionId);
    }
}
