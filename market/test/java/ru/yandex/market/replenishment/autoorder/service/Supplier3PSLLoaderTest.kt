package ru.yandex.market.replenishment.autoorder.service

import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.Supplier3pSLLoader
import ru.yandex.market.yql_test.annotation.YqlTest
import java.time.LocalDateTime

@ActiveProfiles("unittest")
open class Supplier3PSLLoaderTest : FunctionalTest() {
    companion object {
        private val MOCK_DATE = LocalDateTime.of(2022, 2, 16, 1, 23)
    }

    @Autowired
    private lateinit var loader: Supplier3pSLLoader

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
        csv = "Supplier3PSLLoaderTest_importSL.yql.csv",
        yqlMock = "Supplier3PSLLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
        before = ["Supplier3PSLLoaderTest_importSL.before.csv"],
        after = ["Supplier3PSLLoaderTest_importSL.after.csv"],
    )
    open fun testLoadSupplier3PSL() = loader.load()
}
