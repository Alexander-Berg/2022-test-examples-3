CREATE TABLE IF NOT EXISTS queue_tasks
(
    id                BIGSERIAL PRIMARY KEY,
    queue_name        TEXT NOT NULL,
    payload           TEXT,
    created_at        TIMESTAMP WITH TIME ZONE,
    next_process_at   TIMESTAMP WITH TIME ZONE,
    attempt           INTEGER DEFAULT 0,
    reenqueue_attempt INTEGER DEFAULT 0,
    total_attempt     INTEGER DEFAULT 0
);
