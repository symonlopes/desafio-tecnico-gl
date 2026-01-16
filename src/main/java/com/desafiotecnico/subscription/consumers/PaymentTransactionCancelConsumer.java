package com.desafiotecnico.subscription.consumers;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.domain.PaymentTransactionStatus;
import com.desafiotecnico.subscription.service.PaymentTransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_TRANSACTION_CANCEL)
public class PaymentTransactionCancelConsumer {

    private final PaymentTransactionService paymentTransactionService;

    @RabbitHandler
    public void consumeRenewalStartDlq(com.desafiotecnico.subscription.dto.event.PaymentTransactionEvent event) {
        log.warn(
                "Mensagem de renovação recebida na DLQ (Esgotamento de tentativas): TransactionId={}, SubscriptionId={}",
                event.getTransactionId(),
                event.getSubscriptionId());

        paymentTransactionService.cancelTransaction(event.getTransactionId(),
                "Renovação falhou após máximo de tentativas.", PaymentTransactionStatus.ABORTED);
    }
}
