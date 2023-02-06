CREATE TYPE payments.test_case AS ENUM (
    'test_ok_held',
    'test_ok_clear',
    'test_payment_failed',
    'test_moderation_failed'
);

ALTER TABLE payments.orders ADD COLUMN test payments.test_case DEFAULT NULL;