package ru.yandex.market.replenishment.autoorder.service

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import ru.yandex.market.replenishment.autoorder.config.yql.YqlQueryTest
import ru.yandex.market.replenishment.autoorder.service.yt.loader.ActualFitPreparationLoader
import ru.yandex.market.replenishment.autoorder.utils.TestUtils
import ru.yandex.market.yql_query_service.service.QueryService
import java.time.LocalDate

@ContextConfiguration(
    classes = [
        TimeService::class,
        QueryService::class,
        ActualFitPreparationLoader::class,
    ]
)
class ActualFitPreparationLoaderQueryTest : YqlQueryTest() {

    @Autowired
    private lateinit var actualFitPreparationLoader: ActualFitPreparationLoader

    @Autowired
    private lateinit var timeService: TimeService

    @Test
    fun testLoading() {
        Mockito.`when`(timeService.getNowDate()).thenReturn(LocalDate.of(2021, 9, 16))
        val query = actualFitPreparationLoader.getQuery()
        assertEquals(TestUtils.readResource("/queries/expected_replenishment_actual_fit_fill.yt.sql"), query)
    }
}
