package ru.yandex.direct.core.entity.strategy.type.withavgcpaandweeklybudget

import java.math.BigDecimal
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.weekBudgetLessThan
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class AvgCpaWithWeeklyBudgetValidatorTest {

    private val currency = Currencies.getCurrency(CurrencyCode.RUB)

    fun testData(): List<List<Any?>> = listOf(
        listOf(null, currency.minAutobudgetAvgCpa, null),
        listOf(currency.minAutobudgetAvgCpa, currency.minAutobudgetAvgCpa, null),
        listOf(currency.minAutobudgetAvgCpa, currency.minAutobudgetAvgCpa.plus(BigDecimal.TEN), weekBudgetLessThan()),
        listOf(
            currency.minAutobudgetAvgCpa.plus(BigDecimal.TEN),
            currency.minAutobudgetAvgCpa.plus(BigDecimal.ONE),
            null
        ),
        listOf(null, null, notNull())
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("sum={0}, avgCpa={1}")
    fun `validation is correct`(sum: BigDecimal?, avgCpa: BigDecimal?, defect: Defect<*>?) {
        val validationContainer = AvgCpaWithWeeklyBudgetValidator.Companion.ValidationContainer(sum)

        val validator = AvgCpaWithWeeklyBudgetValidator(validationContainer)

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
