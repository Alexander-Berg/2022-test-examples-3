USE hahn;

SELECT
    '/plans/top-items-per-wh' as sensor,
    DateTime::ToSeconds(AddTimezone(CurrentUtcDatetime(), "Europe/Moscow"))
    - DateTime::GetMinute(AddTimezone(CurrentUtcDatetime(), "Europe/Moscow")) * 60
    - DateTime::GetSecond(AddTimezone(CurrentUtcDatetime(), "Europe/Moscow")) as ts,
    CASE
        WHEN warehouse_id = 145 THEN 'Маршрут'
        WHEN warehouse_id = 147 THEN 'Ростов'
        WHEN warehouse_id = 171 THEN 'Томилино'
        WHEN warehouse_id = 172 THEN 'Софьино'
        WHEN warehouse_id = 999 THEN 'Софьино_CrossDock'
        ELSE 'Dropship_Click&Collect'
    END AS warehouse,
    SUM(items_count_top) as value
FROM `//home/market/production/analytics/business/forecast_sales_monitor/warehouses_by_hours` AS plans
WHERE `date` = DateTime::Format("%Y-%m-%d")(AddTimezone(CurrentUtcDatetime(), "Europe/Moscow"))
    AND hour < DateTime::GetHour(AddTimezone(CurrentUtcDatetime(), "Europe/Moscow"))
GROUP BY warehouse_id

