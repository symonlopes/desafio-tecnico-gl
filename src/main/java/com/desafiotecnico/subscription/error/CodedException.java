package com.desafiotecnico.subscription.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class CodedException extends RuntimeException {
    private final String code;
    private Object details;

    public CodedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public CodedException(String code, String message, Object details) {
        super(message);
        this.code = code;
        this.details = details;
    }
}
