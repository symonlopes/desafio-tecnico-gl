package com.desafiotecnico.subscription.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CodedException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(CodedException ex) {
        ApiError error = ApiError.builder()
                .code(ex.getCode())
                .description(ex.getMessage())
                .details(ex.getDetails())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ApiError error = ApiError.builder()
                .code("VALIDATION_ERROR")
                .description("Validation failed")
                .details(errors)
                .build();
        return ResponseEntity.badRequest().body(error);
    }
}
