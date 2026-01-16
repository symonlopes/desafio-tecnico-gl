package com.desafiotecnico.subscription.domain;

public enum PaymentTransactionStatus {
    CREATED,
    PROCESSING,
    APPROVED,
    DECLINED,
    GATEWAY_ERROR,
    PENDING_RETRY,
    ABORTED,
    VOIDED;
}
