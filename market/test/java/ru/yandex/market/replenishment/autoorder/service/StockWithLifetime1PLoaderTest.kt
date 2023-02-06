package ru.yandex.market.replenishment.autoorder.service

import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.StockWithLifetime1PLoader
import ru.yandex.market.yql_test.annotation.YqlTest
import java.time.LocalDateTime

@ActiveProfiles("unittest")
open class StockWithLifetime1PLoaderTest : FunctionalTest() {
    companion object {
        private val MOCK_DATE = LocalDateTime.of(2020, 5, 15, 8, 0)
    }

    @Autowired
    private lateinit var loader: StockWithLifetime1PLoader

    @Before
    fun mockDateTime() {
        setTestTime(MOCK_DATE)
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/stock_with_lifetime",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/warehouses",
            "//home/market/production/replenishment/order_planning/2020-05-15/intermediate/suppliers",
        ],
        csv = "StockWithLifetimeLoaderTest_importStocksWithLifetimes.yql.csv",
        yqlMock = "StockWithLifetimeLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
        before = ["StockWithLifetimeLoaderTest_importStocksWithLifetimes.before.csv"],
        after = ["StockWithLifetimeLoaderTest_importStocksWithLifetimes.after.csv"],
    )
    open fun testLoadStocksWithLifetimes() = loader.load()
}
