package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.currency.currencies.CurrencyRub
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object TestAutobudgetWeekBundleStrategy {
    @JvmStatic
    fun autobudgetWeekBundle(): AutobudgetWeekBundle =
        TestCommonStrategy.fillCommonClientFields(AutobudgetWeekBundle())
            .withStatusArchived(false)
            .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
            .withBid(CurrencyRub.getInstance().maxAutobudgetBid)
            .withAvgBid(null)
            .withLimitClicks(1000)
}
