--liquibase formatted sql
--changeset fbokovikov:MBI-17811-create-table-test-feed-log-and-delta-raw
CREATE TABLE SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW
(
	"GID" NUMBER(20,0) NOT NULL ENABLE,
	"FID" NUMBER(20,0) NOT NULL ENABLE,
	"START_TIME" DATE NOT NULL ENABLE,
	"FINISH_TIME" DATE,
	"YML_TIME" VARCHAR2(40 CHAR),
	"RETURN_CODE" NUMBER(1,0) NOT NULL ENABLE,
	"NOFFERS" NUMBER(*,0),
	"IS_MODIFIED" NUMBER(1,0),
	"DOWNLOAD_TIME" DATE,
	"TOTAL_OFFERS" NUMBER(10,0),
	"INDEXED_STATUS" NUMBER(1,0),
	"DOWNLOAD_RETCODE" NUMBER(4,0),
	"DOWNLOAD_STATUS" VARCHAR2(500 CHAR),
	"PARSE_RETCODE" NUMBER(4,0),
	"PARSE_LOG" CLOB,
	"CACHED_PARSE_LOG" CLOB,
	"CPA_OFFERS" NUMBER DEFAULT 0,
	"CPA_REAL_OFFERS" NUMBER,
	"MATCHED_OFFERS" NUMBER,
	"DISCOUNT_OFFERS_COUNT" NUMBER(12,0),
	"GENERATION_TYPE" NUMBER(1,0) DEFAULT 0,
	"RELEASE_TIME" DATE,
	"PARSE_ERRORS" CLOB,
	"CACHED_PARSE_ERRORS" CLOB,
	"PARSE_ERRORS_STATS" CLOB,
	"CACHED_PARSE_ERRORS_STATS" CLOB,
	"PARSE_LOG_PARSED" CLOB,
	"MATCHED_CLUSTER_OFFER_COUNT" NUMBER(*,0)
)
TABLESPACE FEED_LOG_NG3_TS
LOB (PARSE_LOG, CACHED_PARSE_LOG, PARSE_ERRORS, CACHED_PARSE_ERRORS, PARSE_ERRORS_STATS, CACHED_PARSE_ERRORS_STATS, PARSE_LOG_PARSED) STORE AS (TABLESPACE FEED_LOG_NG2_TS DISABLE STORAGE IN ROW)
PARTITION BY RANGE (RELEASE_TIME) INTERVAL (NUMTODSINTERVAL(1, 'DAY'))
(PARTITION P_TEST_FEED_LOG_AND_D_RAW_MIN VALUES LESS THAN (TO_DATE('01-12-2015','DD-MM-YYYY')));

--changeset fbokovikov:MBI-18685_comment_on_tfladr
COMMENT ON COLUMN shops_web.test_feed_log_and_delta_raw.matched_cluster_offer_count is 'Кол-во сматченных на кластера офферов';

--changeset fbokovikov:MBI-18338-create-index-on-fladr-FID
CREATE INDEX "SHOPS_WEB"."I_TEST_FEED_LOG_RAW_FID" ON "SHOPS_WEB"."TEST_FEED_LOG_AND_DELTA_RAW" ("FID") TABLESPACE FEED_LOG_NG2_TS LOCAL ONLINE;

--changeset fbokovikov:MBI-18338-create-index-on-fladr-PK
CREATE UNIQUE INDEX "SHOPS_WEB"."I_TEST_FEED_LOG_RAW_PK" ON "SHOPS_WEB"."TEST_FEED_LOG_AND_DELTA_RAW" ("GID", "FID", "RELEASE_TIME") TABLESPACE FEED_LOG_NG2_TS LOCAL ONLINE;

--changeset fbokovikov:MBI-18338-create-index-on-fladr-ST_F_G
CREATE UNIQUE INDEX "SHOPS_WEB"."I_TEST_FEED_LOG_RAW_ST_F_G" ON "SHOPS_WEB"."TEST_FEED_LOG_AND_DELTA_RAW" ("START_TIME", "GID", "FID", "RELEASE_TIME") TABLESPACE FEED_LOG_NG2_TS LOCAL ONLINE;

--changeset sviperll:MBI-18748_4
COMMENT ON COLUMN shops_web.test_feed_log_and_delta_raw.parse_errors is 'Не используется';

--changeset sviperll:MBI-18748_5
COMMENT ON COLUMN shops_web.test_feed_log_and_delta_raw.cached_parse_errors is 'Не используется';

--changeset sviperll:MBI-18748_6
COMMENT ON COLUMN shops_web.test_feed_log_and_delta_raw.cached_parse_errors_stats is 'Не используется';

--changeset gaklimov:MBI-20558-totalPromosCount-test
ALTER TABLE SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW ADD TOTAL_PROMOS_COUNT NUMBER;
COMMENT ON COLUMN shops_web.test_feed_log_and_delta_raw.TOTAL_PROMOS_COUNT is 'Общее количество акций';

