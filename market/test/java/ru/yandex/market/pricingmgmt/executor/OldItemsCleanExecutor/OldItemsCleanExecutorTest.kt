package ru.yandex.market.pricingmgmt.executor.OldItemsCleanExecutor

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.executor.OldItemsCleanExecutor
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil
import java.time.LocalDateTime

class OldItemsCleanExecutorTest(
    @Autowired private val oldItemsCleanExecutor: OldItemsCleanExecutor,
    @Autowired private val timeService: TimeService,
) : ControllerTest() {

    @Test
    @DbUnitDataSet(
        before = ["OldItemsCleanExecutor.before.csv"],
        after = ["OldItemsCleanExecutor.after.csv"]
    )
    fun testClean() {
        Mockito.`when`(timeService.getNowDateTime()).thenReturn(
            LocalDateTime.of(2022, 3, 20, 0, 0, 0)
        )
        Mockito.`when`(timeService.getNowOffsetDateTime()).thenReturn(
            DateTimeTestingUtil.createOffsetDateTime(2022, 3, 20, 0, 0, 0)
        )

        oldItemsCleanExecutor.run()
    }
}
