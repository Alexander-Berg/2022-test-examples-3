PRAGMA yt.InferSchema = '1000';
PRAGMA AnsiInForEmptyOrNullableItemsCollections;

$warehouses_with_regions = (
    select
        wh.id as id,
        wh.region_id as region_id
    from `//home/market/production/replenishment/order_planning/2020-11-23/intermediate/warehouses` as wh
);

$transit = (
    SELECT
        msku,
        warehouse_id,
        SUM(in_transit) AS transit
    FROM `//home/market/production/replenishment/order_planning/2020-11-23/outputs/transits`
    GROUP BY msku, warehouse_id
);

$forecasts = (
    SELECT
        msku,
        region,
        SUM_IF(f.demand, f.`date` BETWEEN '2020-11-23' AND '2020-12-06') AS sf14d_1p,
        SUM_IF(f.demand, f.`date` BETWEEN '2020-11-23' AND '2020-12-20') AS sf28d_1p,
        SUM_IF(f.demand, f.`date` BETWEEN '2020-11-23' AND '2021-01-17') AS sf56d_1p,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2020-11-23' AND '2020-12-06') AS sf14d,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2020-11-23' AND '2020-12-20') AS sf28d,
        SUM_IF(f.total_demand, f.`date` BETWEEN '2020-11-23' AND '2021-01-17') AS sf56d,
    FROM `//home/market/production/replenishment/order_planning/2020-11-23/intermediate/forecast_region` AS f
    GROUP BY msku, f.region AS region
);

-- стоки
$stocks = (
    SELECT
        m.approved_market_sku_id                    AS msku,
        s.warehouse_id                              AS warehouse_id,
        SUM(IF(fit < freezed, 0, fit - freezed))    AS count
    FROM `//home/market/production/mstat/dictionaries/stock_sku/1d/latest` AS s
        LEFT JOIN `//home/market/production/ir/ultra-controller/supplier_to_market_sku` AS m
    ON m.shop_sku_id = s.shop_sku AND m.shop_id = s.supplier_id
    WHERE m.approved_market_sku_id > 0
    GROUP BY m.approved_market_sku_id, s.warehouse_id
);

-- каттимы
$catteams = (
    SELECT DISTINCT
        g.market_sku    AS msku,
        c.catteam       AS catteam
    FROM `//home/market/production/analytics/business/fulfillment/goods_to_sale/prod/latest` AS g
        LEFT JOIN `//home/market/production/mstat/analyst/const/hid_to_category_director/latest` AS c
    ON c.hid = g.hid
    WHERE g.hid > 0
);

--сейфти сток
$safety_stocks = (
    SELECT
        s.msku                              AS msku,
        s.region                            AS region,
        s.safety_stock                      AS safety_stock,
        s.`date`                            AS `date`
    FROM `//home/market/production/replenishment/order_planning/2020-11-23/intermediate/ss_region` AS s
);

SELECT
    r.msku                                  AS msku,
    r.ssku                                  AS ssku,
    r.supplier_id                           AS supplier_id,
    r.supplier_type                         AS supplier_type,
    r.warehouse_id                          AS warehouse_id,
    WeakField(r.warehouse_id_from, Int64)   AS warehouse_from,
    c.catteam                               AS catteam,
    r.`date`                                AS delivery_date,
    r.lead_time                             AS delivery_time,
    r.qty                                   AS purch_qty,
    t_from.transit                          AS transit_from,
    t_to.transit                            AS transit_to,
    ff.sf14d_1p                             AS sf14d_1p,
    ff.sf28d_1p                             AS sf28d_1p,
    ff.sf56d_1p                             AS sf56d_1p,
    ff.sf14d                                AS sf14d,
    ff.sf28d                                AS sf28d,
    ff.sf56d                                AS sf56d,
    s_from.count                            AS stock_from,
    s_to.count                              AS stock_to,
    Math::Round(ss_from.safety_stock, -1)   AS safety_stock_from,
    Math::Round(ss_to.safety_stock, -1)     AS safety_stock_to
FROM `//home/market/production/replenishment/order_planning/2020-11-23/intermediate/inter_wh_movements` AS r
    LEFT JOIN $transit AS t_from
ON t_from.msku = r.msku AND t_from.warehouse_id = WeakField(r.warehouse_id_from, Int64)
    LEFT JOIN $warehouses_with_regions as wwr
on r.warehouse_id = wwr.id
    LEFT JOIN $warehouses_with_regions as wwr_from
on WeakField(r.warehouse_id_from, Int64) = wwr_from.id
    LEFT JOIN $transit AS t_to
ON t_to.msku = r.msku AND t_to.warehouse_id = r.warehouse_id
    LEFT JOIN $forecasts AS ff
ON ff.msku = r.msku AND ff.region = wwr.region_id
    LEFT JOIN $stocks AS s_from
ON s_from.msku = r.msku AND s_from.warehouse_id = WeakField(r.warehouse_id_from, Int64)
    LEFT JOIN $stocks AS s_to
ON s_to.msku = r.msku AND s_to.warehouse_id = r.warehouse_id
    LEFT JOIN $safety_stocks AS ss_from
ON ss_from.msku = r.msku
    AND ss_from.region = wwr_from.region_id
    AND ss_from.`date` = r.`date`
    LEFT JOIN $safety_stocks AS ss_to
ON ss_to.msku = r.msku AND ss_to.region = wwr.region_id AND ss_to.`date` = r.`date`
    LEFT JOIN $catteams AS c
ON c.msku = r.msku
WHERE delivery_type = 'inter-warehouse movement';
