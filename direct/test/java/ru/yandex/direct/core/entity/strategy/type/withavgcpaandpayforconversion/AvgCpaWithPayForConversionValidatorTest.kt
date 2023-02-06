package ru.yandex.direct.core.entity.strategy.type.withavgcpaandpayforconversion

import java.math.BigDecimal
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class AvgCpaWithPayForConversionValidatorTest {

    private val currency = Currencies.getCurrency(CurrencyCode.RUB)
    private val enabledFeatures = setOf(FeatureName.INCREASED_CPA_LIMIT_FOR_PAY_FOR_CONVERSION)

    fun testData(): List<List<Any?>> = listOf(
        listOf(true, setOf<FeatureName>(), currency.autobudgetPayForConversionAvgCpaWarning, null),
        listOf(
            true,
            setOf<FeatureName>(),
            currency.autobudgetPayForConversionAvgCpaWarningIncreased,
            lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarning)
        ),
        listOf(true, enabledFeatures, currency.autobudgetPayForConversionAvgCpaWarning, null),
        listOf(true, enabledFeatures, currency.autobudgetPayForConversionAvgCpaWarningIncreased, null),
        listOf(
            true,
            enabledFeatures,
            currency.autobudgetPayForConversionAvgCpaWarningIncreased.plus(BigDecimal.TEN),
            lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarningIncreased)
        ),
        listOf(false, setOf<FeatureName>(), currency.autobudgetAvgCpaWarning, null),
        listOf(false, setOf<FeatureName>(), currency.autobudgetPayForConversionAvgCpaWarningIncreased, null),
        listOf(false, enabledFeatures, currency.autobudgetAvgCpaWarning, null),
        listOf(
            false,
            enabledFeatures,
            currency.autobudgetAvgCpaWarning.plus(BigDecimal.TEN),
            lessThanOrEqualTo(currency.autobudgetAvgCpaWarning)
        ),
        listOf(false, enabledFeatures, currency.autobudgetPayForConversionAvgCpaWarningIncreased, null),
        //null value
        listOf(true, setOf<FeatureName>(), null, notNull()),
        listOf(true, enabledFeatures, null, notNull()),
        listOf(false, setOf<FeatureName>(), null, notNull()),
        listOf(false, enabledFeatures, null, notNull()),
    )


    @Test
    @Parameters(method = "testData")
    @TestCaseName("isPayForConversionEnabled={0},features={1}, avgCpa={2}")
    fun `validation is correct`(
        isPayForConversionEnabled: Boolean,
        availableFeatures: Set<FeatureName>,
        avgCpa: BigDecimal?,
        defect: Defect<*>?
    ) {
        val validationContainer = AvgCpaWithPayForConversionValidator.Companion.ValidationContainer(
            isPayForConversionEnabled,
            availableFeatures,
            currency
        )

        val validator = AvgCpaWithPayForConversionValidator(validationContainer)

        val validationResult = validator.apply(avgCpa)

        val matcher: Matcher<ValidationResult<BigDecimal?, Defect<*>>> = if (defect != null) {
            Matchers.hasDefectDefinitionWith<BigDecimal?>(
                Matchers.validationError(
                    PathHelper.emptyPath(),
                    defect
                )
            )
        } else {
            Matchers.hasNoDefectsDefinitions()
        }

        validationResult.check(matcher)
    }
}
