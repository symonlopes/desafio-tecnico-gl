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

    public static Plan fromName(String name) {
        if (BASICO.name.equals(name))
            return BASICO;
        if (PREMIUM.name.equals(name))
            return PREMIUM;
        if (FAMILIA.name.equals(name))
            return FAMILIA;
        throw new IllegalArgumentException("Invalid plan name: " + name);
    }
}
