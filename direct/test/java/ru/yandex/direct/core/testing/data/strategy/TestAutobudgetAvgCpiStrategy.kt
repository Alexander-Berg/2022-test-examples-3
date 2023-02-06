package ru.yandex.direct.core.testing.data.strategy

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields

object TestAutobudgetAvgCpiStrategy {
    @JvmStatic
    fun autobudgetAvgCpi(): AutobudgetAvgCpi =
        fillCommonClientFields(AutobudgetAvgCpi())
            .withType(StrategyName.AUTOBUDGET_AVG_CPI)
            .withStatusArchived(false)
            .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
            .withBid(BigDecimal.valueOf(132.5))
            .withAvgCpi(BigDecimal.valueOf(13.6))
            .withSum(BigDecimal.valueOf(9999.99))
}
