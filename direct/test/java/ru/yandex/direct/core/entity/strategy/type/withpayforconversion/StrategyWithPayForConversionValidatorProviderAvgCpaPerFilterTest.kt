package ru.yandex.direct.core.entity.strategy.type.withpayforconversion

import org.junit.Test
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.StrategyWithPayForConversion
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter

class StrategyWithPayForConversionValidatorProviderAvgCpaPerFilterTest :
    StrategyWithPayForConversionValidatorProviderBaseTest() {
    override fun strategy(): StrategyWithPayForConversion =
        autobudgetAvgCpaPerFilter()
            .withGoalId(metrikaGoal.id)
            .withClientId(clientId)

    @Test
    fun `pay for conversion is allowable for BY_ALL_GOALS_GOAL_ID`() {
        val strategy = strategy()
            .withGoalId(CampaignConstants.BY_ALL_GOALS_GOAL_ID)
            .withIsPayForConversionEnabled(true)
        val container = container(clientId, CampaignType.TEXT, options = StrategyOperationOptions())
        checkSuccess(strategy, container)
    }

    @Test
    fun `pay for conversion is allowable for MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID`() {
        val strategy = strategy()
            .withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
            .withIsPayForConversionEnabled(true)
        val container = container(clientId, CampaignType.TEXT, options = StrategyOperationOptions())
        checkSuccess(strategy, container)
    }
}
