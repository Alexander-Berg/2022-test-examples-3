package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.CpmDefault
import ru.yandex.direct.core.entity.strategy.model.StrategyDayBudgetShowMode
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields
import java.math.BigDecimal

object TestCpmDefaultStrategy {
    @JvmStatic
    fun clientCpmDefaultStrategy(): CpmDefault =
        fillCommonClientFields(CpmDefault())
            .withDayBudget(BigDecimal.valueOf(0L))
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.DEFAULT_)
            .withDayBudgetDailyChangeCount(0)
}
