package com.desafiotecnico.subscription.domain;

import lombok.Getter;

@Getter
public final class Plan {

    public static final Plan BASICO = new Plan("BASICO", 1990);
    public static final Plan PREMIUM = new Plan("PREMIUM", 3990);
    public static final Plan FAMILIA = new Plan("FAMILIA", 5990);

    private final String name;
    private final int priceInCents;

    public Plan(String name, int priceInCents) {
        this.name = name;
        this.priceInCents = priceInCents;
    }
}
