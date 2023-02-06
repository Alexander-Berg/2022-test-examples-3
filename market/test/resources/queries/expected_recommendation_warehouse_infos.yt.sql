PRAGMA yt.InferSchema = '1';
PRAGMA yson.DisableStrict;
PRAGMA AnsiInForEmptyOrNullableItemsCollections;
PRAGMA yt.Pool = 'default';

$is_moscow = ($warehouse_id) -> {
   return $warehouse_id in (145, 171, 172);
};

$near_stock = ($w_id, $stock, $m_stock) -> {
   return
      case when $is_moscow($w_id) then
        coalesce($m_stock, 0) - coalesce($stock, 0)
      else
        null end;
};

$transit = (
    select
        t.msku as msku,
        t.warehouse_id as warehouse_id,
        sum_if(t.in_transit, s.supplier_type = 1) as transit1p,
        sum(t.in_transit) as transit
    from
        `//home/market/production/replenishment/order_planning/2021-09-17/outputs/transits` as t
        join `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/suppliers` as s
            on t.supplier_id = s.supplier_id
    group by t.msku, t.warehouse_id);

-- стоки
$stocks = (
    select
        st.msku as msku,
        st.warehouse_id as warehouse_id,
        sum_if(st.fit, s.supplier_type = 1) as count_1p,
        sum(st.fit) as count_overall
    from `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/stock_alpaca` as st
        join `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/suppliers` as s
            on st.supplier_id = s.supplier_id
    where st.msku > 0
    group by st.msku, st.warehouse_id
);

-- стоки московских складов
$moscow_stocks = (
    select
        st.msku as msku,
        sum_if(st.fit, s.supplier_type = 1) as count_1p,
        sum(st.fit) as count_overall
    from `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/stock_alpaca` as st
        join `//home/market/production/replenishment/order_planning/2021-09-17/intermediate/suppliers` as s
            on st.supplier_id = s.supplier_id
    where st.msku > 0
        and $is_moscow(st.warehouse_id)
    group by st.msku
);

select
    r.warehouse_id as warehouse_id,
    r.msku as msku,
    some(t.transit1p) as transit1p,
    some(t.transit) as transit,
    some(coalesce(st.count_1p, 0)) as stock_1p,
    some(coalesce(st.count_overall, 0)) as stock_overall,
    some($near_stock(r.warehouse_id, st.count_1p, m_st.count_1p)) as near_stock_1p,
    some($near_stock(r.warehouse_id, st.count_overall, m_st.count_overall)) as near_stock_overall
from `//home/market/production/replenishment/order_planning/2021-09-16/outputs/recommendations` as r
    left join $transit as t
        on r.msku = t.msku and r.warehouse_id = t.warehouse_id
    left join $stocks as st
        on r.msku = st.msku and r.warehouse_id = st.warehouse_id
    left join $moscow_stocks as m_st
        on r.msku = m_st.msku
group by r.msku, r.warehouse_id;
