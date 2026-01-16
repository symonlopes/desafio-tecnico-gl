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
public class PaymentTransactionCreationRequest {

    @Builder.Default
    private LocalDate dateToProcess = LocalDate.now();
}
