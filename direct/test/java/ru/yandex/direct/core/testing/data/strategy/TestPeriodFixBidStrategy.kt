package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.PeriodFixBid
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields
import java.math.BigDecimal
import java.time.LocalDate

object TestPeriodFixBidStrategy {
    @JvmStatic
    fun clientPeriodFixBidStrategy(): PeriodFixBid =
        fillCommonClientFields(PeriodFixBid())
            .withAutoProlongation(true)
            .withBudget(BigDecimal.valueOf(30_000))
            .withStart(LocalDate.now().plusDays(1))
            .withFinish(LocalDate.now().plusDays(7))
}
