package ru.yandex.direct.core.entity.strategy.type.withweeklybudget

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.mock
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgClick
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerCamp
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerFilter
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerCamp
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerFilter
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpv
import ru.yandex.direct.core.entity.strategy.model.AutobudgetCrr
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxImpressions
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReach
import ru.yandex.direct.core.entity.strategy.model.AutobudgetRoi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekSum
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyWithWeeklyBudget
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import java.math.BigDecimal

@RunWith(Parameterized::class)
class StrategyWithWeeklyBudgetValidatorProviderTest(
    private val testName: String,
    private val strategy: StrategyWithWeeklyBudget
) {

    companion object {
        private val typesRequired = setOf(
            StrategyName.AUTOBUDGET,
            StrategyName.AUTOBUDGET_MAX_REACH,
            StrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
            StrategyName.AUTOBUDGET_AVG_CPV
        )

        private val CPM_STRATEGIES = setOf(
            StrategyName.AUTOBUDGET_MAX_REACH,
            StrategyName.AUTOBUDGET_MAX_IMPRESSIONS,
            StrategyName.AUTOBUDGET_AVG_CPV
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "AutobudgetAvgClick",
                AutobudgetAvgClick().withType(StrategyName.AUTOBUDGET_AVG_CLICK),
            ),
            arrayOf(
                "AutobudgetAvgCpa",
                AutobudgetAvgCpa().withType(StrategyName.AUTOBUDGET_AVG_CPA),
            ),
            arrayOf(
                "AutobudgetAvgCpaPerCamp",
                AutobudgetAvgCpaPerCamp().withType(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP),
            ),
            arrayOf(
                "AutobudgetAvgCpaPerFilter",
                AutobudgetAvgCpaPerFilter().withType(StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER),
            ),
            arrayOf(
                "AutobudgetAvgCpcPerCamp",
                AutobudgetAvgCpcPerCamp().withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP),
            ),
            arrayOf(
                "AutobudgetAvgCpcPerFilter",
                AutobudgetAvgCpcPerFilter().withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER),
            ),
            arrayOf(
                "AutobudgetAvgCpi",
                AutobudgetAvgCpi().withType(StrategyName.AUTOBUDGET_AVG_CPI),
            ),
            arrayOf(
                "AutobudgetAvgCpv",
                AutobudgetAvgCpv().withType(StrategyName.AUTOBUDGET_AVG_CPV),
            ),
            arrayOf(
                "AutobudgetCrr",
                AutobudgetCrr().withType(StrategyName.AUTOBUDGET_CRR),
            ),
            arrayOf(
                "AutobudgetMaxImpressions",
                AutobudgetMaxImpressions().withType(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS),
            ),
            arrayOf(
                "AutobudgetMaxReach",
                AutobudgetMaxReach().withType(StrategyName.AUTOBUDGET_MAX_REACH),
            ),
            arrayOf(
                "AutobudgetRoi",
                AutobudgetRoi().withType(StrategyName.AUTOBUDGET_ROI),
            ),
            arrayOf(
                "AutobudgetWeekSum",
                AutobudgetWeekSum().withType(StrategyName.AUTOBUDGET),
            ),
        )
    }

    private val currency = CurrencyRub.getInstance()

    @Before
    fun setUp() {
        strategy.sum = null
    }

    @Test
    fun shouldValidateOk_whenMinValue() {
        strategy.sum = when (strategy.type) {
            in CPM_STRATEGIES -> currency.minDailyBudgetForPeriod.multiply(BigDecimal(7))
            else -> currency.minAutobudget
        }
        validateAndCheckOk()
    }

    @Test
    fun shouldValidateOk_whenMaxValue() {
        strategy.sum = currency.maxAutobudget
        validateAndCheckOk()
    }

    @Test
    fun shouldSkipCheckIfValueIsNullAndNotRequired() {
        if (!typesRequired.contains(strategy.type)) validateAndCheckOk()
    }

    @Test
    fun shouldValidationFailed_whenValueIsLessThanMin() {
        strategy.sum = when (strategy.type) {
            in CPM_STRATEGIES -> currency.minDailyBudgetForPeriod.multiply(BigDecimal(7)).subtract(BigDecimal.ONE)
            else -> currency.minAutobudget.subtract(BigDecimal.ONE)
        }
        validateAndCheckError(NumberDefects.greaterThanOrEqualTo(strategy.sum.add(BigDecimal.ONE)))
    }

    @Test
    fun shouldValidationFailed_whenValueIsGreaterThanMax() {
        strategy.sum = currency.maxAutobudget.add(BigDecimal.TEN)
        validateAndCheckError(NumberDefects.lessThanOrEqualTo(currency.maxAutobudget))
    }

    @Test
    fun shouldValidationFailed_whenValueIsNullAndRequired() {
        if (typesRequired.contains(strategy.type)) validateAndCheckError(CommonDefects.notNull())
    }

    private fun validateAndCheckOk() {
        val container = StrategyAddOperationContainer(1, mock(ClientId::class.java), 1L, 1L)
        container.currency = CurrencyRub.getInstance()
        val validator = StrategyWithWeeklyBudgetValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        Assert.assertThat(result, Matchers.hasNoDefectsDefinitions())
    }

    private fun validateAndCheckError(defect: Defect<*>) {
        val container = StrategyAddOperationContainer(1, mock(ClientId::class.java), 1L, 1L)
        container.currency = CurrencyRub.getInstance()
        val validator = StrategyWithWeeklyBudgetValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PathHelper.path(PathHelper.field(StrategyWithWeeklyBudget.SUM)),
                    defect
                )
            )
        )
    }


}
