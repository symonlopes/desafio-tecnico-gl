package com.desafiotecnico.subscription.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionEnqueuRequest {
    @Min(value = 1, message = "Amount must be at least 1")
    @Max(value = 500000, message = "Amount must be at most 50000")
    @Builder.Default
    private int limit = 1000;
}
