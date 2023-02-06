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
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path

@RunWith(Parameterized::class)
class AutobudgetWeekBundleValidatorProviderNegativeTest(
    private val testName: String,
    private val strategy: AutobudgetWeekBundle,
    private val defect: Defect<*>
) {

    companion object {
        private val PATH = path(field(AutobudgetWeekBundle.LIMIT_CLICKS))

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "validation error when limit_clicks is null",
                AutobudgetWeekBundle(),
                CommonDefects.notNull(),
            ),
            arrayOf(
                "validation error when limit_clicks is less than min",
                AutobudgetWeekBundle()
                    .withLimitClicks(CurrencyRub.getInstance().minAutobudgetClicksBundle.toLong() - 1),
                NumberDefects.greaterThanOrEqualTo(CurrencyRub.getInstance().minAutobudgetClicksBundle.toLong()),
            ),
            arrayOf(
                "validation error when limit_clicks is greater than max",
                AutobudgetWeekBundle()
                    .withLimitClicks(CurrencyRub.getInstance().maxAutobudgetClicksBundle + 1),
                NumberDefects.lessThanOrEqualTo(CurrencyRub.getInstance().maxAutobudgetClicksBundle)
            ),
        )
    }

    @Test
    fun shouldValidate() {
        val container = StrategyAddOperationContainer(1, Mockito.mock(ClientId::class.java), 1L, 1L)
        container.currency = CurrencyRub.getInstance()
        val validator = AutobudgetWeekBundleValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        assertThat(
            result,
            hasDefectDefinitionWith(
                validationError(
                    PATH,
                    defect
                )
            )
        )
    }

}
