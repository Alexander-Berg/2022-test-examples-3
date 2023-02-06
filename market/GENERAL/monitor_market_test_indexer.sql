--liquibase formatted sql

--changeset batalin:MBI-56515
CREATE OR REPLACE VIEW
SHOPS_WEB.MONITOR_MARKET_TEST_INDEXER
(
   result,
   description
)
AS
 SELECT CASE
            WHEN cnt > (select count(-1) / 3
                        from SHOPS_WEB.FEED_LOG_HISTORY
                        where META_ID = (select id
                                         from V_LAST_FULL_GENERATIONS
                                         where INDEXER_TYPE = 1
                                           and SITE_TYPE = 0)) THEN 2
            ELSE 0
            END AS RESULT,
        CASE
            WHEN cnt > 0
                THEN cnt
                || ' feeds has been broken in the last ('
                || name
                || ') market premoderation indexation, first feed id = '
                || fid
            ELSE ''
            END AS description
 FROM (
          select min(feed_id) fid, max(name) name, count(-1) cnt
          from V_LAST_FULL_GENERATIONS meta
                   left join FEED_LOG_HISTORY flh on meta.id = flh.META_ID
          where SITE_TYPE = 0
            and flh.INDEXER_TYPE = 1 --planeshift
            and RETURN_CODE > 2)
;
