pragma yt.InferSchema='1';
PRAGMA AnsiInForEmptyOrNullableItemsCollections;

$prices_3p = (SELECT
    ssku,
    null as purchase_price,
    null as purchase_promo_price,
    null as purchase_result_price,
    max_by(mo.price, mo.from_datetime) as sale_price,
    null as regular_price
FROM `//home/market/production/mstat/analyst/market-offers/2021-06-14` as mo
    LEFT JOIN `//home/market/production/ir/ultra-controller/supplier_to_market_sku` as stms
        ON mo.market_sku = stms.approved_market_sku_id
WHERE mo.market_sku is not null
    AND mo.price > 0
    AND mo.supplier_type='3'
    AND mo.is_blue_offer
    AND mo.is_last_gen
    AND not mo.is_offer_has_gone
    AND mo.in_last_index
    AND mo.market_sku IN (
            SELECT distinct msku
            FROM `//home/market/production/replenishment/order_planning/2021-06-15/outputs/recommendations_3p` AS r
            WHERE msku is not null AND r.supplier_type = 3)
GROUP BY ('3p.' ||  CAST(stms.shop_id AS String) || '.' || CAST(mo.market_sku as String))  as ssku);


$prices_1p = (SELECT
    purchase_price.ssku as ssku,
    Yson::ConvertToDouble(Yql::Lookup(Yson::ConvertToDict(purchase_price.dict_regular), '2021-06-15')) as purchase_price,
    Yson::ConvertToDouble(Yql::Lookup(Yson::ConvertToDict(purchase_price.dict_promo), '2021-06-15')) as purchase_promo_price,
    Yson::ConvertToDouble(Yql::Lookup(Yson::ConvertToDict(purchase_price.dict_purchase_price), '2021-06-15')) as purchase_result_price,
    sale_price.price as sale_price,
    sale_price.regular_price as regular_price
FROM `//home/market/production/replenishment/order_planning/2021-06-15/intermediate/assortment_prices_dicts` as purchase_price
    LEFT JOIN `//home/market/production/ir/ultra-controller/supplier_to_market_sku` as sku_mapping
        ON sku_mapping.shop_sku_id = purchase_price.ssku
    LEFT JOIN ANY `//home/market/production/replenishment/prod/sales_dynamics/calculation_steps/beru_offers/beru_offers_2021-06-15` as sale_price
        ON sale_price.market_sku = sku_mapping.approved_market_sku_id
WHERE sku_mapping.shop_id = 465852);

SELECT * from $prices_1p
union all
SELECT * FROM $prices_3p
WHERE ssku IS NOT NULL;
