package com.desafiotecnico.subscription.error;

public class PaymentTransactionNotFoundException extends RuntimeException {
    public PaymentTransactionNotFoundException(String message) {
        super(message);
    }
}
