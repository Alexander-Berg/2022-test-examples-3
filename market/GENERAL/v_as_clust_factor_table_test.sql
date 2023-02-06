--liquibase formatted sql

--changeset s-ermakov:MBO-19753_initial_changeset runOnChange:true
CREATE OR REPLACE FORCE VIEW "V_AS_CLUST_FACTOR_TABLE_TEST" ("LIST_ID", "TP", "FP", "FN", "TN") AS
    SELECT list_id,
      SUM(tp) tp,
      SUM(fp) fp,
      SUM(fn) fn,
      SUM(tn) tn
    FROM v_as_clust_check_test
    GROUP BY list_id;
