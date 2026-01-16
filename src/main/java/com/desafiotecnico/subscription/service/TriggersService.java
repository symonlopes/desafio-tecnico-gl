package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.dto.event.PaymentTransactionEvent;
import com.desafiotecnico.subscription.producers.SubscriptionRenewalProducer;
import com.desafiotecnico.subscription.repository.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriggersService {

        private final SubscriptionRenewalProducer subscriptionRenewalProducer;
        private final PaymentTransactionRepository paymentTransactionRepositiry;

        @Transactional
        public void generatePaymentTransactions() {
                log.info("Gerando transações de pagamento para hoje.");
                int subscriptionsCount = paymentTransactionRepositiry.generatePaymentTransactions();
                log.info("Transações de pagamento geradas pelo banco de dados: {}", subscriptionsCount);
        }

        @Transactional
        public void enqueuePaymentTransactions(int limit) {
                log.info("Disparando enfileiramento de transações de pagamento abertas, limite: {}", limit);

                var openPaymentTransactions = paymentTransactionRepositiry.findOpenPaymentTransactions(
                                PageRequest.of(0, limit));

                log.info("{} transações de pagamento abertas para enfileiramento.", openPaymentTransactions.size());

                openPaymentTransactions.forEach(pt -> {
                        subscriptionRenewalProducer.sendRenewalStart(PaymentTransactionEvent.builder()
                                        .subscriptionId(pt.getSubscription().getId())
                                        .transactionId(pt.getId())
                                        .priceInCents(pt.getPriceInCents())
                                        .build());
                });
        }
}