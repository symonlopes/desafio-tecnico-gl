package com.desafiotecnico.subscription.repository;

import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findFirstByUserIdAndExpirationDateAfter(UUID userId, LocalDate date);

    boolean existsByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.status = 'ATIVA' AND s.expirationDate > CURRENT_DATE")
    List<Subscription> findActiveValidSubscriptions(@Param("userId") UUID userId);
}
