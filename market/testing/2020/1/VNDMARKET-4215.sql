--liquibase formatted sql

--changeset zaharov-i:VNDMARKET-4215_add_old_bids_to_audit_testing
INSERT INTO VENDORS.MODELBIDS_FORECAST_AUDIT
SELECT
        TO_DATE('19700101','yyyymmdd') + (amr.MODIFIED_DATE/24/60/60) as BID_UPDATE_TIME,
        vds.VENDOR_ID as VENDOR_ID,
        0 as CATEGORY_ID,
        amr.DOMAIN_ID as MODEL_ID,
        amr.BID_VALUE as BID,
        0 as GROUP_ID,
        0 as IS_COMMON,
        null as POSITION_FORECAST,
        0 as SHOWS_RECOMM,
        0 as SHOWS_FACT,
        0 as SHOWS_NEW_RECOMM,
        0 as CLICKS_RECOMM,
        0 as CLICKS_FACT
FROM SHOPS_WEB.AUCTION_MODEL_RULE amr
         JOIN SHOPS_WEB.AUCTION_MODEL_GENERATION_INFO amgi ON amr.GENERATION_ID = amgi.ID
         JOIN VENDORS.VENDOR_DATASOURCE vds ON vds.DATASOURCE_ID = amr.VENDOR_ID
WHERE DOMAIN_TYPE = 'modelId'
  AND amr.BID_VALUE > 0
  AND amgi.GENERATION_NAME = '20190110_1419';
