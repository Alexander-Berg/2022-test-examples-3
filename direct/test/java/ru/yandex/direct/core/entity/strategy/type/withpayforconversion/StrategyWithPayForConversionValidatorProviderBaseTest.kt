package ru.yandex.direct.core.entity.strategy.type.withpayforconversion

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matcher
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignWithStrategyValidationUtils
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.strategy.container.AbstractStrategyOperationContainer
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyWithPayForConversion
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

abstract class StrategyWithPayForConversionValidatorProviderBaseTest {
    protected abstract fun strategy(): StrategyWithPayForConversion

    @Test
    fun `pay for conversion not allowable for BY_ALL_GOALS_GOAL_ID`() {
        val strategy = strategy()
            .withGoalId(CampaignConstants.BY_ALL_GOALS_GOAL_ID)
            .withIsPayForConversionEnabled(true)
        val container = container(clientId, CampaignType.TEXT)
        val matcher = Matchers.hasDefectDefinitionWith<StrategyWithPayForConversion>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(StrategyWithPayForConversion.IS_PAY_FOR_CONVERSION_ENABLED)),
                StrategyDefects.payForConversionDoesNotAllowAllGoals()
            )
        )
        checkValidationFailed(strategy, container, matcher)
    }

    @Test
    fun `pay for conversion not allowable for MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID`() {
        val strategy = strategy()
            .withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
            .withIsPayForConversionEnabled(true)
        val container = container(clientId, CampaignType.TEXT)
        val matcher = Matchers.hasDefectDefinitionWith<StrategyWithPayForConversion>(
            Matchers.validationError(
                PathHelper.path(PathHelper.field(StrategyWithPayForConversion.IS_PAY_FOR_CONVERSION_ENABLED)),
                StrategyDefects.payForConversionDoesNotAllowAllGoals()
            )
        )
        checkValidationFailed(strategy, container, matcher)
    }

    @Test
    fun `MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID allowable if pay for conversion is disabled`() {
        val strategy = strategy()
            .withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
            .withIsPayForConversionEnabled(false)
        val container = container(clientId, CampaignType.TEXT)
        checkSuccess(strategy, container)
    }

    @Test
    fun `MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID allowable if pay for conversion is null`() {
        val strategy = strategy()
            .withGoalId(CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID)
            .withIsPayForConversionEnabled(null)
        val container = container(clientId, CampaignType.TEXT)
        checkSuccess(strategy, container)
    }

    @Test
    fun `pay for conversion supported in extended mode for specified campaign types`() {
        val types = CampaignWithStrategyValidationUtils.CAMPAIGN_TYPES_AVAILABLE_FOR_PAY_FOR_CONVERSION_EXTENDED_MODE
        types.forEach {
            val strategy = strategy().withIsPayForConversionEnabled(true)
            val container = container(clientId, it)
            checkSuccess(strategy, container)
        }
    }

    @Test
    fun `pay for conversion supported in extended mode for MOBILE_CONTENT`() {
        val strategy = strategy().withIsPayForConversionEnabled(true)
        val container = container(clientId, CampaignType.MOBILE_CONTENT)
        checkSuccess(strategy, container)
    }

    @Test
    fun `pay for conversion supported in extended mode if  CPA_PAY_FOR_CONVERSIONS_EXTENDED_MODE enabled`() {
        val types =
            CampaignWithStrategyValidationUtils.CAMPAIGN_TYPES_AVAILABLE_FOR_PAY_FOR_CONVERSION_EXTENDED_MODE - CampaignType.MOBILE_CONTENT
        types.forEach {
            val strategy = strategy().withIsPayForConversionEnabled(true)
            val container = container(clientId, it)
            checkSuccess(strategy, container)
        }
    }

    private val provider = StrategyWithPayForConversionValidatorProvider

    protected val metrikaGoal = TestFullGoals.defaultMetrikaGoals().first()
    protected val mobileGoal = Goal()
        .withId(TestFullGoals.generateGoalId(GoalType.MOBILE))
        .withIsMobileGoal(true) as Goal
    protected val clientId = 1L

    protected open fun container(
        clientId: Long,
        campaignType: CampaignType?,
        options: StrategyOperationOptions = StrategyOperationOptions(
            isValidatePayForConversionInAutobudgetAvgCpaPerFilter = true
        ),
        goalsMap: Map<BaseStrategy, Set<Goal>> = emptyMap(),
        availableFeatures: Set<FeatureName> = emptySet(),
        requestFromInternalNetwork: Boolean = false,
        isCopy: Boolean = false
    ): AbstractStrategyOperationContainer {
        val m = Mockito.mock(AbstractStrategyOperationContainer::class.java)
        whenever(m.availableFeatures).thenReturn(availableFeatures)
        whenever(m.strategyGoals(any())).doAnswer { goalsMap[it.getArgument(0)] ?: emptySet() }
        whenever(m.campaignType(any())).thenReturn(campaignType)
        whenever(m.clientId).thenReturn(ClientId.fromLong(clientId))
        whenever(m.options).thenReturn(options)
        return m
    }

    protected fun checkSuccess(strategy: StrategyWithPayForConversion, container: AbstractStrategyOperationContainer) {
        val validator = provider.createStrategyValidator(container)
        validator.apply(strategy).check(Matchers.hasNoDefectsDefinitions())
    }

    protected fun checkValidationFailed(
        strategy: StrategyWithPayForConversion,
        container: AbstractStrategyOperationContainer,
        matcher: Matcher<ValidationResult<StrategyWithPayForConversion, Defect<*>>>
    ) {
        val validator = provider.createStrategyValidator(container)
        validator.apply(strategy).check(matcher)
    }
}
