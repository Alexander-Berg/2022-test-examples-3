package ru.yandex.market.pricingmgmt.loaders.stock

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.StockWarehouseLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class StockWarehouseLoaderTest : ControllerTest() {
    @Autowired
    lateinit var loader: StockWarehouseLoader

    @Test
    @DbUnitDataSet(
        before = ["StockWarehouseLoaderTest_import.before.csv"],
        after = ["StockWarehouseLoaderTest_import.after.csv"]
    )
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/dictionaries/stock_sku/1d/latest"
        ],
        csv = "StockWarehouseLoaderTest_import.yql.csv",
        yqlMock = "StockWarehouseLoaderTest.yql.mock"
    )
    fun import() {
        loader.load()
    }
}
