package ru.yandex.direct.core.entity.strategy.type.withconversion

import org.junit.Test
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.strategy.model.StrategyWithConversion
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper

class StrategyWithConversionValidatorProviderCrrTest : StrategyWithConversionValidatorProviderBaseTest() {
    private fun strategy() =
        autobudgetCrr()
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
    fun `goal id can not be null`() {
        val strategy = strategy().withGoalId(null)
        val container = container(clientId, CampaignType.TEXT, goalsMap = mapOf(strategy to setOf(metrikaGoal)))
        val matcher = Matchers.hasDefectDefinitionWith<StrategyWithConversion>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(StrategyWithConversion.GOAL_ID)),
                CommonDefects.notNull()
            )
        )
        checkValidationFailed(strategy, container, matcher)
    }

    @Test
    fun `campaign without metrika counters`() {
        val strategy = strategy()
        val container = container(clientId, CampaignType.MCB, goalsMap = mapOf(strategy to setOf(metrikaGoal)))
        val matcher = Matchers.hasDefectDefinitionWith<StrategyWithConversion>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(StrategyWithConversion.GOAL_ID)),
                CommonDefects.objectNotFound()
            )
        )

        checkValidationFailed(strategy, container, matcher)
    }

    @Test
    fun `strategy with MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID`() {
        val strategy = strategy().withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
        val container = container(clientId, CampaignType.TEXT)
        checkSuccess(strategy, container)
    }

    @Test
    fun `strategy with mobile goal and RMP campaign`() {
        val strategy = strategy().withGoalId(mobileGoal.id)
        val container =
            container(clientId, CampaignType.MOBILE_CONTENT, goalsMap = mapOf(strategy to setOf(mobileGoal)))
        checkSuccess(strategy, container)
    }

    @Test
    fun `all goals are available due to copy strategy`() {
        val strategy = strategy()
        val container = container(clientId, CampaignType.TEXT, isCopy = true)
        checkSuccess(strategy, container)
    }

    @Test
    fun `all goals are available due to request from internal network`() {
        val strategy = strategy()
        val container = container(clientId, CampaignType.TEXT, requestFromInternalNetwork = true)
        checkSuccess(strategy, container)
    }

}