--changeset gaklimov:MBI-20558-validCpcPromosCount-test
ALTER TABLE SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW ADD VALID_CPC_PROMOS_COUNT NUMBER;
COMMENT ON COLUMN shops_web.test_feed_log_and_delta_raw.VALID_CPC_PROMOS_COUNT is 'Количество валидных CPC-акций';

--changeset gaklimov:MBI-20558-validCpaPromosCount-test
ALTER TABLE SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW ADD VALID_CPA_PROMOS_COUNT NUMBER;
COMMENT ON COLUMN shops_web.test_feed_log_and_delta_raw.VALID_CPA_PROMOS_COUNT is 'Количество валидных CPA-акций';

--changeset gaklimov:MBI-20558-primaryOffersWithPromoCount-test
ALTER TABLE SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW ADD PRIMARY_PROMO_OFFERS_COUNT NUMBER;
COMMENT ON COLUMN shops_web.test_feed_log_and_delta_raw.PRIMARY_PROMO_OFFERS_COUNT is 'Количество главных акционных офферов';

--changeset wadim:MBI-23402
ALTER TABLE SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW ADD PUBLISHED_SESSION VARCHAR2(20);
COMMENT ON COLUMN SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW.PUBLISHED_SESSION is 'Id сессии, в которой фид был опубликован';

--changeset fbokovikov:MBI-23411-cpcOffersCount-test
ALTER TABLE SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW ADD CPC_OFFERS_COUNT NUMBER;
COMMENT ON COLUMN SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW.CPC_OFFERS_COUNT is 'Количество офферов с URL';

--changeset fbokovikov:MBI-23411-cpcShopStatus_test
ALTER TABLE SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW ADD CPC_SHOP_STATUS VARCHAR2(16 CHAR);
COMMENT ON COLUMN SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW.CPC_SHOP_STATUS is 'Статус подключения к программе "Заказ на сайте"';

--changeset fbokovikov:MBI-23411-cpaShopStatus_test
ALTER TABLE SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW ADD CPA_SHOP_STATUS VARCHAR2(16 CHAR);
COMMENT ON COLUMN SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW.CPA_SHOP_STATUS is 'Статус подключения к программе "Заказ на Маркете"';

--changeset fbokovikov:MBI-27474-tfladr-add-red-offers-count
ALTER TABLE SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW ADD RED_OFFERS_COUNT NUMBER;

--changeset fbokovikov:MBI-27474-tfladr-add-red-ereal-offers-count
ALTER TABLE SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW ADD RED_REAL_OFFERS_COUNT NUMBER;

--changeset fbokovikov:MBI-tfladr-27474-comments
COMMENT ON COLUMN SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW.RED_OFFERS_COUNT is 'Количество предложений на Красном Маркете (red_status = SBX/REAL)';
COMMENT ON COLUMN SHOPS_WEB.TEST_FEED_LOG_AND_DELTA_RAW.RED_REAL_OFFERS_COUNT is 'Количество предложений на Красном Маркете в продакшене (red_status = REAL)';

--changeset vbauer:MBI-28096-tfladr-feed_file_type
alter table shops_web.test_feed_log_and_delta_raw add feed_file_type number(1, 0);
comment on column shops_web.test_feed_log_and_delta_raw.feed_file_type is 'Тип расширения файла фида';

--changeset fbokovikov:MBI-28489-tfladr-shop-id
alter table shops_web.test_feed_log_and_delta_raw add shop_id number;
comment on column shops_web.test_feed_log_and_delta_raw.shop_id is 'Идентификатор магазина';

--changeset vbauer:MBI-28128-tfladr-market_template
alter table shops_web.test_feed_log_and_delta_raw add market_template number(1, 0);
comment on column shops_web.test_feed_log_and_delta_raw.market_template is 'Тип XLS шаблона';

--changeset yakun:MBI-29398-tfladr-add-honest_discount_offers_count
alter table shops_web.test_feed_log_and_delta_raw add honest_discount_offers_count number(12,0);
comment on column shops_web.test_feed_log_and_delta_raw.honest_discount_offers_count is 'Количество честных скидочных офферов';

--changeset yakun:MBI-29398-tfladr-add-white_promos_offers_count
alter table shops_web.test_feed_log_and_delta_raw add white_promos_offers_count number(12,0);
comment on column shops_web.test_feed_log_and_delta_raw.white_promos_offers_count is 'Количество неверифицированных акционных офферов';

--changeset yakun:MBI-29398-tfladr-add-honest_white_promos_offers_c
alter table shops_web.test_feed_log_and_delta_raw add honest_white_promos_offers_c number(12,0);
comment on column shops_web.test_feed_log_and_delta_raw.honest_white_promos_offers_c is 'Количество честных акционных офферов';

--changeset nastik:MBI-40906-add-feed-process-type-for-datacamp
alter table shops_web.test_feed_log_and_delta_raw add feed_processing_type varchar2(10 char);
comment on column shops_web.test_feed_log_and_delta_raw.feed_processing_type is 'Тип обработки фида партера, схема работы';

--changeset batalin:MBI-46662
drop table shops_web.test_feed_log_and_delta_raw;
