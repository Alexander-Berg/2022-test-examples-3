--liquibase formatted sql

--changeset nastik:MBI-16584_add_cpa_categories_msk_1 endDelimiter:///
DECLARE
 PROCEDURE ADD_CATEGORY(hyperId NUMBER, cpaType NUMBER, fee NUMBER, regionIdArray shops_web.t_number_tbl) IS
    BEGIN
  MERGE INTO MARKET_BILLING.CPA_CATEGORIES t
  USING (SELECT
           hyperId             hyper_id,
           cpaType    cpa_type,
           fee             fee
         FROM dual) n ON (n.hyper_id = t.hyper_id and n.cpa_type = t.cpa_type)
  WHEN MATCHED THEN
  UPDATE SET t.fee = n.fee
  WHEN NOT MATCHED THEN
  INSERT (hyper_id, cpa_type, fee) VALUES (n.hyper_id, n.cpa_type, n.fee);

  MERGE INTO MARKET_BILLING.CPA_CATEGORY_REGIONS tar
    USING (
            select hyperId hyper_id,
            value(ids) region_id from table(regionIdArray)ids
          ) src
    ON (tar.hyper_id = src.hyper_id and tar.region_id = src.region_id)
  WHEN NOT MATCHED THEN
  INSERT (hyper_id, region_id) VALUES (src.hyper_id, src.region_id);
END ADD_CATEGORY;

BEGIN
ADD_CATEGORY(91259, 1, 100, shops_web.t_number_tbl(213));
ADD_CATEGORY(90586, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(10498025, 2, 300, shops_web.t_number_tbl(213));
ADD_CATEGORY(4954975, 2, 500, shops_web.t_number_tbl(213));
END;
///
--changeset nastik:MBI-16584_grant_select endDelimiter:;
 grant select on MARKET_BILLING.CPA_CATEGORIES to shops_web;
 grant select on MARKET_BILLING.CPA_CATEGORY_REGIONS to shops_web;

