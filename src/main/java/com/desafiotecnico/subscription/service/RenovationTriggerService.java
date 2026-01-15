package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.PaymentTransactionStatus;
import com.desafiotecnico.subscription.domain.RenewalTransaction;
import com.desafiotecnico.subscription.dto.event.SubscriptionRenewalStartEvent;
import com.desafiotecnico.subscription.producers.SubscriptionRenewalProducer;
import com.desafiotecnico.subscription.repository.PaymentTransactionRepository;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RenovationTriggerService {

        private final SubscriptionRepository subscriptionRepository;
        private final SubscriptionRenewalProducer subscriptionRenewalProducer;
        private final PaymentTransactionRepository renewalTransactionRepository;

        @Transactional
        public void triggerRenovation(int batchSize, LocalDate dateToProccess) {
                log.info("Disparando processo de renovação para no máximo {} registros, considerando a data {}.",
                                batchSize, dateToProccess);

                var subscriptions = subscriptionRepository.findSubscriptionToProccessPayment(
                                dateToProccess,
                                PageRequest.of(0, batchSize));

                log.info("Foram encontrados {} registros de incrições que serão renovadas.", subscriptions.size());

                subscriptions.forEach(sub -> {

                        var transaction = RenewalTransaction.builder()
                                        .subscription(sub)
                                        .status(PaymentTransactionStatus.CREATED.name())
                                        .dataInicio(java.time.LocalDateTime.now())
                                        .paymentErrorCount(0)
                                        .priceInCents(sub.getPriceInCents())
                                        .build();

                        renewalTransactionRepository.save(transaction);

                        subscriptionRenewalProducer.sendRenewalStart(SubscriptionRenewalStartEvent.builder()
                                        .subscriptionId(sub.getId())
                                        .transactionId(transaction.getId())
                                        .priceInCents(sub.getPriceInCents())
                                        .build());
                });

        }
}
