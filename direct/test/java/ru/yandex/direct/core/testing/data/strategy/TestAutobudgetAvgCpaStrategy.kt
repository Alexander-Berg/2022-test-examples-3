package ru.yandex.direct.core.testing.data.strategy

import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.testing.data.strategy.TestCommonStrategy.fillCommonClientFields
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object TestAutobudgetAvgCpaStrategy {
    @JvmStatic
    fun autobudgetAvgCpa(): AutobudgetAvgCpa =
        fillCommonClientFields(AutobudgetAvgCpa())
            .withType(StrategyName.AUTOBUDGET_AVG_CPA)
            .withStatusArchived(false)
            .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
            .withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
            .withBid(BigDecimal.valueOf(12))
            .withAvgCpa(BigDecimal.valueOf(50))
}
