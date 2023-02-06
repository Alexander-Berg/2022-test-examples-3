package ru.yandex.direct.core.entity.strategy.type.withavgcpv

import java.math.BigDecimal
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
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path

@RunWith(Parameterized::class)
class StrategyWithAvgCpvValidatorProviderNegativeTest(
    private val testName: String,
    private val strategy: StrategyWithAvgCpv,
    private val defect: Defect<*>
) {
    companion object {
        private val PATH = path(field(StrategyWithAvgCpv.AVG_CPV))

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "validation error when avg_cpv is null",
                AutobudgetAvgCpv(),
                CommonDefects.notNull(),
            ),
            arrayOf(
                "validation error when avg_cpv is less than min",
                AutobudgetAvgCpv()
                    .withAvgCpv(CurrencyRub.getInstance().minAvgCpv.subtract(BigDecimal.ONE)),
                NumberDefects.greaterThanOrEqualTo(CurrencyRub.getInstance().minAvgCpv),
            ),
            arrayOf(
                "validation error when avg_cpv is greater than max",
                AutobudgetAvgCpv()
                    .withAvgCpv(CurrencyRub.getInstance().maxAvgCpv.add(BigDecimal.ONE)),
                NumberDefects.lessThanOrEqualTo(CurrencyRub.getInstance().maxAvgCpv)
            ),
        )
    }

    @Test
    fun shouldValidate() {
        val container = StrategyAddOperationContainer(1, Mockito.mock(ClientId::class.java), 1L, 1L)
        container.currency = CurrencyRub.getInstance()
        val validator = StrategyWithAvgCpvValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        assertThat(
            result,
            hasDefectDefinitionWith(
                validationError(
                    PATH, defect
                )
            )
        )
    }
}
