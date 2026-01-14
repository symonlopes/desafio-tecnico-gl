package com.desafiotecnico.subscription.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {
    @NotNull(message = "User ID is required")
    @NotEmpty(message = "User ID is required")
    private UUID userId;
    @NotNull(message = "Plan is required")
    @NotEmpty(message = "Plan is required")
    private String plan;
}
