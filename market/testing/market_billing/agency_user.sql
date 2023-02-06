--liquibase formatted sql

--changeset trishlex:MBI-43157
-- таблица только для тестинга
CREATE TABLE MARKET_BILLING.AGENCY_USER (
    AGENCY_ID NUMBER NOT NULL,
    USER_ID NUMBER NOT NULL
);

--changeset trishlex:MBI-43157-agency_user-comments
COMMENT ON TABLE MARKET_BILLING.AGENCY_USER IS 'Таблица для хранения пользователей агенств. Только для тестинга для восстановления партнеров';
COMMENT ON COLUMN MARKET_BILLING.AGENCY_USER.AGENCY_ID IS 'id агенства';
COMMENT ON COLUMN MARKET_BILLING.AGENCY_USER.USER_ID IS 'id пользователя';