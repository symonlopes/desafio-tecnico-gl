package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.dto.event.PaymentTransactionEvent;
import com.desafiotecnico.subscription.producers.SubscriptionRenewalProducer;
import com.desafiotecnico.subscription.repository.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriggersService {

        private final SubscriptionRenewalProducer subscriptionRenewalProducer;
        private final PaymentTransactionRepository paymentTransactionRepository;

        @Transactional
        public void generatePaymentTransactions(LocalDate dateToProcess) {
                log.info("Gerando transações de pagamento para hoje.");
                int subscriptionsCount = paymentTransactionRepository.generatePaymentTransactions();
                log.info("Transações de pagamento geradas pelo banco de dados: {}", subscriptionsCount);
        }

        public void enqueuePaymentTransactions(int limit, LocalDate dateToProcess) {

                log.info("Disparando enfileiramento de transações de pagamento abertas, limite: {}", limit);

                var openPaymentTransactions = paymentTransactionRepository.findOpenPaymentTransactions(
                                PageRequest.of(0, limit));

                log.info("{} transações em aberto serão enviadas para fila.", openPaymentTransactions.size());

                openPaymentTransactions.parallelStream().forEach(pt -> {
                        subscriptionRenewalProducer.sendRenewalStart(
                                        PaymentTransactionEvent.builder()
                                                        .subscriptionId(pt.getSubscription().getId())
                                                        .transactionId(pt.getId())
                                                        .priceInCents(pt.getPriceInCents())
                                                        .build());
                });

        }
}