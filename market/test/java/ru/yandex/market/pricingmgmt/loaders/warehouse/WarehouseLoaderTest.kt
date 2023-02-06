package ru.yandex.market.pricingmgmt.loaders.warehouse

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.WarehouseLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class WarehouseLoaderTest : ControllerTest() {

    @Autowired
    lateinit var warehouseLoader: WarehouseLoader

    @Test
    @DbUnitDataSet(after = ["WarehouseLoaderTest_importWarehouses.after.csv"])
    @YqlTest(schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/analyst/regular/cubes_vertica/dim_warehouse"
        ],
        csv = "WarehouseLoaderTest_importWarehouses.yql.csv",
        yqlMock = "WarehouseLoaderTest.yql.mock")
    fun importWarehouses() {
        warehouseLoader.load()
    }

    @Test
    @DbUnitDataSet(after = ["WarehouseLoaderTest_importWarehouses.after.csv"],
        before = ["WarehouseLoaderTest_importWarehousesIfExist.before.csv"])
    @YqlTest(schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/analyst/regular/cubes_vertica/dim_warehouse"
        ],
        csv = "WarehouseLoaderTest_importWarehouses.yql.csv",
        yqlMock = "WarehouseLoaderTest.yql.mock")
    fun importWarehousesIfAlreadyExist() {
        warehouseLoader.load()
    }
}
