--liquibase formatted sql

--changeset nastik:MBI-17258 endDelimiter:endDelimiter:///
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
ADD_CATEGORY(90401, 0, 200, shops_web.t_number_tbl(225));
ADD_CATEGORY(90586, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91259, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(4954975, 2, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(10498025, 2, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91708, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(7070735, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(5017483, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91517, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(226665, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91525, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91524, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(242699, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91579, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91540, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(987827, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(91577, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(277646, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(278345, 1, 200, shops_web.t_number_tbl(213));
ADD_CATEGORY(431294, 1, 200, shops_web.t_number_tbl(213));
END;
///
