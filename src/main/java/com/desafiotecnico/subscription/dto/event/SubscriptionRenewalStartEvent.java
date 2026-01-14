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
public class SubscriptionRenewalStartEvent {
    private UUID subscriptionId;
    private UUID transactionId;
    private Integer priceInCents;
}
