--liquibase formatted sql

--changeset munchkim:MARKETFF-5055
CREATE SCHEMA IF NOT EXISTS dbqueue;

CREATE TABLE dbqueue.task
(
    id                BIGSERIAL PRIMARY KEY,
    queue_name        TEXT NOT NULL,
    payload           TEXT,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT now(),
    next_process_at   TIMESTAMP WITH TIME ZONE DEFAULT now(),
    attempt           INTEGER                  DEFAULT 0,
    reenqueue_attempt INTEGER                  DEFAULT 0,
    total_attempt     INTEGER                  DEFAULT 0
);

CREATE
INDEX queue_task_name_time_desc_idx ON dbqueue.task USING btree (queue_name, next_process_at, id DESC);

CREATE
UNIQUE INDEX IF NOT EXISTS payload_queue_name_unique_index ON dbqueue.task(payload, queue_name);

CREATE TABLE dbqueue.queue_log
(
    id         BIGSERIAL PRIMARY KEY,
    queue_name TEXT   NOT NULL,
    event      TEXT   NOT NULL,
    entity_id  BIGINT NOT NULL,
    task_id    BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    host_name  VARCHAR(256),
    payload    TEXT
);

create
index queue_log_entity_id_index on dbqueue.queue_log (entity_id);

