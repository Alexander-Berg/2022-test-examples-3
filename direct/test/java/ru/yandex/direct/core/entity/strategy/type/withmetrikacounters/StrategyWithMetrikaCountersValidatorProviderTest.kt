package ru.yandex.direct.core.entity.strategy.type.withmetrikacounters

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.hamcrest.Matcher
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter.getRequestBasedMetrikaClientAdapterForStrategies
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerFilterStrategy
import ru.yandex.direct.core.validation.defects.Defects
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.metrika.client.MetrikaClient
import ru.yandex.direct.metrika.client.MetrikaClientException
import ru.yandex.direct.metrika.client.model.response.UserCountersResponse
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
class StrategyWithMetrikaCountersValidatorProviderTest {

    @Test
    fun `validate counters with unavailable metrika endpoint for getUsersCountersNum2`() {
        val container = StrategyAddOperationContainer(1, Mockito.mock(ClientId::class.java), 1L, 1L)
        val strategy = TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter()
            .withGoalId(12)
            .withCids(listOf(13))
            .withMetrikaCounters(listOf(15))
        val metrikaClient = Mockito.mock(MetrikaClient::class.java)
        whenever(metrikaClient.getUsersCountersNum2(any(), any())).thenThrow(MetrikaClientException())

        container.metrikaClientAdapter = getRequestBasedMetrikaClientAdapterForStrategies(
            metrikaClient, listOf(1), setOf(), listOf(strategy), false
        )

        val validationResult = StrategyWithMetrikaCountersValidatorProvider.createStrategyValidator(container)
            .apply(strategy)

        Assertions.assertThat(validationResult).`is`(
            Conditions.matchedBy(
                matcher(
                    PathHelper.emptyPath(),
                    Defects.metrikaReturnsResultWithErrors()
                )
            )
        )
    }

    @Test
    fun `validate counters with unavailable metrika endpoint for getUsersCountersNumExtended2`() {
        val container = StrategyAddOperationContainer(1, Mockito.mock(ClientId::class.java), 1L, 1L)
        val strategy = TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter()
            .withGoalId(12)
            .withCids(listOf(13))
            .withMetrikaCounters(listOf(15))
        val metrikaClient = Mockito.mock(MetrikaClient::class.java)
        whenever(metrikaClient.getUsersCountersNum2(any(), any())).thenReturn(
            UserCountersResponse()
                .withUsers(listOf())
                .withHasMoreCounters(false)
        )
        whenever(metrikaClient.getUsersCountersNumExtended2(any(), any())).thenThrow(MetrikaClientException())

        container.metrikaClientAdapter = getRequestBasedMetrikaClientAdapterForStrategies(
            metrikaClient, listOf(1), setOf(), listOf(strategy), false
        )

        val validationResult = StrategyWithMetrikaCountersValidatorProvider.createStrategyValidator(container)
            .apply(strategy)

        Assertions.assertThat(validationResult).`is`(
            Conditions.matchedBy(
                matcher(
                    PathHelper.emptyPath(),
                    Defects.metrikaReturnsResultWithErrors()
                )
            )
        )
    }

    @Test
    fun `there is now requests to metrika if migration oneshot`() {
        val container = StrategyAddOperationContainer(
            1,
            Mockito.mock(ClientId::class.java),
            1L,
            1L,
            StrategyOperationOptions(isCampaignToPackageStrategyOneshot = true)
        )
        val strategy = TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter()
            .withGoalId(12)
            .withCids(listOf(13))
            .withMetrikaCounters(listOf(15))
        val metrikaClient = Mockito.mock(MetrikaClient::class.java)

        container.metrikaClientAdapter = getRequestBasedMetrikaClientAdapterForStrategies(
            metrikaClient, listOf(1), setOf(), listOf(strategy), false
        )

        val validationResult = StrategyWithMetrikaCountersValidatorProvider.createStrategyValidator(container)
            .apply(strategy)

        Assertions.assertThat(validationResult).`is`(Conditions.matchedBy(Matchers.hasNoDefectsDefinitions<Any>()))
        Mockito.verifyNoInteractions(metrikaClient)
    }

    fun matcher(path: Path, defect: Defect<*>): Matcher<ValidationResult<Any, Defect<*>>> =
        Matchers.hasDefectDefinitionWith(
            Matchers.validationError(
                path,
                defect
            )
        )
}
