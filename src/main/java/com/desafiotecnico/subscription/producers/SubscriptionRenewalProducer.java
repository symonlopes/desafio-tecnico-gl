package com.desafiotecnico.subscription.producers;

import com.desafiotecnico.subscription.dto.event.SubscriptionCancelEvent;
import com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse;
import com.desafiotecnico.subscription.dto.event.TransactionCancelEvent;

import java.util.List;

import com.desafiotecnico.subscription.dto.event.PaymentTransactionEvent;

public interface SubscriptionRenewalProducer {
    void sendRenewalStart(PaymentTransactionEvent event);

    void sendRenewalStart(PaymentTransactionEvent event, long delayMs);

    void sendCancelSubscription(SubscriptionCancelEvent event);

    void sendPaymentResponse(PaymentGatewayResponse event);

    void sendCancelTransaction(TransactionCancelEvent event);

}
