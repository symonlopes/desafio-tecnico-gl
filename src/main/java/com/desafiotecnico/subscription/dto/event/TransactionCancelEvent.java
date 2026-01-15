package com.desafiotecnico.subscription.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCancelEvent {
    private UUID transactionId;
    private UUID subscriptionId;
    private String reason;
    private LocalDateTime occurredAt;
}
