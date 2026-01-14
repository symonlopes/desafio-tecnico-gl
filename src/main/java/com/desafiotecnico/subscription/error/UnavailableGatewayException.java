package com.desafiotecnico.subscription.error;

public class UnavailableGatewayException extends RuntimeException {
    public UnavailableGatewayException(String message) {
        super(message);
    }
}
