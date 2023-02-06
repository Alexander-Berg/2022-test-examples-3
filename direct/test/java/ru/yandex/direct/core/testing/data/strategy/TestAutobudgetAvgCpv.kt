package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpv
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields
import ru.yandex.direct.currency.currencies.CurrencyRub

object TestAutobudgetAvgCpv {

    @JvmStatic
    fun autobudgetAvgCpv(): AutobudgetAvgCpv =
        fillCommonClientFields(AutobudgetAvgCpv())
            .withAvgCpv(CurrencyRub.getInstance().defaultAvgCpv)
            .withSum(CurrencyRub.getInstance().minDailyBudgetForPeriod.multiply(7L.toBigDecimal()))

}
