package com.desafiotecnico.subscription.domain;

import lombok.Getter;

@Getter
public final class RenewalStatus {

    public static final RenewalStatus NEW = new RenewalStatus("NEW");
    public static final RenewalStatus PROCESSING = new RenewalStatus("PROCESSING");
    public static final RenewalStatus RENEWED = new RenewalStatus("RENEWED");
    public static final RenewalStatus FAILED = new RenewalStatus("FAILED");
    public static final RenewalStatus FINISHED = new RenewalStatus("FINISHED");

    private final String name;

    private RenewalStatus(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
