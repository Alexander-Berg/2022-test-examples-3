--liquibase formatted sql

--changeset hjujgfg:DELIVERY-8432
CREATE TABLE qrtz_blob_triggers
(
    trigger_name  character varying(80)                                             NOT NULL,
    trigger_group character varying(80)                                             NOT NULL,
    blob_data     text,
    sched_name    character varying(120) DEFAULT 'TestScheduler'::character varying NOT NULL
);


CREATE TABLE qrtz_calendars
(
    calendar_name character varying(80)                                             NOT NULL,
    calendar      text                                                              NOT NULL,
    sched_name    character varying(120) DEFAULT 'TestScheduler'::character varying NOT NULL
);


CREATE TABLE qrtz_cron_triggers
(
    trigger_name    character varying(80)                                             NOT NULL,
    trigger_group   character varying(80)                                             NOT NULL,
    cron_expression character varying(80)                                             NOT NULL,
    time_zone_id    character varying(80),
    sched_name      character varying(120) DEFAULT 'TestScheduler'::character varying NOT NULL
);


CREATE TABLE qrtz_fired_triggers
(
    entry_id          character varying(95)                                             NOT NULL,
    trigger_name      character varying(80)                                             NOT NULL,
    trigger_group     character varying(80)                                             NOT NULL,
    instance_name     character varying(80)                                             NOT NULL,
    fired_time        bigint                                                            NOT NULL,
    priority          integer                                                           NOT NULL,
    state             character varying(16)                                             NOT NULL,
    job_name          character varying(80),
    job_group         character varying(80),
    is_nonconcurrent  boolean,
    is_update_data    boolean,
    sched_name        character varying(120) DEFAULT 'TestScheduler'::character varying NOT NULL,
    sched_time        bigint                                                            NOT NULL,
    requests_recovery boolean
);


CREATE TABLE qrtz_job_details
(
    job_name          character varying(128)                                            NOT NULL,
    job_group         character varying(80)                                             NOT NULL,
    description       character varying(120),
    job_class_name    character varying(200)                                            NOT NULL,
    is_durable        boolean,
    is_nonconcurrent  boolean,
    is_update_data    boolean,
    sched_name        character varying(120) DEFAULT 'TestScheduler'::character varying NOT NULL,
    requests_recovery boolean,
    job_data          bytea
);


CREATE TABLE qrtz_locks
(
    lock_name  character varying(40)                                             NOT NULL,
    sched_name character varying(120) DEFAULT 'TestScheduler'::character varying NOT NULL
);


INSERT INTO qrtz_locks
VALUES ('trigger_access');
INSERT INTO qrtz_locks
VALUES ('job_access');
INSERT INTO qrtz_locks
VALUES ('calendar_access');
INSERT INTO qrtz_locks
VALUES ('state_access');
INSERT INTO qrtz_locks
VALUES ('misfire_access');


CREATE TABLE qrtz_paused_trigger_grps
(
    trigger_group character varying(80)                                             NOT NULL,
    sched_name    character varying(120) DEFAULT 'TestScheduler'::character varying NOT NULL
);


CREATE TABLE qrtz_scheduler_state
(
    instance_name     character varying(200)                                            NOT NULL,
    last_checkin_time bigint,
    checkin_interval  bigint,
    sched_name        character varying(120) DEFAULT 'TestScheduler'::character varying NOT NULL
);


CREATE TABLE qrtz_simple_triggers
(
    trigger_name    character varying(80)                                             NOT NULL,
    trigger_group   character varying(80)                                             NOT NULL,
    repeat_count    bigint                                                            NOT NULL,
    repeat_interval bigint                                                            NOT NULL,
    times_triggered bigint                                                            NOT NULL,
    sched_name      character varying(120) DEFAULT 'TestScheduler'::character varying NOT NULL
);


CREATE TABLE qrtz_simprop_triggers
(
    sched_name    character varying(120) NOT NULL,
    trigger_name  character varying(200) NOT NULL,
    trigger_group character varying(200) NOT NULL,
    str_prop_1    character varying(512),
    str_prop_2    character varying(512),
    str_prop_3    character varying(512),
    int_prop_1    integer,
    int_prop_2    integer,
    long_prop_1   bigint,
    long_prop_2   bigint,
    dec_prop_1    numeric(13, 4),
    dec_prop_2    numeric(13, 4),
    bool_prop_1   boolean,
    bool_prop_2   boolean
);


CREATE TABLE qrtz_triggers
(
    trigger_name   character varying(80)                                             NOT NULL,
    trigger_group  character varying(80)                                             NOT NULL,
    job_name       character varying(80)                                             NOT NULL,
    job_group      character varying(80)                                             NOT NULL,
    description    character varying(120),
    next_fire_time bigint,
    prev_fire_time bigint,
    priority       integer,
    trigger_state  character varying(16)                                             NOT NULL,
    trigger_type   character varying(8)                                              NOT NULL,
    start_time     bigint                                                            NOT NULL,
    end_time       bigint,
    calendar_name  character varying(80),
    misfire_instr  smallint,
    job_data       bytea,
    sched_name     character varying(120) DEFAULT 'TestScheduler'::character varying NOT NULL
);


CREATE TABLE qrtz_log
(
    ID                SERIAL PRIMARY KEY,
    JOB_NAME          VARCHAR,
    JOB_GROUP         VARCHAR,
    TRIGGER_FIRE_TIME TIMESTAMP,
    JOB_FINISHED_TIME TIMESTAMP,
    JOB_STATUS        VARCHAR,
    HOST_NAME         VARCHAR
);


CREATE TABLE job_monitoring_config
(
    job_name                varchar(256) NOT NULL
        CONSTRAINT job_monitoring_config_pkey
            PRIMARY KEY,
    max_delay_time          INTEGER      NOT NULL,
    warn_delay_time         INTEGER      NOT NULL DEFAULT 0,
    max_execution_time      INTEGER      NOT NULL,
    warn_execution_time     INTEGER      NOT NULL DEFAULT 0,
    max_failed_runs         INTEGER      NOT NULL DEFAULT 1,
    warn_failed_runs        INTEGER      NOT NULL DEFAULT 0,
    runs_number_to_consider INTEGER      NOT NULL DEFAULT 1,
    tracking_start_time     TIMESTAMP    NOT NULL DEFAULT current_timestamp
);


