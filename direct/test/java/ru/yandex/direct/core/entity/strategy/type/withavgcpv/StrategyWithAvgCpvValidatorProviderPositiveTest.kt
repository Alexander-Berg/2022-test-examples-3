package ru.yandex.direct.core.entity.strategy.type.withavgcpv

import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpv
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpv
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions

@RunWith(Parameterized::class)
class StrategyWithAvgCpvValidatorProviderPositiveTest(
    private val testName: String,
    private val strategy: StrategyWithAvgCpv
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "validation ok when avg_cpv is equal to min",
                AutobudgetAvgCpv()
                    .withAvgCpv(CurrencyRub.getInstance().minAvgCpv),
            ),
            arrayOf(
                "validation ok when avg_cpv is equal to max",
                AutobudgetAvgCpv()
                    .withAvgCpv(CurrencyRub.getInstance().maxAvgCpv),
            ),
        )
    }

    @Test
    fun shouldValidate() {
        val container = StrategyAddOperationContainer(1, Mockito.mock(ClientId::class.java), 1L, 1L)
        container.currency = CurrencyRub.getInstance()
        val validator = StrategyWithAvgCpvValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        assertThat(result, hasNoDefectsDefinitions())
    }
}
