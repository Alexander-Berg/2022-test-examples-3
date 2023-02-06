package ru.yandex.market.pricingmgmt.executor.PricesSelectionHistoryCleanExecutor

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.executor.PricesSelectionHistoryCleanExecutor
import ru.yandex.market.pricingmgmt.service.TimeService
import java.time.LocalDateTime

class PricesSelectionHistoryCleanExecutorTest(
    @Autowired private val pricesSelectionHistoryCleanExecutor: PricesSelectionHistoryCleanExecutor,
    @Autowired private val timeService: TimeService,
) : ControllerTest() {

    @Test
    @DbUnitDataSet(
        before = ["PricesSelectionHistoryCleanExecutorTest.before.csv"],
        after = ["PricesSelectionHistoryCleanExecutorTest.after.csv"]
    )
    fun testCleanLogs() {
        Mockito.`when`(timeService.getNowDateTime()).thenReturn(
            LocalDateTime.of(2022, 3, 20, 0, 0, 0)
        )

        pricesSelectionHistoryCleanExecutor.run()
    }
}
