package com.desafiotecnico.subscription.domain;

import lombok.Getter;

@Getter
public enum PlanType {
    BASICO(1990),
    PREMIUM(3990),
    FAMILIA(5990);

    private final int priceInCents;

    PlanType(int priceInCents) {
        this.priceInCents = priceInCents;
    }
}
