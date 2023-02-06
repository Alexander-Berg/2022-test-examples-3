package ru.yandex.direct.core.testing.data.strategy

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import ru.yandex.direct.core.entity.strategy.model.AutobudgetRoi
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields

object TestAutobudgetRoiStrategy {
    @JvmStatic
    fun autobudgetRoi(): AutobudgetRoi =
        fillCommonClientFields(AutobudgetRoi())
            .withStatusArchived(false)
            .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
            .withGoalId(15L)
            .withBid(BigDecimal.valueOf(132.5))
            .withSum(BigDecimal.valueOf(9999.99))
            .withProfitability(BigDecimal.valueOf(1.5))
            .withReserveReturn(10L)
            .withRoiCoef(BigDecimal.valueOf(20))
}
