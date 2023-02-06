CREATE TABLE IF NOT EXISTS dbqueue
(
    id                BIGSERIAL NOT NULL PRIMARY KEY,
    queue_name        TEXT      NOT NULL,
    payload           TEXT,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT now(),
    next_process_at   TIMESTAMP WITH TIME ZONE DEFAULT now(),
    attempt           INTEGER                  DEFAULT 0,
    reenqueue_attempt INTEGER                  DEFAULT 0,
    total_attempt     INTEGER                  DEFAULT 0
);

CREATE TABLE IF NOT EXISTS task_log
(
    id                  BIGSERIAL NOT NULL PRIMARY KEY,
    task_id             BIGINT NOT NULL REFERENCES dbqueue(id) ON DELETE CASCADE,
    message             TEXT,
    request_id          TEXT
)
