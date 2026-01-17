
CREATE INDEX idx_subscriptions_user_access ON subscriptions (user_id, status, expiration_date);

CREATE INDEX IF NOT EXISTS idx_payments_check_today ON payment_transactions (subscription_id, data_inicio);