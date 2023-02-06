package ru.yandex.direct.core.testing.data.strategy

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields

object TestAutobudgetCrrStrategy {
    @JvmStatic
    fun autobudgetCrr(): AutobudgetCrr =
        fillCommonClientFields(AutobudgetCrr())
            .withStatusArchived(false)
            .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
            .withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
            .withCrr(110L)
            .withSum(BigDecimal.valueOf(9999.99))
            .withIsPayForConversionEnabled(false)

}
