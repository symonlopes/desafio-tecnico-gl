package com.desafiotecnico.subscription.consumers;

import com.desafiotecnico.subscription.config.RabbitMQConfig;
import com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse;
import com.desafiotecnico.subscription.error.UnavailableGatewayException;
import com.desafiotecnico.subscription.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackConsumer {

    private final SubscriptionService subscriptionService;

    /**
     * Consumer responsável por receber as respostas do gateway de pagamentos.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_GATEWAY_RESPONSE)
    public void receivePaymentResponse(PaymentGatewayResponse event) {
        log.info("Resposta do gateway de pagamentos recebida via RabbitMQ para a transação: {}",
                event.getTransactionId());
        try {
            subscriptionService.processPaymentCallback(event);
        } catch (UnavailableGatewayException e) {
            log.warn(
                    "Gateway indisponível durante o processamento de callback de pagamento para a transação {}. A mensagem será reenviada.",
                    event.getTransactionId());
            throw e; // Spring AMQP retry mechanism will handle this
        }
    }
}
