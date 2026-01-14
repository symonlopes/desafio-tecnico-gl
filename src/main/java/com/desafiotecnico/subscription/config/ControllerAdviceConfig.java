package com.desafiotecnico.subscription.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("subscriptions_plan_check")) {
            ApiError error = ApiError.builder()
                    .code("INVALID_PLAN")
                    .description("Invalid plan selected")
                    .build();
            return ResponseEntity.badRequest().body(error);
        }

        // Fallback for other data integrity violations
        ApiError error = ApiError.builder()
                .code("DATA_INTEGRITY_VIOLATION")
                .description("Data integrity violation")
                .build();
        return ResponseEntity.status(409).body(error);
    }
}
