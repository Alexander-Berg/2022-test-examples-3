package ru.yandex.market.pricingmgmt.loaders.category

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.CategoryLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class CategoryLoaderTest : ControllerTest() {

    @Autowired
    lateinit var categoryLoader: CategoryLoader

    @Test
    @DbUnitDataSet(
        before = ["CategoryLoaderTest_importCategories.before.csv"],
        after = ["CategoryLoaderTest_importCategories.after.csv"]
    )
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/mbo/export/recent/tovar-tree"
        ],
        csv = "CategoryLoaderTest_importCategories.has.data.yql.csv",
        yqlMock = "CategoryLoaderTest.has.data.yql.mock"
    )
    fun importCategories_testLoading() {
        categoryLoader.load()
    }
}
