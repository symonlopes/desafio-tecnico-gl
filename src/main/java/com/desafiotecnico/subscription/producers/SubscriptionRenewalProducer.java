package com.desafiotecnico.subscription.producers;

import com.desafiotecnico.subscription.dto.event.SubscriptionCancelEvent;
import com.desafiotecnico.subscription.dto.event.SubscriptionRenewalStartEvent;

public interface SubscriptionRenewalProducer {
    void sendRenewalStart(SubscriptionRenewalStartEvent event);

    void sendRenewalStart(SubscriptionRenewalStartEvent event, long delayMs);

    void sendCancelSubscription(SubscriptionCancelEvent event);

    void sendPaymentResponse(com.desafiotecnico.subscription.dto.event.PaymentGatewayResponse event);
}
