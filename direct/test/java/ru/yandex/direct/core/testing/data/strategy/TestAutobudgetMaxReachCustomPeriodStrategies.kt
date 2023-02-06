package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields
import ru.yandex.direct.currency.currencies.CurrencyRub
import java.math.BigDecimal
import java.time.LocalDate

object TestAutobudgetMaxReachCustomPeriodStrategies {
    @JvmStatic
    fun clientAutobudgetMaxReachCustomPeriodStrategy(): AutobudgetMaxReachCustomPeriod =
        fillCommonClientFields(AutobudgetMaxReachCustomPeriod())
            .withAutoProlongation(true)
            .withAvgCpm(CurrencyRub.getInstance().defaultCpmPrice)
            .withBudget(BigDecimal.valueOf(30_000))
            .withStart(LocalDate.now().plusDays(1))
            .withFinish(LocalDate.now().plusDays(7))
}
