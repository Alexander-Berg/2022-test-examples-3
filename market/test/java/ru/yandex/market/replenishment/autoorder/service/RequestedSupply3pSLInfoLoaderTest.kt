package ru.yandex.market.replenishment.autoorder.service

import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.RequestedSupply3pSLInfoLoader
import ru.yandex.market.yql_test.annotation.YqlTest
import java.time.LocalDateTime

@ActiveProfiles("unittest")
open class RequestedSupply3pSLInfoLoaderTest : FunctionalTest() {
    companion object {
        private val MOCK_DATE = LocalDateTime.of(2022, 2, 16, 1, 23)
    }

    @Autowired
    private lateinit var loader: RequestedSupply3pSLInfoLoader

    @Before
    fun mockDateTime() {
        setTestTime(MOCK_DATE)
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = [
            "//home/market/production/replenishment/reports/sl_3p/request_supplies",
        ],
        csv = "RequestedSupply3pSLInfoLoaderTest_import.yql.csv",
        yqlMock = "RequestedSupply3PSLInfoLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
        before = ["RequestedSupply3pSLInfoLoaderTest_import.before.csv"],
        after = ["RequestedSupply3pSLInfoLoaderTest_import.after.csv"],
    )
    open fun testLoadRequestedSupply3pSLInfo() = loader.load()
}
