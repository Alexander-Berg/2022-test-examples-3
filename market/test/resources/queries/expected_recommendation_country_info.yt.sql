PRAGMA yt.InferSchema = '1';
PRAGMA yson.DisableStrict;
PRAGMA AnsiInForEmptyOrNullableItemsCollections;

$stocks = (
    select
        st.msku as msku,
        sum_if(st.fit, s.supplier_type = 1) as count_1p,
        sum(st.fit) as count_overall
    from `//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_alpaca` as st
        join `//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers` as s
            on st.supplier_id = s.supplier_id
    where st.msku > 0
    group by st.msku
);

$forecasts = (
    SELECT
        msku,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2020-05-15' AND '2020-05-28') AS sf14d,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2020-05-15' AND '2020-06-11') AS sf28d,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2020-05-15' AND '2020-07-09') AS sf56d,
    FROM `//home/market/production/replenishment/order_planning/2020-05-15/intermediate/forecast_region` AS f
    GROUP BY msku
);

$assortment = (
    SELECT
        model_id    AS msku
        FROM `//home/market/production/mbo/export/recent/models/sku`
        WHERE model_id > 0 AND title != '' AND category_id > 0
);

select
    a.msku as msku,
    coalesce(st.count_1p, 0) as stock_1p,
    coalesce(st.count_overall, 0) as stock_overall,
    ff.sf14d as sf14d,
    ff.sf28d as sf28d,
    ff.sf56d as sf56d
from $assortment as a
    left join $stocks as st
        on a.msku = st.msku
    left join $forecasts as ff
        on a.msku = ff.msku
where
    st.msku is not null or
    ff.msku is not null
