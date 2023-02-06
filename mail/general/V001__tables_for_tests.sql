CREATE SCHEMA IF NOT EXISTS sendr_qtools;

CREATE TYPE sendr_qtools.task_state as enum ('failed', 'pending', 'processing', 'finished', 'deleted', 'cleanup');
CREATE TYPE sendr_qtools.worker_state as enum ('running', 'shutdown', 'failed', 'cleanedup');
CREATE TYPE sendr_qtools.task_type as enum ('run_action', 'map', 'reduce');
CREATE TYPE sendr_qtools.worker_type as enum ('run_action', 'mapper', 'reducer');

CREATE TABLE sendr_qtools.merchants
(
    uid     BIGINT PRIMARY KEY,
    name    TEXT        NOT NULL,
    created TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE sendr_qtools.tasks
(
    task_id     BIGSERIAL                              NOT NULL
        CONSTRAINT tasks_pkey PRIMARY KEY,
    task_type   sendr_qtools.task_type                 NOT NULL,
    state       sendr_qtools.task_state                NOT NULL,
    params      JSONB,
    details     JSONB,
    retries     INTEGER                  DEFAULT 0     NOT NULL,
    run_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created     TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated     TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    action_name TEXT
);

CREATE TABLE sendr_qtools.workers
(
    worker_id   TEXT                      NOT NULL
        CONSTRAINT workers_pkey PRIMARY KEY,
    worker_type sendr_qtools.worker_type  NOT NULL,
    host        TEXT                      NOT NULL,
    state       sendr_qtools.worker_state NOT NULL,
    heartbeat   TIMESTAMP WITH TIME ZONE,
    startup     TIMESTAMP WITH TIME ZONE,
    task_id     BIGINT
        CONSTRAINT workers_task_id_fkey REFERENCES sendr_qtools.tasks
);

CREATE TABLE sendr_qtools.example_jsonb
(
    id    BIGINT PRIMARY KEY,
    data  JSONB NOT NULL DEFAULT '{}'::JSONB
);


CREATE TABLE public.settings
(
    key   TEXT                      NOT NULL
        CONSTRAINT settings_pkey PRIMARY KEY,
    value TEXT NOT NULL,
    created TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
