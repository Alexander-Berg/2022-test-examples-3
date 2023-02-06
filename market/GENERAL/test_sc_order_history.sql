--liquibase formatted sql

--changeset kukabara:test_sc_order_history
CREATE TABLE IF NOT EXISTS test_sc_order_history
(
    id                BIGSERIAL   NOT NULL
        CONSTRAINT test_sc_order_history_pkey
            PRIMARY KEY,

    yandex_id         TEXT
        CONSTRAINT fk_test_sc_order_history_yandex_id
            REFERENCES test_sc_order (yandex_id),

    partner_id        BIGINT      NOT NULL,
    order_partner_id  TEXT        NULL,
    external_order_id TEXT,

    status            TEXT        NOT NULL,
    delivery_date     TIMESTAMPTZ,
    courier           TEXT,

    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX test_sc_order_history_idx_yandex_id ON test_sc_order_history (yandex_id);
CREATE INDEX test_sc_order_history_idx_order_partner_id ON test_sc_order_history (order_partner_id);

