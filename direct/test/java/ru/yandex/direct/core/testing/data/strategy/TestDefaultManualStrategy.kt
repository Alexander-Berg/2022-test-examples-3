package ru.yandex.direct.core.testing.data.strategy

import java.math.BigDecimal
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyDayBudgetShowMode
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields

object TestDefaultManualStrategy {
    @JvmStatic
    fun clientDefaultManualStrategy(): DefaultManualStrategy =
        fillCommonClientFields(DefaultManualStrategy())
            .withDayBudget(BigDecimal.valueOf(10_000L))
            .withDayBudgetShowMode(StrategyDayBudgetShowMode.DEFAULT_)
            .withDayBudgetDailyChangeCount(1)
}
