DROP TABLE IF EXISTS takeout_tasks;

CREATE TABLE takeout_tasks (
    id                  BIGSERIAL PRIMARY KEY,
    job_id              VARCHAR(255) NOT NULL,
    api_name            VARCHAR(255) NOT NULL,
    api_url             TEXT NOT NULL,
    status              VARCHAR(255) NOT NULL,
    file_description    TEXT,
    error               TEXT,
    submitted_at        TIMESTAMP without time zone NOT NULL,
    started_at          TIMESTAMP without time zone NULL,
    finished_at         TIMESTAMP without time zone NULL,
    updated_at          timestamp without time zone default (now()) not null

);
CREATE INDEX takeout_tasks_idx ON takeout_tasks (job_id);
