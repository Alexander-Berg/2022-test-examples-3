package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgClick
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields
import ru.yandex.direct.currency.currencies.CurrencyRub

object TestAutobudgetAvgClick {
    @JvmStatic
    fun autobudgetAvgClick(): AutobudgetAvgClick =
        fillCommonClientFields(AutobudgetAvgClick())
            .withStatusArchived(false)
            .withAvgBid(CurrencyRub.getInstance().maxAutobudgetBid)

}
