CREATE TABLE IF NOT EXISTS scheduled_tasks_log
(
    id          BIGSERIAL PRIMARY KEY NOT NULL,
    name        TEXT                  NOT NULL,
    host        TEXT                  NOT NULL,
    start_time  BIGINT                NOT NULL,
    finish_time BIGINT,
    status      TEXT,
    fail_cause  TEXT
);
