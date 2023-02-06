SELECT order_id
    , MIN(from_dt) event_date
    , shop_id
FROM order_history h
WHERE substatus = 31
    AND from_dt > current_date
    AND shop_id = ANY('{%s}'::int[])
GROUP BY order_id
    , shop_id
LIMIT 50
