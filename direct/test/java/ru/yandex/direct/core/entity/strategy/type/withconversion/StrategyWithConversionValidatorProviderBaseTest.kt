package ru.yandex.direct.core.entity.strategy.type.withconversion

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matcher
import org.mockito.Mockito
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.strategy.container.AbstractStrategyOperationContainer
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyWithConversion
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.ValidationResult

abstract class StrategyWithConversionValidatorProviderBaseTest {
    private val provider = StrategyWithConversionValidatorProvider

    protected val metrikaGoal = TestFullGoals.defaultMetrikaGoals().first()
    protected val mobileGoal = Goal()
        .withId(TestFullGoals.generateGoalId(GoalType.MOBILE))
        .withIsMobileGoal(true) as Goal
    protected val clientId = 1L

    protected fun container(
        clientId: Long,
        campaignType: CampaignType?,
        goalsMap: Map<BaseStrategy, Set<Goal>> = emptyMap(),
        availableFeatures: Set<FeatureName> = emptySet(),
        requestFromInternalNetwork: Boolean = false,
        isCopy: Boolean = false
    ): AbstractStrategyOperationContainer {
        val m = Mockito.mock(AbstractStrategyOperationContainer::class.java)
        whenever(m.availableFeatures).thenReturn(availableFeatures)
        whenever(m.strategyGoals(any())).doAnswer { goalsMap[it.getArgument(0)] ?: emptySet() }
        whenever(m.campaignType(any())).thenReturn(campaignType)
        whenever(m.options).thenReturn(
            StrategyOperationOptions(
                isCopy = isCopy,
                isRequestFromInternalNetwork = requestFromInternalNetwork
            )
        )
        whenever(m.clientId).thenReturn(ClientId.fromLong(clientId))
        return m
    }

    protected fun checkSuccess(strategy: StrategyWithConversion, container: AbstractStrategyOperationContainer) {
        val validator = provider.createStrategyValidator(container)
        validator.apply(strategy).check(Matchers.hasNoDefectsDefinitions())
    }

    protected fun checkValidationFailed(
        strategy: StrategyWithConversion,
        container: AbstractStrategyOperationContainer,
        matcher: Matcher<ValidationResult<StrategyWithConversion, Defect<*>>>
    ) {
        val validator = provider.createStrategyValidator(container)
        validator.apply(strategy).check(matcher)
    }
}
