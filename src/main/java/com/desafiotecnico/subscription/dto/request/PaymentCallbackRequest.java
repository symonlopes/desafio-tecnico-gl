package com.desafiotecnico.subscription.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;

    private boolean success;

    private String message;
}
