package com.desafiotecnico.subscription.error;

public class RenewalTransactionNowFound extends RuntimeException {
    public RenewalTransactionNowFound(String message) {
        super(message);
    }
}
