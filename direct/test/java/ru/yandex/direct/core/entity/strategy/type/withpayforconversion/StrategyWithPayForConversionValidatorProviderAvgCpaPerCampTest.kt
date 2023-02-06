package ru.yandex.direct.core.entity.strategy.type.withpayforconversion

import org.junit.Test
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.strategy.model.StrategyWithPayForConversion
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerCampStrategy.autobudgetAvgCpaPerCamp
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.PathHelper

class StrategyWithPayForConversionValidatorProviderAvgCpaPerCampTest :
    StrategyWithPayForConversionValidatorProviderBaseTest() {
    override fun strategy(): StrategyWithPayForConversion =
        autobudgetAvgCpaPerCamp()
            .withGoalId(metrikaGoal.id)
            .withClientId(clientId)

    @Test
    fun `strategy inconsistent when pay for conversion extended mode (CPM_BANNER, feature disabled)`() {
        val strategy = strategy().withIsPayForConversionEnabled(true)
        val container = container(clientId, CampaignType.CPM_BANNER)
        val matcher = Matchers.hasDefectDefinitionWith<StrategyWithPayForConversion>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(StrategyWithPayForConversion.IS_PAY_FOR_CONVERSION_ENABLED)),
                StrategyDefects.inconsistentStrategyToCampaignType()
            )
        )
        checkValidationFailed(strategy, container, matcher)
    }

    @Test
    fun `strategy consistent when pay for conversion extended mode (CPM_BANNER, feature enabled)`() {
        val strategy = strategy().withIsPayForConversionEnabled(true)
        val container = container(
            clientId,
            CampaignType.CPM_BANNER,
            availableFeatures = setOf(FeatureName.CPA_STRATEGY_IN_CPM_BANNER_CAMPAIGN_ENABLED)
        )
        checkSuccess(strategy, container)
    }

    @Test
    fun `strategy consistent when pay for conversion extended mode when CPA_PAY_FOR_CONVERSIONS_EXTENDED_MODE enabled`() {
        val strategy = strategy().withIsPayForConversionEnabled(true)
        val container = container(clientId, CampaignType.TEXT)
        checkSuccess(strategy, container)
    }

}
