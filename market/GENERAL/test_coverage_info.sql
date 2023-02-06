--liquibase formatted sql

--changeset virtual-marat:initial
CREATE TABLE t2m.test_coverage_info
(
    id                  text NOT NULL,
    branch              text NOT NULL,
    pull_request        text,
    project             text,
    subproject          text,
    changed             timestamp with time zone          NOT NULL,
    statements_coverage double precision                  NOT NULL,
    branches_coverage   double precision                  NOT NULL,
    functions_coverage  double precision                  NOT NULL,
    lines_coverage      double precision                  NOT NULL,
    CONSTRAINT test_coverage_info_pkey PRIMARY KEY (id)
)

--changeset virtual-marat:MBO-29257

create sequence t2m.test_coverage_info_id_seq;
alter table t2m.test_coverage_info alter id set default nextval('t2m.test_coverage_info_id_seq');
alter sequence t2m.test_coverage_info_id_seq owned by t2m.test_coverage_info.id;

--changeset goryunov-se:MBO-33485

ALTER TABLE t2m.test_coverage_info 
ADD COLUMN statement_covered int, 
ADD COLUMN statement_total int, 
ADD COLUMN statement_skipped int, 
ADD COLUMN branches_covered int, 
ADD COLUMN branches_total int, 
ADD COLUMN branches_skipped int, 
ADD COLUMN functions_covered int, 
ADD COLUMN functions_total int, 
ADD COLUMN functions_skipped int, 
ADD COLUMN lines_covered int, 
ADD COLUMN lines_total int, 
ADD COLUMN lines_skipped int;
