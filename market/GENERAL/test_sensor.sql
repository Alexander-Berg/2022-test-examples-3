SELECT '/aggregates/test_sensor' as sensor,
       round(extract(epoch from date_trunc('hour', now()))) as ts,
       case c.warehouse_id
              when 1   then 'Маршрут'
              when 145 then 'Маршрут'
              when 147 then 'Ростов'
              when 171 then 'Томилино'
              when 172 then 'Софьино'
              when 47804 then 'Софьино'
              when 47828 then 'Софьино'
              when 47934 then 'Софьино'
              else 'Dropship_Click&Collect'
       end as warehouse,
       case c.catteam
              when 'Дом и сад' then 'Дом и сад'
              when 'Авто' then 'Дом и сад'
              when 'DIY & Auto' then 'Дом и сад'
              when 'Товары для дома' then 'Дом и сад'
              when 'Фарма' then 'Фарма'
              when 'Fashion' then 'Фарма'
              else c.catteam
        end as category_team,
       c.status as status,
       sum (c.value) as value
FROM (SELECT
           i.warehouse_id as warehouse_id,
           d.category_team as catteam,
           $status_name(o.status) as status,
           sum (i.count * i.buyer_price) as value
      FROM orders o join order_item i on i.order_id = o.id
      JOIN $/yt/util/category_tree d
        ON i.category_id = d.hid::bigint
      WHERE o.rgb = 1 and not o.fake and o.user_id != $stress_test_uid AND NOT (o.user_id BETWEEN 2190550858753437195 AND 2190550859753437194)
        and o.context = 0 and o.created_at >= current_date
	    and o.created_at < date_trunc('hour', now())
      GROUP BY warehouse_id, catteam, status
) c
GROUP BY warehouse, category_team, status
ORDER BY warehouse, category_team, status
