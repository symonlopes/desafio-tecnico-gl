package com.desafiotecnico.subscription.error;

import org.springframework.web.client.RestClientException;

public class UnavailableGatewayException extends RuntimeException {
    public UnavailableGatewayException(String message) {
        super(message);
    }

    public UnavailableGatewayException(String string, RestClientException e) {
        super(string, e);
    }
}
