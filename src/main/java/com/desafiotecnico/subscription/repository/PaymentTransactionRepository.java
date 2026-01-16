package com.desafiotecnico.subscription.repository;

import com.desafiotecnico.subscription.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findBySubscriptionIdAndStatus(UUID subscriptionId, String status);
}
