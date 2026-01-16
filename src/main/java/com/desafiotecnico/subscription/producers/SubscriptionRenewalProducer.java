package com.desafiotecnico.subscription.producers;

import com.desafiotecnico.subscription.dto.event.SubscriptionCancelEvent;
import com.desafiotecnico.subscription.dto.event.PaymentTransactionEvent;

public interface SubscriptionRenewalProducer {
    void sendRenewalStart(PaymentTransactionEvent event);

    void sendRenewalStart(PaymentTransactionEvent event, long delayMs);

    void sendCancelSubscription(SubscriptionCancelEvent event);

    void sendPaymentResponse(com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse event);

    void sendCancelTransaction(com.desafiotecnico.subscription.dto.event.TransactionCancelEvent event);
}
