package com.desafiotecnico.subscription.domain;

public enum PaymentTransactionStatus {
    CREATED,
    PROCESSING,
    WAITING_PAYMENT_PROCESS_RESPONSE,
    APPROVED,
    DECLINED,
    GATEWAY_ERROR,
    PENDING_RETRY,
    ABORTED,
    VOIDED;
}
