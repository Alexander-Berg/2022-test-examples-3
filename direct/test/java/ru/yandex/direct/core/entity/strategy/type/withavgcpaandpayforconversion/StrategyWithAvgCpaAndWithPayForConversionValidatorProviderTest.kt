package ru.yandex.direct.core.entity.strategy.type.withavgcpaandpayforconversion

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.math.BigDecimal
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.strategy.container.AbstractStrategyOperationContainer
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgCpaAndPayForConversion
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.Currency
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
class StrategyWithAvgCpaAndPayForConversionValidatorProviderTest {

    private val currency = Currencies.getCurrency(CurrencyCode.RUB)
    private val enabledFeatures = setOf(FeatureName.INCREASED_CPA_LIMIT_FOR_PAY_FOR_CONVERSION)

    fun testData(): List<List<Any?>> = listOf(
        listOf(true, setOf<FeatureName>(), currency.autobudgetPayForConversionAvgCpaWarning, null),
        listOf(
            true,
            setOf<FeatureName>(),
            currency.autobudgetPayForConversionAvgCpaWarningIncreased,
            NumberDefects.lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarning)
        ),
        listOf(true, enabledFeatures, currency.autobudgetPayForConversionAvgCpaWarning, null),
        listOf(true, enabledFeatures, currency.autobudgetPayForConversionAvgCpaWarningIncreased, null),
        listOf(
            true,
            enabledFeatures,
            currency.autobudgetPayForConversionAvgCpaWarningIncreased.plus(BigDecimal.TEN),
            NumberDefects.lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarningIncreased)
        ),
        listOf(false, setOf<FeatureName>(), currency.autobudgetAvgCpaWarning, null),
        listOf(false, setOf<FeatureName>(), currency.autobudgetPayForConversionAvgCpaWarningIncreased, null),
        listOf(false, enabledFeatures, currency.autobudgetAvgCpaWarning, null),
        listOf(
            false,
            enabledFeatures,
            currency.autobudgetAvgCpaWarning.plus(BigDecimal.TEN),
            NumberDefects.lessThanOrEqualTo(currency.autobudgetAvgCpaWarning)
        ),
        listOf(false, enabledFeatures, currency.autobudgetPayForConversionAvgCpaWarningIncreased, null),
        //null pay for conversion
        listOf(null, setOf<FeatureName>(), currency.autobudgetAvgCpaWarning, null),
        listOf(null, setOf<FeatureName>(), currency.autobudgetPayForConversionAvgCpaWarningIncreased, null),
        listOf(null, enabledFeatures, currency.autobudgetAvgCpaWarning, null),
        listOf(
            null,
            enabledFeatures,
            currency.autobudgetAvgCpaWarning.plus(BigDecimal.TEN),
            NumberDefects.lessThanOrEqualTo(currency.autobudgetAvgCpaWarning)
        ),
        listOf(null, enabledFeatures, currency.autobudgetPayForConversionAvgCpaWarningIncreased, null),
        //null value
        listOf(true, setOf<FeatureName>(), null, CommonDefects.notNull()),
        listOf(true, enabledFeatures, null, CommonDefects.notNull()),
        listOf(false, setOf<FeatureName>(), null, CommonDefects.notNull()),
        listOf(false, enabledFeatures, null, CommonDefects.notNull()),
        listOf(null, setOf<FeatureName>(), null, CommonDefects.notNull()),
        listOf(null, enabledFeatures, null, CommonDefects.notNull()),
    )


    @Test
    @Parameters(method = "testData")
    @TestCaseName("isPayForConversionEnabled={0},features={1}, avgCpa={2}")
    fun `validation is correct`(
        isPayForConversionEnabled: Boolean?,
        availableFeatures: Set<FeatureName>,
        avgCpa: BigDecimal?,
        defect: Defect<*>?
    ) {
        val container = mockContainer(availableFeatures, currency)
        val validator = StrategyWithAvgCpaAndPayForConversionValidatorProvider.createStrategyValidator(container)
        val strategy = autobudgetAvgCpa()
            .withIsPayForConversionEnabled(isPayForConversionEnabled)
            .withAvgCpa(avgCpa)

        val validationResult = validator.apply(strategy)

        val matcher: Matcher<ValidationResult<StrategyWithAvgCpaAndPayForConversion, Defect<*>>> = if (defect != null) {
            Matchers.hasDefectDefinitionWith<StrategyWithAvgCpaAndPayForConversion>(
                Matchers.validationError(
                    PathHelper.path(field(StrategyWithAvgCpaAndPayForConversion.AVG_CPA)),
                    defect
                )
            )
        } else {
            Matchers.hasNoDefectsDefinitions()
        }

        validationResult.check(matcher)
    }

    private fun mockContainer(
        availableFeatures: Set<FeatureName>,
        currency: Currency
    ): AbstractStrategyOperationContainer {
        val m = mock<AbstractStrategyOperationContainer>()
        whenever(m.availableFeatures).thenReturn(availableFeatures)
        whenever(m.currency).thenReturn(currency)

        return m
    }
}
