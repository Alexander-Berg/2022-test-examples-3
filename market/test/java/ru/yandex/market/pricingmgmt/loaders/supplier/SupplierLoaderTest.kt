package ru.yandex.market.pricingmgmt.loaders.supplier

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.SupplierLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class SupplierLoaderTest : ControllerTest() {
    @Autowired
    lateinit var loader: SupplierLoader

    @Test
    @DbUnitDataSet(after = ["SupplierLoaderTest_import.after.csv"])
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/mstat/dictionaries/axapta_suppliers/latest"
        ],
        csv = "SupplierLoaderTest_import.yql.csv",
        yqlMock = "SupplierLoaderTest.yql.mock"
    )
    fun import() {
        loader.load()
    }
}
