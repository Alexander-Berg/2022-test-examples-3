PRAGMA yt.InferSchema = '1';
PRAGMA yson.DisableStrict;
PRAGMA AnsiInForEmptyOrNullableItemsCollections;
PRAGMA yt.Pool = 'default';

$fullfilment_partners = (
    SELECT id
        FROM `//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest`
    WHERE is_fullfilment = 1
);

$warehouses_with_regions = (
    select
        wh.id as id,
        wh.region_id as region_id
    from `//home/market/production/replenishment/order_planning/2021-09-16/intermediate/warehouses` as wh
);

$forecasts = (
    SELECT
        msku,
        region,
        SUM_IF(f.demand, f.`date` BETWEEN '2021-09-17' AND '2021-09-30') AS sf14d_1p,
        SUM_IF(f.demand, f.`date` BETWEEN '2021-09-17' AND '2021-10-14') AS sf28d_1p,
        SUM_IF(f.demand, f.`date` BETWEEN '2021-09-17' AND '2021-11-11') AS sf56d_1p,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2021-09-17' AND '2021-09-30') AS sf14d,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2021-09-17' AND '2021-10-14') AS sf28d,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2021-09-17' AND '2021-11-11') AS sf56d,
    FROM `//home/market/production/replenishment/order_planning/2021-09-16/intermediate/forecast_region` AS f
    LEFT JOIN `//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest` AS sku_transitions
        ON f.msku = sku_transitions.old_entity_id
    GROUP BY COALESCE(sku_transitions.new_entity_id, f.msku) as msku, f.region AS region
);

$oos_by_region_and_dates = (
    select
        msku,
        region,
        CAST(s.`date` as Date) as `date`,
        sum(s.missed_orders) as missed_orders,
        sum_if(s.missed_orders, sup.supplier_type = 1) as missed_orders_1p
    from `//home/market/production/replenishment/order_planning/2021-09-16/intermediate/suppliers_demand` as s
        inner join `//home/market/production/replenishment/order_planning/2021-09-16/intermediate/warehouses` as w
        on w.id = s.warehouse_id
        left join any `//home/market/production/replenishment/order_planning/2021-09-16/intermediate/suppliers` as sup
        on sup.supplier_id = s.supplier_id
    where s.supplier_id > 0 AND s.supplier_id IN $fullfilment_partners
    group by s.msku as msku, s.`date`, w.region_id as region
    having not bool_or(s.on_stock)
);

$oos_by_region = (
    SELECT
        msku,
        region,
        sum_if(cast(missed_orders as double), `date` between Date('2021-08-19') and Date('2021-09-16')) as missed_orders_28d,
        sum_if(cast(missed_orders_1p as double), `date` between Date('2021-08-19') and Date('2021-09-16')) as missed_orders_28d_1p,
        sum(cast(missed_orders as double)) as missed_orders_56d,
        sum(cast(missed_orders_1p as double)) as missed_orders_56d_1p,
        count_if(`date` between CAST('2021-08-19' as Date) and CAST('2021-09-16' as Date)) as oos_28_days,
        count(1) as oos_days
    FROM $oos_by_region_and_dates
    GROUP BY msku, region
);

$oos = (
    select
        coalesce(sku_transitions.new_entity_id, oos.msku) as msku,
        region,
        oos_28_days,
        oos_days,
        missed_orders_28d,
        missed_orders_56d,
        missed_orders_28d_1p,
        missed_orders_56d_1p
    FROM $oos_by_region as oos
        left join `//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest` as sku_transitions
        on oos.msku = sku_transitions.old_entity_id
);

$actual_fit = (
     SELECT
         msku,
         region_id,
         fit
     FROM `//home/market/test/replenishment/autoorder/import_preparation/actual_fit/2021-09-17`
);

$transit = (
    select
        msku,
        region,
        sum_if(in_transit, s.supplier_type = 1) as transit1p,
        sum(in_transit) as transit
    from
        `//home/market/production/replenishment/order_planning/2021-09-17/outputs/transits` as t
    join `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/suppliers` as s
            on t.supplier_id = s.supplier_id
    left join $warehouses_with_regions AS wwr ON t.warehouse_id = wwr.id
    left join `//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest` as sku_transitions
            on t.msku = sku_transitions.old_entity_id

    group by coalesce(sku_transitions.new_entity_id, t.msku) as msku, wwr.region_id as region
);

$stocks = (
    select
        msku,
        region,
        sum_if(st.fit, s.supplier_type = 1) as count_1p,
        sum(st.fit) as count_overall
    from `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/stock_alpaca` as st
        join `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/suppliers` as s
            on st.supplier_id = s.supplier_id
        left join $warehouses_with_regions AS wwr
            on st.warehouse_id = wwr.id
        left join `//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest` as sku_transitions
            on st.msku = sku_transitions.old_entity_id
    where st.msku > 0
    group by coalesce(sku_transitions.new_entity_id, st.msku) as msku, wwr.region_id as region
);

$recommendations = (
    select
        coalesce(sku_transitions.new_entity_id, rec.msku) as msku,
        rec.warehouse_id as warehouse_id
    from `//home/market/production/replenishment/order_planning/2021-09-16/outputs/recommendations` as rec
    left join `//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest` as sku_transitions
            on rec.msku = sku_transitions.old_entity_id
);

select
    r.msku as msku,
    wwr.region_id as region,
    some(ff.sf14d_1p) as sf14d_1p,
    some(ff.sf28d_1p) as sf28d_1p,
    some(ff.sf56d_1p) as sf56d_1p,
    some(ff.sf14d) as sf14d,
    some(ff.sf28d) as sf28d,
    some(ff.sf56d) as sf56d,
    some(coalesce(oos.oos_28_days, 0)) as oos_28_days,
    some(coalesce(oos.oos_days, 0)) as oos_days,
    some(coalesce(oos.missed_orders_28d, 0)) as missed_orders_28d,
    some(coalesce(oos.missed_orders_56d, 0)) as missed_orders,
    some(coalesce(oos.missed_orders_28d_1p, 0)) as missed_orders_28d_1p,
    some(coalesce(oos.missed_orders_56d_1p, 0)) as missed_orders_1p,
    some(coalesce(act_fit.fit, 0)) as actual_fit,
    some(t.transit1p) as transit1p,
    some(t.transit) as transit,
    some(coalesce(st.count_1p, 0)) as stock_1p,
    some(coalesce(st.count_overall, 0)) as stock_overall
from $recommendations as r
    left join $warehouses_with_regions AS wwr
        on r.warehouse_id = wwr.id
    left join $forecasts as ff
        on r.msku = ff.msku and wwr.region_id = ff.region
    left join $oos as oos
        on r.msku = oos.msku and wwr.region_id = oos.region
    left join $actual_fit as act_fit
        on r.msku = act_fit.msku and wwr.region_id = act_fit.region_id
    left join $transit as t
        on r.msku = t.msku and wwr.region_id = t.region
    left join $stocks as st
        on r.msku = st.msku and wwr.region_id = st.region
where
    ff.msku is not null or
    oos.msku is not null or
    act_fit.msku is not null or
    t.msku is not null or
    st.msku is not null
group by r.msku, wwr.region_id;
