PRAGMA yt.ExpirationInterval = "3d";
PRAGMA DisableAnsiInForEmptyOrNullableItemsCollections;

$fullfilment_partners = (
    SELECT id
        FROM `//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest`
        WHERE is_fullfilment = 1
);

$oos_by_dates = (
    select
        msku,
        region,
        CAST(s.`date` as Date) as `date`,
        sum(s.missed_orders) as missed_orders
    from `//home/market/production/replenishment/order_planning/latest/intermediate/suppliers_demand` as s
        inner join `//home/market/production/replenishment/order_planning/latest/intermediate/warehouses` as w
        on w.id = s.warehouse_id
    where s.supplier_id > 0 AND s.supplier_id IN $fullfilment_partners
    group by s.msku as msku, s.`date`, w.region_id as region
    having not bool_or(s.on_stock)
);

INSERT INTO `//home/market/testing123/replenishment/autoorder/import_preparation/forecast_oos/2021-09-16`
WITH TRUNCATE
SELECT
    msku,
    region,
    sum_if(cast(missed_orders as double), `date` between '2021-08-18' and '2021-09-15') as missed_orders_28d,
    sum(cast(missed_orders as double)) as missed_orders_56d,
    count(1) as oos_days
FROM $oos_by_dates
GROUP BY msku, region
