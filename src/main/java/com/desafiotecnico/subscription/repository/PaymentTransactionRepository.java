package com.desafiotecnico.subscription.repository;

import com.desafiotecnico.subscription.domain.PaymentTransaction;
import com.desafiotecnico.subscription.domain.Subscription;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    @Modifying(clearAutomatically = true)
    @Query(value = """

            INSERT INTO
                payment_transactions (
                id,
                subscription_id,
                price_in_cents,
                status,
                data_inicio
            )
            SELECT
                gen_random_uuid(),
                s.id,
                s.price_in_cents,
                'CREATED',
                CAST(NOW() AS DATE)
            FROM subscriptions s
            WHERE s.expiration_date = CAST(NOW() AS DATE)
            AND s.status = 'ATIVA'
            AND s.auto_renew
            AND NOT EXISTS (
                SELECT 1
                FROM payment_transactions pt
                WHERE pt.subscription_id = s.id
                AND DATE(pt.data_inicio) = CAST(NOW() AS DATE)
            )
            LIMIT 10000;
                 """, nativeQuery = true)
    int generatePaymentTransactions();

    // TODO: falta o join para verificar se a inscrição está ativa.
    @Query(value = """
            SELECT pt.* FROM payment_transactions pt
            	join subscriptions s  on s.id = pt.subscription_id
                WHERE DATE(pt.data_inicio) = CAST(NOW() as DATE)
                AND pt.status = 'CREATED'
                AND pt.data_finalizacao IS NULL
                and s.status = 'ATIVA'
                """, nativeQuery = true)
    List<PaymentTransaction> findOpenPaymentTransactions(Pageable pageable);
}
