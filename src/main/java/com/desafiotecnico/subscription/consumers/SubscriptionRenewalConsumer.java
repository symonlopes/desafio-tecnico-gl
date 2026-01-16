package com.desafiotecnico.subscription.consumers;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.dto.event.PaymentTransactionEvent;
import com.desafiotecnico.subscription.service.PaymentTransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalConsumer {

    private final PaymentTransactionService paymentTransactionService;

    /*
     * O parâmetro concurrency deve ser ajustado de acordo com o rate limit do
     * gateway de pagamento e instâncias desse código em execução.
     * 
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_SUBSCRIPTION_RENEWAL_START, concurrency = "100")
    public void consumePaymentTransactionStartationStart(PaymentTransactionEvent event) {
        log.info("Mensagem de QUEUE_SUBSCRIPTION_RENEWAL_START recebida via RabbitMQ: {}", event);
        paymentTransactionService.startPaymentTransaction(event);
    }
}
