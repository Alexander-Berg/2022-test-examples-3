package ru.yandex.market.pricingmgmt.loaders.warehouseForPromo

import org.junit.jupiter.api.Test
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.loaders.WarehouseForPromoLoader
import ru.yandex.market.yql_test.annotation.YqlTest

internal class WarehouseForPromoLoaderTest : AbstractFunctionalTest() {
    @Autowired
    lateinit var loader: WarehouseForPromoLoader

    @Test
    @DbUnitDataSet(
        before = ["WarehouseForPromoLoaderTest_load.before.csv"],
        after = ["WarehouseForPromoLoaderTest_load.after.csv"]
    )
    @YqlTest(schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/analyst/regular/cubes_vertica/dim_warehouse"
        ],
        csv = "WarehouseForPromoLoaderTest_load.yql.csv",
        yqlMock = "WarehouseForPromoLoaderTest.yql.mock")
    fun load() {
        loader.load()
    }
}
