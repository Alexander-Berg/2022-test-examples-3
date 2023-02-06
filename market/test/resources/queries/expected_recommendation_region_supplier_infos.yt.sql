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

$stocks = (
    select
        msku,
        region,
        sum_if(st.fit, s.supplier_type = 1) as count_1p,
        sum(st.fit) as count_overall,
        supplier
    from `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/stock_alpaca` as st
        join `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/suppliers` as s
            on st.supplier_id = s.supplier_id
    LEFT JOIN $warehouses_with_regions AS wwr ON st.warehouse_id = wwr.id
    where st.msku > 0
    group by st.msku as msku, wwr.region_id as region, s.supplier_id as supplier
);

$transits = (
    select
        msku,
        region,
        sum_if(tr.in_transit, s.supplier_type = 1) as transit1p,
        sum(tr.in_transit) as transit,
        supplier
    from `//home/market/production/replenishment/order_planning/2021-09-17/outputs/transits` as tr
        join `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/suppliers` as s
            on tr.supplier_id = s.supplier_id
    LEFT JOIN $warehouses_with_regions AS wwr ON tr.warehouse_id = wwr.id
    where tr.msku > 0
    group by tr.msku as msku, wwr.region_id as region, s.supplier_id as supplier
);

$oos_by_dates = (
    select
        msku,
        region,
        supplier_id,
        CAST(s.`date` as Date) as `date`
    from `//home/market/production/replenishment/order_planning/2021-09-16/intermediate/suppliers_demand` as s
        inner join $warehouses_with_regions AS wwr ON s.warehouse_id = wwr.id
    where s.supplier_id > 0 and s.supplier_id IN $fullfilment_partners
    group by s.msku as msku, s.supplier_id as supplier_id, s.`date`, wwr.region_id as region
    having not bool_or(s.on_stock)
);

$oos = (
    select
        oos.msku as msku,
        oos.region as region,
        oos.supplier_id as supplier_id,
        count_if(oos.`date` between CAST('2021-09-09' as Date) and CAST('2021-09-16' as Date)) as oos_7_days,
        count_if(oos.`date` between CAST('2021-08-19' as Date) and CAST('2021-09-16' as Date)) as oos_28_days,
        count(1) as oos_56_days
    from $oos_by_dates as oos
    group by oos.msku, oos.region, oos.supplier_id
);

select
    r.msku as msku,
    wwr.region_id as region,
    some(coalesce(st.count_1p, 0)) as stock_1p,
    some(coalesce(st.count_overall, 0)) as stock_overall,
    r.supplier_id as supplier_id,
    some(coalesce(os.oos_7_days, 0)) AS oos_7_days,
    some(coalesce(os.oos_28_days, 0)) AS oos_28_days,
    some(coalesce(os.oos_56_days, 0)) AS oos_56_days,
    some(tr.transit1p) as transit1p,
    some(tr.transit) as transit
from `//home/market/production/replenishment/order_planning/2021-09-16/outputs/recommendations` as r
    left join $warehouses_with_regions AS wwr
        on r.warehouse_id = wwr.id
    left join $stocks as st
        on r.msku = st.msku and wwr.region_id = st.region and st.supplier = r.supplier_id
    LEFT JOIN $oos as os
        ON r.msku = os.msku AND wwr.region_id = os.region AND r.supplier_id = os.supplier_id
    left join $transits as tr
        on r.msku = tr.msku and wwr.region_id = tr.region and r.supplier_id = tr.supplier
where
    st.msku is not null OR tr.msku is not null
group by r.msku, wwr.region_id, r.supplier_id;
