--liquibase formatted sql

--changeset kukabara:test_sc_order
CREATE TABLE IF NOT EXISTS test_sc_order
(
    yandex_id         TEXT PRIMARY KEY,
    partner_id        BIGINT      NOT NULL,
    order_partner_id  TEXT        NULL,
    external_order_id TEXT,
    status            TEXT        NOT NULL,

    delivery_date     TIMESTAMPTZ,
    courier           TEXT,

    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX test_sc_order_idx_order_partner_id ON test_sc_order (order_partner_id);
