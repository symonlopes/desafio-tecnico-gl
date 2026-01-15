package com.desafiotecnico.subscription.consumers;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.dto.event.TransactionCancelEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalTransactionCancelConsumer {

    private final com.desafiotecnico.subscription.service.RenewalTransactionService renewalTransactionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TRANSACTION_CANCEL)
    public void consumeTransactionCancel(TransactionCancelEvent event) {
        log.info("Mensagem de cancelamento de transação recebida: TransactionId={}, SubscriptionId={}, Reason={}",
                event.getTransactionId(),
                event.getSubscriptionId(),
                event.getReason());

        renewalTransactionService.cancelTransaction(event.getTransactionId(), event.getReason());
    }
}
