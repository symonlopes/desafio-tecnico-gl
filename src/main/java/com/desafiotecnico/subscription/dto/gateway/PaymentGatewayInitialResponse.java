package com.desafiotecnico.subscription.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayInitialResponse {
    private String customId;
    private String errorCode;
    private String errorDescription;
}
