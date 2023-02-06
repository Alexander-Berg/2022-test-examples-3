package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressionsCustomPeriod
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields
import ru.yandex.direct.currency.currencies.CurrencyRub
import java.math.BigDecimal
import java.time.LocalDate

object TestAutobudgetMaxImpressionsCustomPeriodStrategy {
    
    @JvmStatic
    fun clientAutobudgetMaxImpressionsCustomPeriodStrategy(): AutobudgetMaxImpressionsCustomPeriod =
        fillCommonClientFields(AutobudgetMaxImpressionsCustomPeriod())
            .withAutoProlongation(true)
            .withBudget(BigDecimal.valueOf(30_000))
            .withStart(LocalDate.now())
            .withFinish(LocalDate.now().plusDays(7))
            .withAvgCpm(CurrencyRub.getInstance().defaultCpmPrice)
}
