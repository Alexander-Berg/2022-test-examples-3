PRAGMA yt.InferSchema = '1';
PRAGMA yson.DisableStrict;
PRAGMA AnsiInForEmptyOrNullableItemsCollections;
PRAGMA yt.Pool = 'default';

-- склады
$warehouses_with_regions = (
    select
        wh.id as id,
        wh.region_id as region_id
    from `//home/market/production/replenishment/order_planning/2020-08-04/intermediate/warehouses` as wh
);

--сейфти сток
$safety_stocks_by_region = (
    SELECT
        s.msku                              AS msku,
        s.region                            AS region,
        s.safety_stock                      AS safety_stock,
        s.`date`                            AS `date`
    FROM `//home/market/production/replenishment/order_planning/2020-08-04/intermediate/ss_region_reduced` AS s
);

--safety stock по стране
$safety_stocks_country_wide = (
    SELECT
        s.msku                              AS msku,
        SUM(s.safety_stock)                 AS safety_stock,
        s.`date`                            AS `date`
    FROM `//home/market/production/replenishment/order_planning/2020-08-04/intermediate/ss_region_reduced` AS s
    GROUP BY s.msku, s.`date`
);

--ручной сток
$manual_stocks = (
    select
       msku, region, max_items, max_items_by_days, min_items, min_items_by_days, `date`
    from `//home/market/production/replenishment/order_planning/2020-08-04/intermediate/manual_stock_model`
);

select
    r.lead_time as delivery_time,
    r.`date` as delivery_date,
    r.order_date as order_date,
    r.xdoc_date as xdoc_date,
    r.warehouse_id as warehouse_id,
    WeakField(r.warehouse_id_from, Int64) as warehouse_id_from,
    r.supplier_id as supplier_id,
    r.qty as purch_qty,
    WeakField(r.distr_demand, Int64, 0) as distr_demand,
    r.msku as msku,
    r.min_shipment as min_shipment,
    r.shipment_quantum as shipment_quantum,
    r.delivery_type as supply_route,
    r.ssku as ssku,
    WeakField(r.has_cheaper, Bool, false) as has_cheaper_recommendation,
    case when WeakField(r.distr_demand, Int64, 0) > 0 then
        Math::Round(ss_country.safety_stock, -1)
    else
        Math::Round(ss_region_reduced.safety_stock, -1)
    end as safety_stock,
    ms.max_items as max_items,
    ms.max_items_by_days as max_items_by_days,
    ms.min_items as min_items,
    ms.min_items_by_days as min_items_by_days,
    r.post_opt_comment as reason_of_recommendation,
    r.inactive as inactive,
    r.inactive_reason as inactive_reason
from `//home/market/production/replenishment/order_planning/2020-08-04/outputs/recommendations` as r
    left join $warehouses_with_regions as wwr
on r.warehouse_id = wwr.id
    left join $safety_stocks_by_region as ss_region_reduced
on r.msku = ss_region_reduced.msku
        and wwr.region_id = ss_region_reduced.region
        and r.`date` = ss_region_reduced.`date`
    left join $safety_stocks_country_wide as ss_country
on r.msku = ss_country.msku and r.`date` = ss_country.`date`
    left join $manual_stocks as ms
on r.msku = ms.msku and wwr.region_id = ms.region and r.`date` = ms.`date`
where
    delivery_type != 'inter-warehouse movement'
