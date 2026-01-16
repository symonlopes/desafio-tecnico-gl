package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.domain.PaymentTransaction;
import com.desafiotecnico.subscription.domain.PaymentTransactionStatus;
import com.desafiotecnico.subscription.dto.event.PaymentTransactionEvent;
import com.desafiotecnico.subscription.producers.SubscriptionRenewalProducer;
import com.desafiotecnico.subscription.repository.PaymentTransactionRepository;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriggersService {

        private final SubscriptionRepository subscriptionRepository;
        private final SubscriptionRenewalProducer subscriptionRenewalProducer;
        private final PaymentTransactionRepository paymentTransactionRepositiry;

        @Transactional
        public void triggerRenovation(int batchSize, LocalDate dateToProccess) {
                log.info("Disparando renovação. Batch: {}, Data: {}", batchSize, dateToProccess);

                var subscriptions = subscriptionRepository.findSubscriptionToProccessPayment(
                                dateToProccess,
                                PageRequest.of(0, batchSize));

                log.info("Registros encontrados: {}", subscriptions.size());

                subscriptions.forEach(sub -> {
                        // 1. Prepara e Salva a Transação (Dentro do contexto transacional)
                        var transaction = PaymentTransaction.builder()
                                        .subscription(sub)
                                        .status(PaymentTransactionStatus.CREATED.name())
                                        .dataInicio(java.time.LocalDateTime.now())
                                        .priceInCents(sub.getPriceInCents())
                                        .build();

                        paymentTransactionRepositiry.save(transaction);

                        // 2. Prepara o Evento
                        var event = PaymentTransactionEvent.builder()
                                        .subscriptionId(sub.getId())
                                        .transactionId(transaction.getId())
                                        .priceInCents(sub.getPriceInCents())
                                        .build();

                        // 3. O PULO DO GATO: Só envia SE e QUANDO o commit ocorrer
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                        log.debug("Transação commitada com sucesso. Enviando evento para ID {}",
                                                        transaction.getId());
                                        subscriptionRenewalProducer.sendRenewalStart(event);
                                }
                        });
                });
        }
}