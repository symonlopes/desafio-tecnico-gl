package com.desafiotecnico.subscription.repository;

import com.desafiotecnico.subscription.domain.Subscription;
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
     * Só dispara evento de renovação para assinaturas ativas.
     */
    @Query(value = "SELECT s.* FROM subscriptions s " +
            "WHERE s.expiration_date = :date " +
            "AND s.status = 'ATIVA' " +
            "AND NOT EXISTS (SELECT 1 FROM renewal_transactions rt WHERE rt.subscription_id = s.id AND rt.data_finalizacao IS NULL)", nativeQuery = true)
    List<Subscription> findSubscriptionToProccessPayment(@Param("date") LocalDate date, Pageable pageable);

}
