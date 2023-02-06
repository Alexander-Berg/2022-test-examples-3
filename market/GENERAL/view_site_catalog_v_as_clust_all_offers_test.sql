-- Automatically saved for VIEW.SITE_CATALOG.V_AS_CLUST_ALL_OFFERS_TEST at 2022/04/12

  CREATE OR REPLACE FORCE EDITIONABLE VIEW "SITE_CATALOG"."V_AS_CLUST_ALL_OFFERS_TEST" ("LIST_ID", "OFFER_ID") AS 
  SELECT list_id,
    first_offer_id offer_id
  FROM as_clusterizer
  WHERE first_offer_id_cluster_test   IS NOT NULL AND snapshot_id = 0
  UNION ALL
  SELECT list_id,
    second_offer_id offer_id
  FROM as_clusterizer
  WHERE second_offer_id_cluster_test   IS NOT NULL AND snapshot_id = 0