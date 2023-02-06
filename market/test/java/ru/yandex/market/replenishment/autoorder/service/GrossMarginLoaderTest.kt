package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.GrossMarginLoader
import ru.yandex.market.yql_test.annotation.YqlTest
import java.time.LocalDate

class GrossMarginLoaderTest : FunctionalTest() {

    @Autowired
    lateinit var loader: GrossMarginLoader

    @Autowired
    private lateinit var timeService: TimeService

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = [
                "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_ue_partitioned/2022-05",
                "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_ue_partitioned/2022-04"
            ],
            csv = "GrossMarginLoaderTest.yql.csv",
            yqlMock = "GrossMarginLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
            before = ["GrossMarginLoaderTest.before.csv"],
            after = ["GrossMarginLoaderTest.after.csv"],
    )
    fun testLoad_isOk() {
        Mockito.`when`(timeService.getNowDate()).thenReturn(LocalDate.of(2022, 6, 15))
        loader.load()
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = [
                "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_ue_partitioned/2022-05",
                "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_ue_partitioned/2022-04"
            ],
            csv = "GrossMarginLoaderTest.yql.csv",
            yqlMock = "GrossMarginLoaderTest.yql.mock",
    )
    @DbUnitDataSet(
            before = ["GrossMarginLoaderTest.before.csv"],
            after = ["GrossMarginLoaderTest.after.csv"],
    )
    fun testDoubleLoad_isOk() {
        Mockito.`when`(timeService.getNowDate()).thenReturn(LocalDate.of(2022, 6, 15))
        loader.load()
        loader.load()
    }
}
