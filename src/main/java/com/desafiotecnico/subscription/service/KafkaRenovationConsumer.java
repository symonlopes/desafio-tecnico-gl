package com.desafiotecnico.subscription.service;

import com.desafiotecnico.subscription.config.KafkaConfig;
import com.desafiotecnico.subscription.dto.event.RenovationEvent;
import com.desafiotecnico.subscription.error.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import com.desafiotecnico.subscription.domain.RenewalStatus;
import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import com.desafiotecnico.subscription.repository.RenewalTransactionRepository;
import com.desafiotecnico.subscription.repository.SubscriptionRepository;
import com.desafiotecnico.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile("kafka")
public class KafkaRenovationConsumer {

    private final SubscriptionService subscriptionService;
    private final RenewalTransactionRepository renewalTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 60_000, multiplier = 2.0, maxDelay = 600_000), include = {
            PaymentException.class })
    @KafkaListener(topics = KafkaConfig.TOPIC_RENOVATION, groupId = "subscription-group")
    public void consumeRenovationStart(RenovationEvent event) {
        log.info("Mensagem de evento para iniciar renovação de assinatura recebida: {}", event);

        // É preciso atualizar a quantidade de tentativas de pagamento e, quando atingir
        // o limite, cancelar a assinatura.
        throw new PaymentException("Transaction not found");

        // Update transaction
        // transaction.setStatus(RenewalStatus.RENEWED.getName());
        // transaction.setDataFinalizacao(LocalDateTime.now());
        // renewalTransactionRepository.save(transaction);

        // // Update Subscription
        // var subscription = subscriptionRepository.findById(event.getSubscriptionId())
        // .orElseThrow(() -> new PaymentException("Subscription not found"));

        // subscription.setExpirationDate(subscription.getExpirationDate().plusMonths(1));
        // subscriptionRepository.save(subscription);

        // log.info("Processamento de pagamento de para inscrição {} concluído com
        // sucesso.", event.getSubscriptionId());
    }

    @DltHandler
    public void dltHandler(RenovationEvent event) {
        log.error("Message moved to DLT (Max retries reached): {}", event);

        // Cancel Subscription
        try {
            subscriptionService.cancelSubscription(event.getSubscriptionId());
        } catch (Exception e) {
            log.error("Error canceling subscription {}", event.getSubscriptionId(), e);
        }

        // Update Transaction to FAILED
        try {
            var transaction = renewalTransactionRepository.findById(event.getTransactionId()).orElse(null);
            if (transaction != null) {
                transaction.setStatus(RenewalStatus.FAILED.getName());
                transaction.setDataFinalizacao(LocalDateTime.now());
                renewalTransactionRepository.save(transaction);
            }
        } catch (Exception e) {
            log.error("Error updating transaction {}", event.getTransactionId(), e);
        }
    }
}
