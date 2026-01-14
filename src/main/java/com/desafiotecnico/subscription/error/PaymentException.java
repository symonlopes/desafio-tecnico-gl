package com.desafiotecnico.subscription.error;

public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
