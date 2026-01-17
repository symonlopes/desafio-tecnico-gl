CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID,
    plan VARCHAR(255),
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    price_in_cents INTEGER,
    start_date DATE,
    expiration_date DATE,
    last_renewal_date DATE,
    cancel_reason VARCHAR(255),
    status VARCHAR(255)
);

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    status VARCHAR(255),
    data_inicio TIMESTAMP,
    data_finalizacao TIMESTAMP,
    price_in_cents INTEGER,
    cancellation_reason VARCHAR(255),
    CONSTRAINT fk_payment_transactions_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id)
);