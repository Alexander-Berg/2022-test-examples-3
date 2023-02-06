package ru.yandex.direct.core.entity.strategy.type.withcustomperiodbudgetandcustombid

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDate.now
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.service.CampaignStrategyUtils
import ru.yandex.direct.core.validation.defects.MoneyDefects
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Money
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
class BudgetValidatorTest {

    fun testsData(): List<Array<Any>> = listOf(
        arrayOf("valid budget: between minimal and maximal", TestData(budget = DEFAULT_MIN_BUDGET)),
        arrayOf("invalid budget: null", TestData(budget = null, defect = CommonDefects.notNull())),
        arrayOf(
            "invalid budget: less than minimal",
            TestData(
                budget = DEFAULT_MIN_BUDGET.subtract(BigDecimal.ONE),
                defect = MoneyDefects.invalidValueCpmNotLessThan(
                    Money.valueOf(DEFAULT_MIN_BUDGET, CurrencyCode.RUB)
                )
            )
        ),
        arrayOf(
            "invalid budget: greater than maximal",
            TestData(
                budget = DEFAULT_MAX_BUDGET.add(BigDecimal.ONE),
                defect = MoneyDefects.invalidValueCpmNotGreaterThan(
                    Money.valueOf(DEFAULT_MAX_BUDGET, CurrencyCode.RUB)
                )
            )
        ),
    )

    @Test
    @Parameters(method = "testsData")
    @TestCaseName("{0}")
    fun test(testCaseName: String, testData: TestData) {
        val validator = BudgetValidator(testData.currencyCode, testData.start, testData.finish)
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
        private val NOW = now()
        private val DEFAULT_START = NOW
        private val DEFAULT_FINISH = NOW.plusDays(7)
        private val DEFAULT_CURRENCY = CurrencyCode.RUB
        private val DEFAULT_MIN_BUDGET =
            CampaignStrategyUtils.calculateMinimalAvailableBudgetForCpmRestartingStrategyWithCustomPeriod(
                DEFAULT_START,
                DEFAULT_FINISH,
                DEFAULT_CURRENCY
            )
        private val DEFAULT_MAX_BUDGET =
            CampaignStrategyUtils.calculateMaximumAvailableBudgetForCpmRestartingStrategyWithCustomPeriod(
                DEFAULT_START,
                DEFAULT_FINISH,
                DEFAULT_CURRENCY
            )

        data class TestData(
            val budget: BigDecimal?,
            val start: LocalDate = DEFAULT_START,
            val finish: LocalDate = DEFAULT_FINISH,
            val currencyCode: CurrencyCode = CurrencyCode.RUB,
            val defect: Defect<*>? = null
        )
    }
}
