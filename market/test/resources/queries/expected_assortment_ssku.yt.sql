SELECT
    child_sskus_list as sub_ssku,
    shop_sku as assort_ssku,
    supplier_id as supplier_id
FROM (
        SELECT
            Yson::ConvertToStringList(child_sskus) as child_sskus_list,
            shop_sku as shop_sku,
            supplier_id as supplier_id
        FROM `//home/market/prestable/mstat/dictionaries/mbo/warehouse_service/2022-01-23`
        WHERE shop_sku is not null and supplier_id is not null
     )
FLATTEN BY (child_sskus_list);
