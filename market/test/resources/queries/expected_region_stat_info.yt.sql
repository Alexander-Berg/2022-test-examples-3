PRAGMA yt.InferSchema = '1';
PRAGMA AnsiInForEmptyOrNullableItemsCollections;

$is_moscow = ($region_id) -> {
   return $region_id = 145;
};

$warehouses_with_regions = (
    select
        wh.id as id,
        wh.region_id as region_id
    from `//home/market/production/replenishment/order_planning/2020-08-11/intermediate/warehouses` as wh
);

$is_1P = ($supplier_id) -> { return $supplier_id == 465852};

$fullfilment_partners = (
    SELECT id
        FROM `//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest`
        WHERE is_fullfilment = 1
);

$msku_with_fullfilment_supplier = (
    SELECT approved_market_sku_id as msku
    FROM `//home/market/production/ir/ultra-controller/supplier_to_market_sku`
    WHERE shop_id in $fullfilment_partners
);

$assortment_with_new_msku = (
    SELECT
        msku
    FROM `//home/market/production/mbo/export/recent/models/sku` AS sku
        LEFT JOIN `//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest` AS sku_transitions
            ON sku.model_id = sku_transitions.old_entity_id
    WHERE sku.model_id > 0 AND sku.title != '' AND sku.category_id > 0
    AND COALESCE(sku_transitions.new_entity_id, sku.model_id) IN $msku_with_fullfilment_supplier
    GROUP BY COALESCE(sku_transitions.new_entity_id, sku.model_id) as msku
);

$assortment = (
    SELECT
        mskus.msku AS msku,
        region.region_id AS region
    FROM $assortment_with_new_msku AS mskus
        CROSS JOIN `//home/market/production/replenishment/order_planning/2020-08-11/intermediate/regions` AS region
);

$forecast = (SELECT
        msku,
        f.region AS region,
        sum(f.demand) AS forecast_1p,
        sum(f.total_demand) AS forecast
    FROM `//home/market/production/replenishment/order_planning/2020-08-04/intermediate/forecast_region` AS f
        LEFT JOIN `//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest` AS sku_transitions
            ON f.msku = sku_transitions.old_entity_id
    WHERE f.`date` = '2020-08-11'
    GROUP BY COALESCE(sku_transitions.new_entity_id, f.msku) as msku, f.region
);

$oos = (SELECT
        msku,
        region,
        sum(s.missed_orders) as missed_orders
    FROM `//home/market/production/replenishment/order_planning/2020-08-11/intermediate/suppliers_demand` as s
        INNER JOIN `//home/market/production/replenishment/order_planning/2020-08-11/intermediate/warehouses` as w
            ON w.id = s.warehouse_id
    WHERE s.supplier_id > 0 AND s.supplier_id IN $fullfilment_partners AND CAST(s.`date` as Date) = CAST('2020-08-11' as Date)
    GROUP BY s.msku as msku, w.region_id as region
    HAVING NOT bool_or(s.on_stock)
);

$stock = (SELECT
        msku as msku,
        region,
        sum_if(greatest(available_amount, 0), $is_1P(supplier_id)) as stock_1P,
        sum_if(greatest(available_amount, 0), not $is_1P(supplier_id) ) as stock_3P
    FROM `//home/market/production/mstat/dictionaries/stock_sku/1d/2020-08-11` as stock
        LEFT JOIN `//home/market/production/ir/ultra-controller/supplier_to_market_sku` as mapping
            ON stock.shop_sku = mapping.shop_sku_id AND stock.supplier_id = mapping.shop_id
        LEFT JOIN $warehouses_with_regions as wwr
            ON stock.warehouse_id = wwr.id
        LEFT JOIN `//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest` AS sku_transitions
            ON mapping.approved_market_sku_id = sku_transitions.old_entity_id
    WHERE $is_moscow(wwr.region_id)
        AND mapping.approved_market_sku_id > 0
    GROUP BY COALESCE(sku_transitions.new_entity_id, mapping.approved_market_sku_id) as msku, wwr.region_id as region
);

SELECT
    a.msku AS msku,
    a.region AS region,
    f.forecast_1p as forecast_1p,
    f.forecast as forecast,
    oos.missed_orders as missed_orders,
    s.stock_1P as stock_1P,
    s.stock_3P as stock_3P
FROM $assortment as a
    LEFT JOIN $forecast as f ON a.msku = f.msku AND a.region = f.region
    LEFT JOIN $oos as oos ON a.msku = oos.msku AND a.region = oos.region
    LEFT JOIN $stock as s ON a.msku = s.msku AND a.region = s.region
WHERE (f.msku IS NOT NULL
   OR oos.msku IS NOT NULL
   OR s.msku IS NOT NULL);
