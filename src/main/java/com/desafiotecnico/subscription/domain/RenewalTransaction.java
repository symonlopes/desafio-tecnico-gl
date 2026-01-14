package com.desafiotecnico.subscription.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "renewal_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewalTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    private String status;

    private LocalDateTime dataInicio;

    private LocalDateTime dataFinalizacao;

    private Integer paymentAttempts;
}
