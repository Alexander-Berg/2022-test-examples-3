--liquibase formatted sql

--changeset e-golov:LILUCRM-1417-test_uids_and_test_uid_groups_tables
CREATE TABLE test_puid_groups (
  id VARCHAR PRIMARY KEY,
  name VARCHAR NOT NULL
);

CREATE TABLE test_puids (
    id SERIAL PRIMARY KEY,
    puid BIGINT NOT NULL,
    name VARCHAR,
    group_id VARCHAR REFERENCES test_puid_groups(id) ON DELETE CASCADE DEFAULT 'default',
    UNIQUE (group_id, puid)
);

INSERT INTO test_puid_groups (id, name)
VALUES ('default', 'По умолчанию');