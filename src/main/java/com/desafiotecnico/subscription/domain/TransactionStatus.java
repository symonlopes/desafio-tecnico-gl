package com.desafiotecnico.subscription.domain;

public enum TransactionStatus {

    NEW,
    WAITING_PAYMENT_GATEWAY_RESPONSE,
    PROCESSING,
    RENEWED,
    FAILED,
    WAITING_RETRY,
    FINISHED,
    CANCELLED;
}
