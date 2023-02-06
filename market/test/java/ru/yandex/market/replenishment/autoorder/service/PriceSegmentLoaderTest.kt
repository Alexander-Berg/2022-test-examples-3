package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.PriceSegmentLoader
import ru.yandex.market.yql_test.annotation.YqlTest

class PriceSegmentLoaderTest : FunctionalTest() {

    @Autowired
    private lateinit var loader: PriceSegmentLoader

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/deepmind/dictionaries/msku_priceband/latest",
        ],
        csv = "PriceSegmentLoaderTest.yql.csv",
        yqlMock = "PriceSegmentLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
        before = ["PriceSegmentLoaderTest.before.csv"],
        after = ["PriceSegmentLoaderTest.after.csv"],
    )
    fun testLoad() = loader.load()
}
