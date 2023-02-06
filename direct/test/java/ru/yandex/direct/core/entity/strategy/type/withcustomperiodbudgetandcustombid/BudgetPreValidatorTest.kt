package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import java.math.BigDecimal
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.validation.defects.MoneyDefects
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Money
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
class BudgetPreValidatorTest {

    fun testsData(): List<Array<Any>> = listOf(
        arrayOf(
            "invalid budget: less than minimal",
            TestData(
                budget = MINIMAL_BUDGET_FOR_STRATEGY_CAMPAIGNS_TEST_VALUE.subtract(BigDecimal.ONE),
                minimalBudgetForStrategyCampaigns = MINIMAL_BUDGET_FOR_STRATEGY_CAMPAIGNS_TEST_VALUE,
                defect = MoneyDefects.invalidValueCpmNotLessThan(
                    Money.valueOf(MINIMAL_BUDGET_FOR_STRATEGY_CAMPAIGNS_TEST_VALUE, CurrencyCode.RUB)
                )
            )
        ),
        arrayOf(
            "valid budget: greater than or equal to minimal",
            TestData(
                budget = MINIMAL_BUDGET_FOR_STRATEGY_CAMPAIGNS_TEST_VALUE.add(BigDecimal.ONE),
                minimalBudgetForStrategyCampaigns = MINIMAL_BUDGET_FOR_STRATEGY_CAMPAIGNS_TEST_VALUE
            )
        ),
        arrayOf("valid budget: null", TestData(budget = null))
    )

    @Test
    @Parameters(method = "testsData")
    @TestCaseName("{0}")
    fun test(testCaseName: String, testData: TestData) {
        val validator = BudgetPreValidator(CurrencyCode.RUB, testData.minimalBudgetForStrategyCampaigns)
        val validationResult = validator.apply(testData.budget)

        testData.defect?.let {
            Assert.assertThat(
                validationResult,
                Matchers.hasDefectDefinitionWith(
                    Matchers.validationError(PathHelper.emptyPath(), it)
                )
            )
        } ?: Assert.assertThat(validationResult, Matchers.hasNoDefectsDefinitions())
    }

    companion object {
        private val MINIMAL_BUDGET_FOR_STRATEGY_CAMPAIGNS_TEST_VALUE = BigDecimal(200)

        data class TestData(
            val budget: BigDecimal?,
            val minimalBudgetForStrategyCampaigns: BigDecimal = BigDecimal(0),
            val defect: Defect<*>? = null
        )
    }
}
