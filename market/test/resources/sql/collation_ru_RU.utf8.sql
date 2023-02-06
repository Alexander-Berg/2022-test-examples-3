--liquibase formatted sql

--changeset vdorogin:LILUCRM-4957_create_collation_ru_RU.utf8
CREATE COLLATION IF NOT EXISTS "ru_RU.utf8" (provider = icu, locale = 'ru_RU.utf8');