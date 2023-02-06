--liquibase formatted sql

--changeset snoop:MBI-21691-set-import-interval-test-only
INSERT INTO barc_tms.import_interval(id,time,host,entity_name,from_date)
  SELECT barc_tms.import_interval_seq.nextval,SYSDATE,'yandex-mbi-db-MBI-21691','AUCTION_RESULT',
  NVL(t.time,(SELECT MIN(publish_date) FROM shops_web.auction_generation_info gi WHERE EXISTS (
  SELECT 1 FROM shops_web.auction_rule ar WHERE ar.generation_id = gi.id)))
FROM (
  SELECT MAX(TO_DATE(value,'yymmddHH24'))+interval '1' hour as time FROM shops_web.environment
  WHERE NAME = 'migrateAuctionRuleLastHourDone') t