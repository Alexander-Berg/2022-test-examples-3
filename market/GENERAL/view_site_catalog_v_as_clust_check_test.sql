-- Automatically saved for VIEW.SITE_CATALOG.V_AS_CLUST_CHECK_TEST at 2022/04/12

  CREATE OR REPLACE FORCE EDITIONABLE VIEW "SITE_CATALOG"."V_AS_CLUST_CHECK_TEST" ("LIST_ID", "FIRST_OFFER_ID", "SECOND_OFFER_ID", "OPERATOR_STATUS", "FIRST_OFFER_ID_CLUSTER", "SECOND_OFFER_ID_CLUSTER", "TP", "FP", "TN", "FN") AS 
  SELECT clust."LIST_ID",
    clust."FIRST_OFFER_ID",
    clust."SECOND_OFFER_ID",
    clust."OPERATOR_STATUS",
    clust."FIRST_OFFER_ID_CLUSTER_TEST",
    clust."SECOND_OFFER_ID_CLUSTER_TEST",
    CASE
      WHEN clust.operator_status = 1
      AND first_offer_id_cluster_test = second_offer_id_cluster_test
      THEN 1
      ELSE 0
    END tp,
    CASE
      WHEN clust.operator_status = 2
      AND first_offer_id_cluster_test = second_offer_id_cluster_test
      THEN 1
      ELSE 0
    END fp,
    CASE
      WHEN clust.operator_status  = 2
      AND first_offer_id_cluster_test != second_offer_id_cluster_test
      THEN 1
      ELSE 0
    END tn,
    CASE
      WHEN clust.operator_status  = 1
      AND first_offer_id_cluster_test != second_offer_id_cluster_test
      THEN 1
      ELSE 0
    END fn
  FROM as_clusterizer clust
  WHERE NVL(first_offer_id_cluster_test,      -1) != -1
  AND NVL(second_offer_id_cluster_test,       -1) != -1
  AND clust.operator_status             IN (1, 2)
  AND snapshot_id = 0