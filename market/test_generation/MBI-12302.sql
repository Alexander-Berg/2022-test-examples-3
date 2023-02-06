--liquibase formatted sql

--changeset disproper:MBI-12302_use_hdfs_test_generation_table_update
ALTER TABLE shops_web.test_generation ADD sc_version varchar2(16)
;
UPDATE shops_web.test_generation set sc_version='19700101_0001'
;
ALTER TABLE shops_web.test_generation
MODIFY (
sc_version not null
)
;