package com.desafiotecnico.subscription.error;

public class FatalProcessingException extends RuntimeException {

    public FatalProcessingException(String message) {
        super(message);
    }

    public FatalProcessingException(String string, Exception e) {
        super(string, e);
    }
}
