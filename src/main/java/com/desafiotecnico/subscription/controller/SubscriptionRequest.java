package com.desafiotecnico.subscription.controller;

import com.desafiotecnico.subscription.domain.PlanType;
import lombok.Data;

import java.util.UUID;

@Data
public class SubscriptionRequest {
    private UUID userId;
    private PlanType plan;
}
