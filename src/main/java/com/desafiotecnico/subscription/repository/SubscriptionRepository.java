package com.desafiotecnico.subscription.repository;

import com.desafiotecnico.subscription.domain.Subscription;
import com.desafiotecnico.subscription.domain.SubscriptionStatus;
import org.springframework.data.domain.Pageable;
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

    /*
     * Busca as assinaturas que não possuiem transações de renovação em aberto.
     */
    @Query("SELECT s FROM Subscription s WHERE s.expirationDate = :date " +
            "AND NOT EXISTS (SELECT rt FROM RenewalTransaction rt WHERE rt.subscription = s AND rt.dataFinalizacao IS NULL)")
    List<Subscription> findSubscriptionToProccessPayment(@Param("date") LocalDate date, Pageable pageable);

}
