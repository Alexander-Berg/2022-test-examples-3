package ru.yandex.market.pricingmgmt.loaders.vendor

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.VendorLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class VendorLoaderTest : ControllerTest() {
    @Autowired
    lateinit var loader: VendorLoader

    @Test
    @DbUnitDataSet(
        before = ["VendorLoaderTest_import.before.csv"],
        after = ["VendorLoaderTest_import.after.csv"])
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/ir/ultra-controller/supplier_to_market_sku",
            "//home/market/production/mbo/stat/mboc_offers_expanded_sku/latest",
            "//home/market/production/mstat/dictionaries/deepmind/ssku_status/latest",
            "//home/market/production/mstat/dictionaries/mbo/all_vendors/latest"
        ],
        csv = "VendorLoaderTest_import.yql.csv",
        yqlMock = "VendorLoaderTest.yql.mock"
    )
    fun import() {
        loader.load()
    }
}
