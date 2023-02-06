package ru.yandex.direct.core.testing.data.strategy

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerCamp
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields

object TestAutobudgetAvgCpaPerCampStrategy {
    @JvmStatic
    fun autobudgetAvgCpaPerCamp(): AutobudgetAvgCpaPerCamp =
        fillCommonClientFields(AutobudgetAvgCpaPerCamp())
            .withStatusArchived(false)
            .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
            .withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
            .withAvgCpa(BigDecimal.valueOf(678.9))
            .withSum(BigDecimal.valueOf(9999.99))
            .withIsPayForConversionEnabled(false)
}
