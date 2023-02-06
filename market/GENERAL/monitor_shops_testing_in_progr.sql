--liquibase formatted sql
--changeset fbokovikov:MBI-22663 endDelimiter:/
/*
магазины не находятся больше 8 дней на модерации
*/
CREATE OR REPLACE VIEW SHOPS_WEB.MONITOR_SHOPS_TESTING_IN_PROGR AS
SELECT
  CASE
    WHEN msg IS NULL
    THEN 0
    ELSE 1
  END AS result,
  CASE
    WHEN msg IS NULL
    THEN NULL
    ELSE shops_count || ' shops testing is delayed: ' || msg
  END AS description
FROM
(
  SELECT shops_web.stragg(case when rownum <= 3 then
    'shop '
    ||datasource_id
    ||' ('||TRUNC(sysdate-TIME,1)||' days)'
    end)
    || (case when count(1) > 3 then ', ...' end) msg,
    count(-1) shops_count
  FROM
  (
    select datasource_id, nvl(updated_at,start_date) time
    from shops_web.datasources_in_testing dst
    where in_progress = 1
      and nvl(updated_at,start_date) < sysdate - interval '8' day
    order by nvl(updated_at,start_date) desc
  )
  /* Happy-New-Year-Hack! */
  WHERE sysdate - trunc(sysdate, 'yyyy') >= 15
)
/
