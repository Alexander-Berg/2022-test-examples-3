SELECT t.id ticket_id
    , shop_id
    , business_id
    , creation_time
FROM recheck_ticket t
    JOIN ext_organization_info i ON t.shop_id = i.datasource_id
WHERE type_id = 28
    AND creation_time > current_date
    AND business_id = ANY('{%s}'::int[])
