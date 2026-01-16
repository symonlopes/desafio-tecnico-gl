package com.desafiotecnico.subscription.repository;

import com.desafiotecnico.subscription.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findFirstByUserIdAndExpirationDateAfter(UUID userId, LocalDate date);
}
