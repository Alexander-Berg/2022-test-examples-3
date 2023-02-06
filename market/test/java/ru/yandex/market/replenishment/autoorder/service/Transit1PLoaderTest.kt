package ru.yandex.market.replenishment.autoorder.service

import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.Transit1PLoader
import ru.yandex.market.yql_test.annotation.YqlTest
import java.time.LocalDateTime

open class Transit1PLoaderTest : FunctionalTest() {

    companion object {
        private val MOCK_DATE = LocalDateTime.of(2020, 12, 12, 8, 0)
    }

    @Before
    fun mockDateTime() {
        setTestTime(MOCK_DATE)
    }

    @Autowired
    private lateinit var loader: Transit1PLoader

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/replenishment/order_planning/2020-12-12/intermediate/transits_raw",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest",
            "//home/market/production/ir/ultra-controller/supplier_to_market_sku"
        ],
        csv = "TransitLoaderTest_importTransits.yql.csv",
        yqlMock = "TransitLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
        before = ["TransitLoaderTest_importTransits.before.csv"],
        after = ["TransitLoaderTest_importTransits.after.csv"],
    )
    open fun testLoad() = loader.load()
}
