package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressions
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields
import ru.yandex.direct.currency.currencies.CurrencyRub

object TestAutobudgetMaxImpressionsStrategy {
    @JvmStatic
    fun clientAutobudgetMaxImpressionsStrategy(): AutobudgetMaxImpressions =
        fillCommonClientFields(AutobudgetMaxImpressions())
            .withSum(CurrencyRub.getInstance().maxAutobudget)
            .withAvgCpm(CurrencyRub.getInstance().defaultCpmPrice)
}
