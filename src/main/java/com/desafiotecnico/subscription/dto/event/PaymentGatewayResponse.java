package com.desafiotecnico.subscription.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayResponse {

    private UUID transactionId;
    private boolean success;
    private String message;
}
