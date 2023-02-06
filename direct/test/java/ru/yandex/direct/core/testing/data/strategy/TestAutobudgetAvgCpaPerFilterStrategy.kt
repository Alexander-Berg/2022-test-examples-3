package ru.yandex.direct.core.testing.data.strategy

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerFilter
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields

object TestAutobudgetAvgCpaPerFilterStrategy {
    @JvmStatic
    fun autobudgetAvgCpaPerFilter(): AutobudgetAvgCpaPerFilter =
        fillCommonClientFields(AutobudgetAvgCpaPerFilter())
            .withType(StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER)
            .withStatusArchived(false)
            .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
            .withGoalId(15L)
            .withBid(BigDecimal.valueOf(132.5))
            .withFilterAvgCpa(BigDecimal.valueOf(1111.5))
            .withSum(BigDecimal.valueOf(9999.99))
            .withIsPayForConversionEnabled(true)

}
