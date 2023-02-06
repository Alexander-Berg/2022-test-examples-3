PRAGMA yt.InferSchema = '1';
PRAGMA yson.DisableStrict;
PRAGMA AnsiInForEmptyOrNullableItemsCollections;
PRAGMA yt.Pool = 'default';

$stocks = (
    select
        st.msku as msku,
        sum_if(st.fit, s.supplier_type = 1) as count_1p,
        sum(st.fit) as count_overall
    from `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/stock_alpaca` as st
        join `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/suppliers` as s
            on st.supplier_id = s.supplier_id
    where st.msku > 0
    group by st.msku
);

$forecasts = (
    SELECT
        msku,
        SUM_IF(f.demand, f.`date` BETWEEN '2021-09-17' AND '2021-09-30') AS sf14d_1p,
        SUM_IF(f.demand, f.`date` BETWEEN '2021-09-17' AND '2021-10-14') AS sf28d_1p,
        SUM_IF(f.demand, f.`date` BETWEEN '2021-09-17' AND '2021-11-11') AS sf56d_1p,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2021-09-17' AND '2021-09-30') AS sf14d,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2021-09-17' AND '2021-10-14') AS sf28d,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2021-09-17' AND '2021-11-11') AS sf56d,
    FROM `//home/market/production/replenishment/order_planning/2021-09-16/intermediate/forecast_region` AS f
    GROUP BY msku
);

select
    r.msku as msku,
    some(coalesce(st.count_1p, 0)) as stock_1p,
    some(coalesce(st.count_overall, 0)) as stock_overall,
    some(ff.sf14d_1p) as sf14d_1p,
    some(ff.sf28d_1p) as sf28d_1p,
    some(ff.sf56d_1p) as sf56d_1p,
    some(ff.sf14d) as sf14d,
    some(ff.sf28d) as sf28d,
    some(ff.sf56d) as sf56d
from `//home/market/production/replenishment/order_planning/2021-09-16/outputs/recommendations` as r
    left join $stocks as st
        on r.msku = st.msku
    left join $forecasts as ff
        on r.msku = ff.msku
group by r.msku;
