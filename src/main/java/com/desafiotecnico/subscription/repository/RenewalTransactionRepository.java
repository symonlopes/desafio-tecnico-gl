package com.desafiotecnico.subscription.repository;

import com.desafiotecnico.subscription.domain.RenewalTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RenewalTransactionRepository extends JpaRepository<RenewalTransaction, UUID> {
    Optional<RenewalTransaction> findBySubscriptionIdAndStatus(UUID subscriptionId, String status);
}
