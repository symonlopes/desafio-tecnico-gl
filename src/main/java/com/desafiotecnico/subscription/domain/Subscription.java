package com.desafiotecnico.subscription.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private String plan;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = true;

    @Column(name = "price_in_cents")
    private Integer priceInCents;

    private LocalDate startDate;
    private LocalDate expirationDate;
    private LocalDate lastRenewalDate;
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

}
