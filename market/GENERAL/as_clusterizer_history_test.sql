--liquibase formatted sql

--changeset lexhigh:MBO-33808_as_clusterizer_history_test_create_table
create table if not exists site_catalog.as_clusterizer_history_test
(
    ID          BIGINT,
    METRIC      BIGINT,
    CHECK_DATE  TIMESTAMP WITH TIME ZONE,
    METRIC_TYPE VARCHAR(20),
    DATA_COUNT  BIGINT
);

--changeset lexhigh:MBO-38245_AS_CLUSTERIZER_HISTORY_TEST_create_fwd context:"prod_only and fdw_enabled" endDelimiter:\\
alter table as_clusterizer_history_test rename to migration_as_clusterizer_history_test;
create foreign table if not exists site_catalog.as_clusterizer_history_test (
    id          bigint,
    metric      numeric(8,4),
    check_date  timestamp with time zone,
    metric_type varchar(20),
    data_count  bigint
) server ora_scat options (schema 'SITE_CATALOG', table 'AS_CLUSTERIZER_HISTORY_TEST');
alter table site_catalog.as_clusterizer_history_test alter column metric TYPE numeric(8,4);
\\

--changeset lexhigh:MBO-38245_AS_CLUSTERIZER_HISTORY_TEST_analyze context:"prod_only and fdw_enabled"
ANALYSE site_catalog.as_clusterizer_history_test;
