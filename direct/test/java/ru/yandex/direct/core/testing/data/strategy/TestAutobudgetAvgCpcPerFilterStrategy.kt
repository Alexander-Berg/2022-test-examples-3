package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerFilter
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields
import ru.yandex.direct.currency.currencies.CurrencyRub

object TestAutobudgetAvgCpcPerFilterStrategy {
    @JvmStatic
    fun autobudgetAvgCpcPerFilter(): AutobudgetAvgCpcPerFilter =
        fillCommonClientFields(AutobudgetAvgCpcPerFilter())
            .withStatusArchived(false)
            .withBid(CurrencyRub.getInstance().maxAutobudgetBid)
            .withFilterAvgBid(CurrencyRub.getInstance().maxAutobudgetBid)
}
