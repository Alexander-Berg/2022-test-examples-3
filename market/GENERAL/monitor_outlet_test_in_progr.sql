/*
точки не находятся больше 10 дней на модерации
*/
CREATE OR REPLACE VIEW SHOPS_WEB.monitor_outlet_test_in_progr AS
SELECT
  CASE
    WHEN msg IS NULL
    THEN 0
    ELSE 1
  END AS result,
  CASE
    WHEN msg IS NULL
    THEN NULL
    ELSE out_cnt || ' outlets testing is delayed: ' || msg
  END AS description
FROM
(
  select shops_web.stragg(case when rownum <= 3 then
      'shop '
      ||shop_id
      ||' outlet_id '
      ||outlet_id
      ||' ('||TRUNC(sysdate-time,1)||' days)'
      end)
      || (case when count(1) > 3 then ', ...' end) msg,
      count(-1) out_cnt
  FROM
  (
    select shop_id, outlet_id, om.update_time time
    from shops_web.V_ABO_OUTLET_MODIFICATION om
    join shops_web.outlet_info oi on oi.id = om.outlet_id and oi.status = 1
    where oi.status = 1 and om.update_time < sysdate - 10
    order by om.update_time
  )
  /* Happy-New-Year-Hack! */
  WHERE sysdate - trunc(sysdate, 'yyyy') >= 15
)
/*
insert into monitor.monitor_query
(sql_query,  service, period_min, query_descr)
values ('select result, description from SHOPS_WEB.monitor_outlet_test_in_progr',  'mbi-data',  60, 'Слишком долго проверяются точки')
*/
