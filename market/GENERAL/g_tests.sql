--liquibase formatted sql

--changeset kukabara:g_client_insert context:"tests"
INSERT INTO g_client (id, is_deleted, timestamp, key_field_timestamp, predefined, deletion_mark, description, code, latitude, longitude)
VALUES ('970095AB-E0B5-C87F-B910-9719A9D2347A', false, 1, null, null, false, 'Мария', null, null, null);
