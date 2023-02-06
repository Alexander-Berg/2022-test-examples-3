package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerCamp
import ru.yandex.direct.currency.currencies.CurrencyRub

object TestAutobudgetAvgCpcPerCampStrategy {
    @JvmStatic
    fun autobudgetAvgCpcPerCamp(): AutobudgetAvgCpcPerCamp =
        TestCommonStrategy.fillCommonClientFields(AutobudgetAvgCpcPerCamp())
            .withStatusArchived(false)
            .withAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice)
}
