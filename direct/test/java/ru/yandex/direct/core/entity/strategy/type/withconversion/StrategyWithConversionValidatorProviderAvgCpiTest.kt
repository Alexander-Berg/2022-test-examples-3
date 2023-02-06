package ru.yandex.direct.core.entity.strategy.type.withconversion

import org.junit.Test
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.strategy.model.StrategyWithConversion
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

class StrategyWithConversionValidatorProviderAvgCpiTest : StrategyWithConversionValidatorProviderBaseTest() {

    private fun strategy() =
        autobudgetAvgCpi()
            .withGoalId(metrikaGoal.id)
            .withClientId(clientId)

    @Test
    fun `no mobile goal on RMP campaign`() {
        val strategy = strategy()
        val container =
            container(clientId, CampaignType.MOBILE_CONTENT, goalsMap = mapOf(strategy to setOf(metrikaGoal)))
        val matcher = Matchers.hasDefectDefinitionWith<StrategyWithConversion>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(StrategyWithConversion.GOAL_ID)),
                CommonDefects.objectNotFound()
            )
        )
        checkValidationFailed(strategy, container, matcher)
    }

    @Test
    fun `no mobile goal on avg_cpi strategy`() {
        val strategy = strategy()
        val container = container(clientId, CampaignType.TEXT, goalsMap = mapOf(strategy to setOf(metrikaGoal)))
        val matcher = Matchers.hasDefectDefinitionWith<StrategyWithConversion>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(StrategyWithConversion.GOAL_ID)),
                CommonDefects.objectNotFound()
            )
        )
        checkValidationFailed(strategy, container, matcher)
    }

    @Test
    fun `goal not available`() {
        val strategy = strategy()
        val container = container(clientId, CampaignType.TEXT)
        val matcher = Matchers.hasDefectDefinitionWith<StrategyWithConversion>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(StrategyWithConversion.GOAL_ID)),
                CommonDefects.objectNotFound()
            )
        )
        checkValidationFailed(strategy, container, matcher)
    }

    @Test
    fun `goal id can be null`() {
        val strategy = strategy().withGoalId(null)
        val container = container(clientId, CampaignType.TEXT, goalsMap = mapOf(strategy to setOf(metrikaGoal)))
        checkSuccess(strategy, container)
    }

    @Test
    fun `strategy with mobile goal`() {
        val strategy = strategy().withGoalId(mobileGoal.id)
        val container = container(clientId, CampaignType.TEXT, goalsMap = mapOf(strategy to setOf(mobileGoal)))
        checkSuccess(strategy, container)
    }
}
