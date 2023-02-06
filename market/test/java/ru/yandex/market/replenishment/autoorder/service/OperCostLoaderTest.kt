package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.OperCostLoader
import ru.yandex.market.yql_test.annotation.YqlTest
import java.time.LocalDate

class OperCostLoaderTest : FunctionalTest() {

    @Autowired
    lateinit var loader: OperCostLoader

    @Autowired
    private lateinit var timeService: TimeService

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = [
                "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_ue_partitioned/2022-05"
            ],
            csv = "OperCostLoaderTest.yql.csv",
            yqlMock = "OperCostLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
            before = ["OperCostLoaderTest.before.csv"],
            after = ["OperCostLoaderTest.after.csv"],
    )
    fun testLoad_isOk() {
        Mockito.`when`(timeService.getNowDate()).thenReturn(LocalDate.of(2022, 6, 15))
        loader.load()
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = [
                "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_ue_partitioned/2022-05"
            ],
            csv = "OperCostLoaderTest.yql.csv",
            yqlMock = "OperCostLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
            before = ["OperCostLoaderTest.before.csv"],
            after = ["OperCostLoaderTest.after.csv"],
    )
    fun testDoubleLoad_isOk() {
        Mockito.`when`(timeService.getNowDate()).thenReturn(LocalDate.of(2022, 6, 15))
        loader.load()
        loader.load()
    }
}
