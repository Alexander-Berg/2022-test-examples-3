CREATE TABLE IF NOT EXISTS dbqueue (
    id            BIGSERIAL PRIMARY KEY,
    queue_name    VARCHAR(128) NOT NULL,
    task          TEXT,
    create_time   TIMESTAMP WITH TIME ZONE DEFAULT now(),
    process_time  TIMESTAMP WITH TIME ZONE DEFAULT now(),
    attempt       INTEGER                  DEFAULT 0,
    actor         VARCHAR(128),
    log_timestamp VARCHAR(128)
) WITH (fillfactor = 80);

CREATE TABLE IF NOT EXISTS task_log
(
    id                  BIGSERIAL NOT NULL PRIMARY KEY,
    task_id             BIGINT NOT NULL REFERENCES dbqueue(id) ON DELETE CASCADE,
    message             TEXT,
    request_id          TEXT
)