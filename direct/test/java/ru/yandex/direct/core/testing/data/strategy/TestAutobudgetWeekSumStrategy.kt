package ru.yandex.direct.core.testing.data.strategy

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekSum
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields

object TestAutobudgetWeekSumStrategy {

    @JvmStatic
    fun autobudget(): AutobudgetWeekSum =
        fillCommonClientFields(AutobudgetWeekSum())
            .withType(StrategyName.AUTOBUDGET)
            .withStatusArchived(false)
            .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
            .withGoalId(15L)
            .withBid(BigDecimal.valueOf(132.5))
            .withSum(BigDecimal.valueOf(9999.99))

}
