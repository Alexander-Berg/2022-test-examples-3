ALTER TABLE payments.subscriptions
    ADD COLUMN merchant_oauth_mode payments.merchant_oauth_mode default 'prod';
