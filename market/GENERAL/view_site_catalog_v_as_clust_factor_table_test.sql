-- Automatically saved for VIEW.SITE_CATALOG.V_AS_CLUST_FACTOR_TABLE_TEST at 2022/04/12

  CREATE OR REPLACE FORCE EDITIONABLE VIEW "SITE_CATALOG"."V_AS_CLUST_FACTOR_TABLE_TEST" ("LIST_ID", "TP", "FP", "FN", "TN") AS 
  SELECT list_id,
      SUM(tp) tp,
      SUM(fp) fp,
      SUM(fn) fn,
      SUM(tn) tn
    FROM v_as_clust_check_test
    GROUP BY list_id