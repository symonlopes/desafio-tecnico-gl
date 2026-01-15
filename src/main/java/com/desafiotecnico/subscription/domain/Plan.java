package com.desafiotecnico.subscription.domain;

import lombok.Getter;

@Getter
public enum Plan {

    BASICO("BASICO", 1990),
    PREMIUM("PREMIUM", 3990),
    FAMILIA("FAMILIA", 5990);

    private final String name;
    private final int priceInCents;

    Plan(String name, int priceInCents) {
        this.name = name;
        this.priceInCents = priceInCents;
    }

    public static Plan fromName(String name) {
        for (Plan plan : values()) {
            if (plan.name.equals(name)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Invalid plan name: " + name);
    }
}
