package com.desafiotecnico.subscription.repository;

import com.desafiotecnico.subscription.domain.PaymentTransaction;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
                CURRENT_DATE
            FROM subscriptions s
            WHERE s.expiration_date = CURRENT_DATE
            AND s.status = 'ATIVA'
            AND s.auto_renew
            AND NOT EXISTS (
                SELECT 1
                FROM payment_transactions pt
                WHERE pt.subscription_id = s.id
                AND pt.data_inicio >= CURRENT_DATE AND pt.data_inicio < CURRENT_DATE + 1
            )
            LIMIT 10000;
                 """, nativeQuery = true)
    int generatePaymentTransactions();

    @Query(value = """
            SELECT pt.* FROM payment_transactions pt
            WHERE pt.data_inicio >= CURRENT_DATE
              AND pt.data_inicio < CURRENT_DATE + 1
              AND pt.status = 'CREATED'
              AND pt.data_finalizacao IS NULL;
                """, nativeQuery = true)
    List<PaymentTransaction> findOpenPaymentTransactions(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query(value = """
                UPDATE payment_transactions
                SET status = 'PROCESSING'
                WHERE id IN (
                    SELECT id
                    FROM payment_transactions
                    WHERE status = 'CREATED'
                    AND data_inicio <= CURRENT_DATE
                    ORDER BY data_inicio ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                )
                RETURNING *;
            """, nativeQuery = true)
    List<PaymentTransaction> findAndMarkBatchAsProcessing(@Param("limit") int limit);

}
