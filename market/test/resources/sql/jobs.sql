DROP TABLE IF EXISTS jobs;

CREATE TABLE jobs
(
    id             BIGSERIAL PRIMARY KEY,
    job_id         VARCHAR(255)                 NOT NULL,
    params         TEXT                         NOT NULL,
    submitted_at   TIMESTAMP without time zone  NOT NULL,
    started_at     TIMESTAMP without time zone  NULL,
    finished_at    TIMESTAMP without time zone  NULL,
    status         VARCHAR(127)                 NOT NULL,
    result         TEXT,
    host           VARCHAR(255),
    submitted_from VARCHAR(255),
    updated_at     TIMESTAMP without time zone default (now()) NOT NULL
);

ALTER TABLE jobs
    ADD CONSTRAINT job_id_idx UNIQUE (job_id);
