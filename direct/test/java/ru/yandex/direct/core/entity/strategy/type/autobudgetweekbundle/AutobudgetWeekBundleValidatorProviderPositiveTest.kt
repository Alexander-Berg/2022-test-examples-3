package ru.yandex.direct.core.entity.strategy.type.autobudgetweekbundle

import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions

@RunWith(Parameterized::class)
class AutobudgetWeekBundleValidatorProviderPositiveTest(
    private val testName: String,
    private val strategy: AutobudgetWeekBundle,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "validation ok when limit_clicks is equal to min",
                AutobudgetWeekBundle()
                    .withLimitClicks(CurrencyRub.getInstance().minAutobudgetClicksBundle.toLong()),
            ),
            arrayOf(
                "validation error when limit_clicks is equal to max",
                AutobudgetWeekBundle()
                    .withLimitClicks(CurrencyRub.getInstance().maxAutobudgetClicksBundle),
            ),
        )
    }

    @Test
    fun shouldValidate() {
        val container = StrategyAddOperationContainer(1, Mockito.mock(ClientId::class.java), 1L, 1L)
        container.currency = CurrencyRub.getInstance()
        val validator = AutobudgetWeekBundleValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        assertThat(result, hasNoDefectsDefinitions())
    }
}
