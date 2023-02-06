PRAGMA yt.ExpirationInterval = "3d";
PRAGMA DisableAnsiInForEmptyOrNullableItemsCollections;

$fullfilment_partners = (
    SELECT id
    FROM `//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest`
    WHERE is_fullfilment = 1
);

$warehouses_with_regions = (
    select
        wh.id as id,
        wh.region_id as region_id
    from `//home/market/production/replenishment/order_planning/latest/intermediate/warehouses` as wh
);

$actual_fit = (
    SELECT
        msku,
        region_id,
        sum(f.fit) as fit
    FROM RANGE (
`//home/market/production/mstat/dwh/presentation/cube_calc_stock_movement_stock`,
'2021-09-01',
'2021-09-14'
)   as f
    LEFT JOIN $warehouses_with_regions AS wwr ON f.warehouse_id = wwr.id
    LEFT JOIN `//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest` as sku_transitions
        ON f.ssku_msku_msku = sku_transitions.old_entity_id
    WHERE COALESCE(f.supplier_1p, '') != ''
        AND f.`date` BETWEEN '2021-09-01' AND '2021-09-14'
        AND f.supplier_id in $fullfilment_partners
    GROUP BY COALESCE(sku_transitions.new_entity_id, f.ssku_msku_msku) as msku, wwr.region_id as region_id
);

INSERT INTO `//home/market/test/replenishment/autoorder/import_preparation/actual_fit/2021-09-16`
WITH TRUNCATE
     SELECT
         msku,
         region_id,
         fit
     FROM $actual_fit
;
