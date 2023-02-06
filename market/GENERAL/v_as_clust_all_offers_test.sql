--liquibase formatted sql

--changeset s-ermakov:MBO-19753_initial_changeset runOnChange:true
CREATE OR REPLACE FORCE VIEW "V_AS_CLUST_ALL_OFFERS_TEST" ("LIST_ID", "OFFER_ID") AS
  SELECT list_id,
    first_offer_id offer_id
  FROM as_clusterizer
  WHERE first_offer_id_cluster_test   IS NOT NULL AND snapshot_id = 0
  UNION ALL
  SELECT list_id,
    second_offer_id offer_id
  FROM as_clusterizer
  WHERE second_offer_id_cluster_test   IS NOT NULL AND snapshot_id = 0;
