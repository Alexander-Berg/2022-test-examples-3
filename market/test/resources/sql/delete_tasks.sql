DROP TABLE IF EXISTS delete_tasks;
DROP SEQUENCE IF EXISTS deleted_task_lock_id_sequence;

CREATE SEQUENCE IF NOT EXISTS deleted_task_lock_id_sequence
    AS INT
    CYCLE
    OWNED BY NONE;

CREATE TABLE IF NOT EXISTS delete_tasks
(
    lock_id        INT                                         NOT NULL DEFAULT NEXTVAL('deleted_task_lock_id_sequence'),
    uid            BIGINT                                      NOT NULL,
    fake_uid       BIGINT                                      NOT NULL,
    type           VARCHAR(30)                                 NOT NULL,
    service        VARCHAR(100)                                NOT NULL,
    data           VARCHAR(30)                                 NOT NULL,
    status         VARCHAR(30) default ('RUNNING')             NOT NULL,
    submitted_at   TIMESTAMP without time zone                 NOT NULL,
    started_at     TIMESTAMP without time zone                 NULL,
    finished_at    TIMESTAMP without time zone                 NULL,
    updated_at     TIMESTAMP without time zone default (now()) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS unique_started_tasks ON delete_tasks (uid, type, service, data, status)
    WHERE status = 'RUNNING';


INSERT INTO delete_tasks (uid, fake_uid, type, service, data, status, submitted_at)
    VALUES (1, 101, 'takeout', 'pers-qa', 'qa', 'COMPLETED', now() - INTERVAL '1 YEAR 1 DAY');
INSERT INTO delete_tasks (uid, fake_uid, type, service, data, status, submitted_at)
    VALUES (2, 102, 'takeout', 'pers-qa', 'qa', 'HARD_DELETED', now() - INTERVAL '1 YEAR 1 DAY');
INSERT INTO delete_tasks (uid, fake_uid, type, service, data, status, submitted_at)
    VALUES (3, 103, 'takeout', 'pers-qa', 'qa', 'COMPLETED', now() - INTERVAL '180 DAY');
INSERT INTO delete_tasks (uid, fake_uid, type, service, data, status, submitted_at)
    VALUES (4, 104, 'service', 'checkouter', 'checkouter', 'COMPLETED', now() - INTERVAL '3 YEAR 1 DAY');
INSERT INTO delete_tasks (uid, fake_uid, type, service, data, status, submitted_at)
    VALUES (5, 105, 'service', 'checkouter', 'checkouter', 'HARD_DELETED', now() - INTERVAL '3 YEAR 1 DAY');
INSERT INTO delete_tasks (uid, fake_uid, type, service, data, status, submitted_at)
    VALUES (6, 106, 'service', 'checkouter', 'checkouter', 'COMPLETED', now() - INTERVAL '2 YEAR');
INSERT INTO delete_tasks (uid, fake_uid, type, service, data, status, submitted_at)
    VALUES (7, 107, 'service', 'pers-basket', 'basket', 'COMPLETED', now() - INTERVAL '2 YEAR');
