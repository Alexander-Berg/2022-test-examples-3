--liquibase formatted sql

--changeset s-ermakov:MBO-19753_initial_changeset runOnChange:true
CREATE OR REPLACE FORCE VIEW "V_AS_CLUST_LIST_METRIC_TEST" ("ID", "NAME", "DATA_COUNT", "NEW_PAIRS_COUNT", "LAST_EDITOR_LOGIN", "CHECKING_STATE", "ASSESS_TYPE", "CHECK_DATE", "CLUSTER_PRECISION", "CLUSTER_RECALL", "CLUTCHER_RECALL", "F1_CLUSTER", "R1_CLUSTER", "GROUP_ID", "CREATE_DATE", "IS_ROBOT_LIST") AS
    SELECT "ID",
      list_info.name
      || ' ('
      || LIST_INFO.DATA_COUNT
      || ')' NAME,
      LIST_INFO.DATA_COUNT,
      nvl(NEW_PAIRS.COUNT_NEW, 0) NEW_PAIRS_COUNT,
      list_info.last_editor_login,
      list_info.checking_state,
      "ASSESS_TYPE",
      "CHECK_DATE",
      "CLUSTER_PRECISION",
      "CLUSTER_RECALL",
      "CLUTCHER_RECALL",
      "F1_CLUSTER",
      "R1_CLUSTER",
      LIST_INFO.GROUP_ID,
      LIST_INFO.CREATE_DATE,
      LIST_INFO.IS_ROBOT_LIST
    FROM as_offer_list_info list_info
      LEFT JOIN v_as_clust_list_new_pairs NEW_PAIRS ON list_info.id = NEW_PAIRS.list_id
      LEFT JOIN
      (SELECT list_id,
         ROUND(DECODE(CLUSTER_PRECISION, -1, 0, CLUSTER_PRECISION) * 100, 4) CLUSTER_PRECISION,
         ROUND(DECODE(CLUSTER_RECALL,    -1, 0, CLUSTER_RECALL) * 100, 4) CLUSTER_RECALL,
         ROUND(DECODE(CLUTCHER_RECALL,   -1, 0, CLUTCHER_RECALL) * 100, 4) CLUTCHER_RECALL,
         CASE WHEN CLUSTER_PRECISION + CLUSTER_RECALL = 0 THEN
           -1
         ELSE
           ROUND(2                         * CLUSTER_PRECISION * CLUSTER_RECALL / (CLUSTER_PRECISION + CLUSTER_RECALL) * 100, 4)
         END F1_CLUSTER,
         ROUND((TP                       + TN) / (TP + TN + FN+FP) * 100, 4) R1_CLUSTER
       FROM
         (SELECT list_id,
            fct_tbl.TP,
            fct_tbl.TN,
            fct_tbl.FN,
            fct_tbl.FP,
            CASE
            WHEN TP         + FP != 0
              THEN fct_tbl.TP / (TP + FP)
            ELSE            -1
            END CLUSTER_PRECISION,
            CASE
            WHEN TP         + FN != 0
              THEN fct_tbl.TP / (TP + FN)
            ELSE            -1
            END CLUSTER_RECALL,
            (SELECT COUNT(DISTINCT offer_id)
             FROM v_as_clust_clutched_offer_test
             WHERE list_id = fct_tbl.list_id
            ) /
            (SELECT COUNT(DISTINCT offer_id)
             FROM v_as_clust_all_offers_test
             WHERE list_id = fct_tbl.list_id
            ) CLUTCHER_RECALL
          FROM v_as_clust_factor_table_test fct_tbl
         )
      ) metrics
        ON LIST_INFO.ID             = METRICS.LIST_ID
    WHERE list_info.assess_type = 2;
