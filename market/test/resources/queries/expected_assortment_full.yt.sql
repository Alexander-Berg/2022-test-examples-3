-- WARNING: не нужно оборачивать названия таблиц в tbl()! Тест работает исключительно через кэш YT из файлика
-- "AssortmentLoaderTest_testLoading.yql.mock"
PRAGMA DisableAnsiInForEmptyOrNullableItemsCollections;

$exctract_date = ($dateString) -> {
    return DateTime::MakeDate(DateTime::Parse("%Y-%m-%d")(COALESCE($dateString, "")));
};

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

$abc = (
    SELECT DISTINCT
        market_sku as msku,
    -- Здесь может быть несколько разных abc_gmv за одну дату, выберется случайная
        MAX_BY(abc_gmv, $exctract_date(`date`)) as abc
    FROM `//home/market/production/analytics/business/operations/replenishment/abc/total/latest`
    WHERE `date` is not null
        AND market_sku IN $msku_with_fullfilment_supplier
    GROUP BY market_sku
);


$departments = (
    SELECT
        msku_msku as msku,
        some(msku_category_category_directors_category_team) as department_name
    FROM `//home/market/production/mstat/analyst/regular/cubes_vertica/dim_ssku_flattened`
    WHERE msku_category_category_directors_category_team != ''
        AND shop_id IN $fullfilment_partners
    GROUP BY msku_msku
);

-- Эта часть вынесена в подзапрос, потому что такой хак магическим образом уменьшает время выгрузки YT с ~ 4,5 минут
-- до < 3,5 минут
$assortment = (
    SELECT
        model_id    AS msku,
        title,
        category_id,
        CAST(
            Listhead(
                ListFilter(data.parameter_values, ($x) -> {
                    return $x.xsl_name = 'packageNumInSpike'
                })
            ).numeric_value
            as Uint32
        )           AS package_num_in_spike,
        Listhead(
            ListFilter(data.parameter_values, ($c) -> {
                return $c.xsl_name IN ('cargoType980','cargoType985','cargoType990') AND $c.bool_value
            })
        ).xsl_name AS honest_sign,
        data.modified_ts AS modified_ts
        FROM `//home/market/production/mbo/export/recent/models/sku`
        WHERE model_id > 0 AND title != '' AND category_id > 0
        AND model_id IN $msku_with_fullfilment_supplier
        );


$fact_golden_matrix = (
    SELECT DISTINCT market_sku  AS msku
    FROM `//home/market/production/mstat/analyst/regular/cubes_vertica/fact_golden_matrix`
    WHERE market_sku IS NOT NULL
        AND market_sku IN $msku_with_fullfilment_supplier
);

$vendor_and_reserve_title_info = (
    SELECT
        o.approved_market_sku_id AS msku,
        some(if(vendor_id > 0, vendor_id)) as vendor_id,
        some(if(vendor_code is not null AND vendor_code != '', vendor_code)) as vendor_code,
        some(if(title is not null AND title != '', title)) as reserve_title
    FROM `//home/market/production/mbo/stat/mboc_offers_expanded_sku/latest` AS o
    WHERE o.approved_market_sku_id > 0
        AND o.supplier_id IN $fullfilment_partners
    GROUP BY o.approved_market_sku_id
);

SELECT
    a.msku                              AS msku,
    coalesce(a.title, vi.reserve_title) AS title,
    a.category_id               AS category_id,
    a.package_num_in_spike      AS package_num_in_spike,
    a.modified_ts               AS modified_ts,
    a.honest_sign               AS honest_sign,
    abc.abc                     AS abc,
    d.department_name           AS department_name,
    fgm.msku IS NOT NULL        AS core_fix_matrix,
    vi.vendor_code              AS vendor_code,
    vi.vendor_id                AS vendor_id
FROM $assortment AS a
    LEFT JOIN $abc AS abc ON abc.msku = a.msku
    LEFT JOIN $departments AS d ON d.msku = a.msku
    LEFT JOIN $fact_golden_matrix AS fgm ON fgm.msku = a.msku
    LEFT JOIN $vendor_and_reserve_title_info AS vi ON vi.msku = a.msku;
