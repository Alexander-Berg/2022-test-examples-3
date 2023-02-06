--liquibase formatted sql

--changeset s-ermakov:MBO-17763_create_db_group_orchestrator
create table db_group_orchestrator
(
  group_id          NUMBER(2) NOT NULL
    CONSTRAINT db_group_orchestrator_pk
      PRIMARY KEY,
  client_id         NCHAR(36),
  client_name       VARCHAR2(1000),
  start_time        TIMESTAMP,
  ping_duration_sec NUMBER,
  last_ping_time    TIMESTAMP
);

--changeset s-ermakov:MBO-17763_insert_values endDelimiter:\\
BEGIN
  FOR x IN 0 .. 99
    LOOP
      INSERT INTO db_group_orchestrator (group_id)
      SELECT x FROM dual;
    END LOOP;
END;
\\
