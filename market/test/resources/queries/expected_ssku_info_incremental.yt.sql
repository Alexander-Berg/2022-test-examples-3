PRAGMA DisableAnsiInForEmptyOrNullableItemsCollections;

$fullfilment_partners = (
    SELECT id
        FROM `//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest`
        WHERE is_fullfilment = 1
);

-- Mappings for SSKU <-> (supplier_id/rs_id) && (msku/shop_sku)
$ssku_msku_mapping = (
    SELECT
        IF(
            m.approved_sku_mapping_id > 0,
            m.approved_sku_mapping_id,
            ofs.approved_market_sku_id
        ) AS msku,
        ofs.raw_shop_sku as shop_sku,
        ofs.raw_supplier_id as supplier_id,
        IF(m.approved_sku_mapping_ts > 0, m.approved_sku_mapping_ts, 0) AS approved_timestamp,
        IF(
            s.real_supplier_id IS NULL OR s.real_supplier_id = '',
            '3p.' || CAST(ofs.raw_supplier_id AS String) || '.' || ofs.raw_shop_sku,
            s.real_supplier_id || '.' || ofs.raw_shop_sku
        ) AS ssku,
        ofs.bar_code AS barcode,
        ofs.title as title
    FROM `//home/market/production/mbo/stat/mboc_offers_expanded_sku/latest` AS ofs
        JOIN `//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest` AS s
            ON s.id = ofs.raw_supplier_id
        LEFT JOIN `//home/market/production/mbo/mboc/offer-mapping` AS m
            ON ofs.raw_shop_sku = m.shop_sku and ofs.business_id = m.business_id
    WHERE s.is_fullfilment = 1 and (m.approved_sku_mapping_id > 0 OR ofs.approved_market_sku_id > 0)
        AND m.approved_sku_mapping_ts > 1614841511000
);

$mapDayOfWeek = ($x) -> {
   RETURN CASE $x
       WHEN "MONDAY" THEN "пн"
       WHEN "TUESDAY" THEN "вт"
       WHEN "WEDNESDAY" THEN "ср"
       WHEN "THURSDAY" THEN "чт"
       WHEN "FRIDAY" THEN "пт"
       WHEN "SATURDAY" THEN "сб"
       WHEN "SUNDAY" THEN "вс"
       ELSE null
   END
};

$parseSchedule = ($data) -> {
    RETURN ListConcat(
               ListFilter(
                    ListMap(
                        Yson::LookupList(
                            $data,
                            'supplySchedule',
                            Yson::Options(false as Strict)
                        ),
                        ($x) -> { RETURN $mapDayOfWeek(Yson::LookupString($x, 'dayOfWeek')); }
                    ),
                    ($x) -> { RETURN $x is not null; }
                ),
                ", "
            );
};

$master_data = (
    SELECT
        m.supplier_id   AS supplier_id,
        m.shop_sku      AS shop_sku,
        SOME(Yson::LookupString(
            m.data,
            'manufacturer',
            Yson::Options(false as Strict)
        ))              AS name,
        SOME(Yson::LookupDouble(
            m.data,
            'quantumOfSupply'
        ))              AS shipment_quantum,
        SOME(Yson::LookupDouble(
            m.data,
            'minShipment'
        ))              AS min_shipment,
        SOME(Yson::LookupDouble(
            m.data,
            'transportUnitSize',
            Yson::Options(true as AutoConvert)
        ))              AS transport_unit_size,
        SOME($parseSchedule(m.data)) AS supply_schedule
    FROM `//home/market/production/mstat/dictionaries/mdm/master_data/latest` AS m
    WHERE m.supplier_id > 0
        AND m.supplier_id IN $fullfilment_partners
        AND m.shop_sku IS NOT NULL
        AND m.shop_sku != ''
        AND m.data IS NOT NULL
    GROUP BY m.supplier_id, m.shop_sku
);

-- Единственный вариант, как мы в теории можем определять 1р поставщиков.
-- К сожалению, абсолютную гарантию он не дает, но в этой таблице попросту
-- нет колонок, которые бы точно дали понять, как формировать SSKU.
$weight_and_dimensions = (
    SELECT
        CASE WHEN supplier_id = 465852 THEN
            shop_sku
        ELSE
            '3p.' || CAST(supplier_id AS String) || '.' || shop_sku
        END                                                     AS ssku,
        CAST(SOME(height_micrometer_value) AS Int64) / 10000    AS height,
        CAST(SOME(length_micrometer_value) AS Int64) / 10000    AS length,
        CAST(SOME(width_micrometer_value) AS Int64) / 10000     AS width,
        CAST(SOME(gross_mg_value) AS Int64) / 1000              AS weight,
    FROM `//home/market/production/mdm/dictionaries/reference_item/1d/latest`
    WHERE supplier_id > 0 AND shop_sku IS NOT NULL AND shop_sku != ''AND supplier_id IN $fullfilment_partners
    GROUP BY supplier_id, shop_sku
);

$supply_prices = (
    SELECT
         '3p.' || CAST(supplier_id AS String) || '.' || CAST(article AS String) as ssku,
         supplier_id,
         MAX_BY(CAST(supply_price as double), id) AS supply_price
    FROM `//home/market/production/mstat/dictionaries/fulfillment_request_item/1d/latest`
    WHERE real_supplier_id IS NULL OR real_supplier_id = '' AND supplier_id IN $fullfilment_partners
          AND CAST(supply_price as double) > 0
    GROUP BY supplier_id,article
);

SELECT
    m.ssku                  AS ssku,
    m.msku                  AS msku,
    m.supplier_id           AS supplier_id,
    m.approved_timestamp    AS approved_timestamp,
    md.name                 AS manufacturer,
    m.barcode               AS barcode,
    d.height                AS height,
    d.length                AS length,
    d.width                 AS width,
    d.weight                AS weight,
    md.shipment_quantum     AS shipment_quantum,
    md.min_shipment         AS min_shipment,
    md.transport_unit_size  AS transport_unit_size,
    sp.supply_price         AS supply_price,
    md.supply_schedule      AS supply_schedule,
    ss.status               AS status,
    m.title                 AS title
FROM $ssku_msku_mapping AS m
    LEFT JOIN $master_data AS md
        ON md.supplier_id = m.supplier_id AND md.shop_sku = m.shop_sku
    LEFT JOIN $weight_and_dimensions AS d
        ON d.ssku = m.ssku
    LEFT JOIN $supply_prices AS sp
        ON sp.ssku = m.ssku
    LEFT JOIN `//home/market/production/deepmind/dictionaries/ssku_status/latest` AS ss
        ON ss.shop_sku = m.ssku and ss.raw_supplier_id = m.supplier_id
