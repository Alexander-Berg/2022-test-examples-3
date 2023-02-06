package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReach
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields
import ru.yandex.direct.currency.currencies.CurrencyRub
import java.math.BigDecimal

object TestAutobudgetMaxReachStrategy {
    @JvmStatic
    fun clientAutobudgetReachStrategy(): AutobudgetMaxReach =
        fillCommonClientFields(AutobudgetMaxReach())
            .withSum(BigDecimal.valueOf(30_000))
            .withAvgCpm(CurrencyRub.getInstance().defaultCpmPrice)
}
