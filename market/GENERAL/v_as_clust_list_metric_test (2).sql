--liquibase formatted sql

--changeset lexhigh:MBO-33808_v_as_clust_list_metric_create_foreign_table context:"prod_only and fdw_enabled"
create foreign table if not exists site_catalog.v_as_clust_list_metric_test (
    ID                      bigint,
    NAME                    varchar(200),
    DATA_COUNT              bigint,
    NEW_PAIRS_COUNT         bigint,
    last_editor_login       VARCHAR(100),
    checking_state          smallint,
    ASSESS_TYPE             smallint,
    CHECK_DATE              timestamp with time zone,
    CLUSTER_PRECISION       numeric(8,4),
    CLUSTER_RECALL          numeric(8,4),
    CLUTCHER_RECALL         numeric(8,4),
    F1_CLUSTER              numeric(8,4),
    R1_CLUSTER              numeric(8,4),
    GROUP_ID                bigint,
    CREATE_DATE             timestamp with time zone,
    IS_ROBOT_LIST           smallint
) server ora_scat options (schema 'SITE_CATALOG', table 'V_AS_CLUST_LIST_METRIC_TEST');
