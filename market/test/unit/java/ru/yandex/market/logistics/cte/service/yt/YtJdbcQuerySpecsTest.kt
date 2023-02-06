package ru.yandex.market.logistics.cte.service.yt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.cte.entity.asc.SupplierIdAndSkuDTO

internal class YtJdbcQuerySpecsTest {

    @Test
    fun testLoadGuaranteePeriodQuery() {
        val trimIndent = """
            PRAGMA AnsiInForEmptyOrNullableItemsCollections;
            SELECT
                mboc_offers_expanded_sku.supplier_id as supplier_id,
                mboc_offers_expanded_sku.shop_sku as shop_sku,
                Yson::ConvertToInt64(Yson::YPath(Yson::Parse(data), '/guaranteePeriod/time')) as time,
                Yson::ConvertToString(Yson::YPath(Yson::Parse(data), '/guaranteePeriod/unit')) as unit
            FROM hahn.`//home/market/prestable/mdm/dictionaries/master_data/1d/latest` as master_data
            inner join hahn.`//home/market/prestable/mstat/dictionaries/mbo/mboc_offers_expanded_sku/latest`
            as mboc_offers_expanded_sku
            on mboc_offers_expanded_sku.raw_supplier_id = master_data.supplier_id
            and mboc_offers_expanded_sku.raw_shop_sku = master_data.shop_sku
            WHERE (mboc_offers_expanded_sku.supplier_id, mboc_offers_expanded_sku.shop_sku)
                in (('1','sku1'),('2','sku2'))
        """.trimIndent()
        assertEquals(trimIndent,
            YtJdbcQuerySpecs.getLoadGuaranteePeriodQuery(
                listOf(
                    SupplierIdAndSkuDTO(1, "sku1"),
                    SupplierIdAndSkuDTO(2, "sku2")),
                "prestable"))

    }

    @Test
    fun testLoadBrandQuery() {
        val trimIndent = """
            PRAGMA AnsiInForEmptyOrNullableItemsCollections;
            select
            rov.sku as external_sku,
            dim.msku_vendor_id as supplier_id,
            dim.msku_vendor_name as brand
            from hahn.`//home/market/prestable/mstat/dictionaries/wms/wms_sku/latest` as rov
            left JOIN (
                select ssku,msku_msku,msku_vendor_name, msku_vendor_id
                from hahn.`//home/market/prestable/mstat/analyst/regular/cubes_vertica/dim_ssku_flattened`
                group by ssku,msku_msku,msku_vendor_name, msku_vendor_id) as dim on rov.manufacturersku = dim.ssku
            where (dim.msku_vendor_id, rov.sku) in (('1','sku1'),('2','sku2'))
            group by
            rov.sku,
            dim.msku_vendor_id
            dim.msku_vendor_name
        """.trimIndent()
        assertEquals(trimIndent,
            YtJdbcQuerySpecs.getLoadBrandQuery(
                listOf(
                    SupplierIdAndSkuDTO(1, "sku1"),
                    SupplierIdAndSkuDTO(2, "sku2")),
                "prestable"))

    }
}
