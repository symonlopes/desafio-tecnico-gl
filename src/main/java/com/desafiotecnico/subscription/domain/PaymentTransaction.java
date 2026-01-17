package com.desafiotecnico.subscription.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    private String status;

    private LocalDateTime dataInicio;

    private LocalDateTime dataFinalizacao;

    @Column(name = "price_in_cents")
    private Integer priceInCents;

    @Column(name = "cancellation_reason")
    private String cancellationReason;
}
