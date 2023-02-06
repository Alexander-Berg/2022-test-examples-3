package ru.yandex.market.pricingmgmt.loaders.sku_price

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.SkuPriceLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class SkuPriceLoaderTest : ControllerTest() {
    @Autowired
    lateinit var loader: SkuPriceLoader

    @Test
    @DbUnitDataSet(after = ["SkuPriceLoaderTest_import.after.csv"])
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/ir/ultra-controller/supplier_to_market_sku",
            "//home/market/production/monetize/dynamic_pricing/assortment/latest"
        ],
        csv = "SkuPriceLoaderTest_import.yql.csv",
        yqlMock = "SkuPriceLoaderTest.yql.mock"
    )
    fun import() {
        loader.load()
    }
}
