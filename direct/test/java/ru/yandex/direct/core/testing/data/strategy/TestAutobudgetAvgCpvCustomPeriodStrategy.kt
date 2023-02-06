package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpvCustomPeriod
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields
import ru.yandex.direct.currency.currencies.CurrencyRub
import java.math.BigDecimal
import java.time.LocalDate

object TestAutobudgetAvgCpvCustomPeriodStrategy {
    @JvmStatic
    fun clientAutobudgetAvgCpvCustomPeriodStrategy(): AutobudgetAvgCpvCustomPeriod =
        fillCommonClientFields(AutobudgetAvgCpvCustomPeriod())
            .withAutoProlongation(true)
            .withBudget(BigDecimal.valueOf(30_000))
            .withAvgCpv(CurrencyRub.getInstance().defaultAvgCpv)
            .withStart(LocalDate.now().plusDays(1))
            .withFinish(LocalDate.now().plusDays(7))
}
