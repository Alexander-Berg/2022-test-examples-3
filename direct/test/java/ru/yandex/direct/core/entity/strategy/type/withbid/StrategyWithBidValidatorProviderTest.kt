package ru.yandex.direct.core.entity.strategy.type.withbid

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerCamp
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerFilter
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerFilter
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetRoi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekSum
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyWithBid
import ru.yandex.direct.core.entity.strategy.model.StrategyWithPayForConversion
import ru.yandex.direct.core.entity.strategy.model.StrategyWithWeeklyBudget
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path
import java.math.BigDecimal
import java.util.IdentityHashMap

@RunWith(Parameterized::class)
class StrategyWithBidValidatorProviderTest(
    private val testName: String,
    private val strategy: StrategyWithBid,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "AutobudgetAvgCpa",
                AutobudgetAvgCpa().withType(StrategyName.AUTOBUDGET_AVG_CPA)
            ),
            arrayOf(
                "AutobudgetAvgCpaPerCamp",
                AutobudgetAvgCpaPerCamp().withType(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP)
            ),
            arrayOf(
                "AutobudgetAvgCpaPerFilter",
                AutobudgetAvgCpaPerFilter().withType(StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER)
            ),
            arrayOf(
                "AutobudgetAvgCpaPerCamp",
                AutobudgetAvgCpaPerCamp().withType(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP)
            ),
            arrayOf(
                "AutobudgetAvgCpcPerFilter",
                AutobudgetAvgCpcPerFilter().withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER)
            ),
            arrayOf(
                "AutobudgetAvgCpi",
                AutobudgetAvgCpi().withType(StrategyName.AUTOBUDGET_AVG_CPI)
            ),
            arrayOf(
                "AutobudgetRoi",
                AutobudgetRoi().withType(StrategyName.AUTOBUDGET_ROI)
            ),
            arrayOf(
                "AutobudgetWeekBundle",
                AutobudgetWeekBundle().withType(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
            ),
            arrayOf(
                "AutobudgetWeekSum",
                AutobudgetWeekSum().withType(StrategyName.AUTOBUDGET)
            ),
        )
    }

    private val currency = CurrencyRub.getInstance()

    @Before
    fun setUp() {
        strategy.bid = null
    }

    @Test
    fun shouldValidateOk_whenMinValue() {
        val container = createContainer()
        strategy.bid = currency.minPrice
        validateAndCheckOk(container)
    }

    @Test
    fun shouldValidateOk_whenMinValueForPerformanceCampaign() {
        val container = createContainer()
        container.typedCampaignsMap =
            mapOf(strategy to listOf(SmartCampaign().withType(CampaignType.PERFORMANCE))).toMap(IdentityHashMap())
        strategy.bid = currency.minCpcCpaPerformance
        validateAndCheckOk(container)
    }

    @Test
    fun shouldValidateOk_whenMaxValue() {
        val container = createContainer()
        strategy.bid = when (strategy.type) {
            StrategyName.AUTOBUDGET_WEEK_BUNDLE -> currency.maxPrice
            else -> currency.maxAutobudgetBid
        }
        validateAndCheckOk(container)
    }

    @Test
    fun shouldValidateOk_whenSumExists() {
        if (strategy is StrategyWithWeeklyBudget) {
            val container = createContainer()
            strategy.bid = currency.minPrice
            strategy.sum = currency.minPrice.add(BigDecimal.TEN)
            validateAndCheckOk(container)
        }
    }

    @Test
    fun shouldValidateOk_whenValueIsNull() {
        val container = createContainer()
        validateAndCheckOk(container)
    }

    @Test
    fun shouldValidationFailed_whenValueIsLessThanMin() {
        val container = createContainer()
        strategy.bid = currency.minPrice.subtract(BigDecimal.ONE)
        validateAndCheckError(container, NumberDefects.greaterThanOrEqualTo(currency.minPrice))
    }

    @Test
    fun shouldValidationFailed_whenValueIsLessThanMin_performanceCampaign() {
        val container = createContainer()
        val campaign = SmartCampaign()
        campaign.type = CampaignType.PERFORMANCE
        campaign.clientId = container.clientId.asLong()
        container.typedCampaignsMap = mapOf(strategy to listOf(campaign)).toMap(IdentityHashMap())
        strategy.bid = currency.minCpcCpaPerformance.subtract(BigDecimal.ONE)
        validateAndCheckError(container, NumberDefects.greaterThanOrEqualTo(currency.minCpcCpaPerformance))
    }

    @Test
    fun shouldValidationFailed_whenValueIsGreaterThanMax() {
        val container = createContainer()
        strategy.bid = when (strategy.type) {
            StrategyName.AUTOBUDGET_WEEK_BUNDLE -> currency.maxPrice.add(BigDecimal.ONE)
            else -> currency.maxAutobudgetBid.add(BigDecimal.ONE)
        }
        validateAndCheckError(container, NumberDefects.lessThanOrEqualTo(strategy.bid.subtract(BigDecimal.ONE)))
    }

    @Test
    fun shouldValidationFailed_whenSumIsLessThanBid() {
        if (strategy is StrategyWithWeeklyBudget) {
            val container = createContainer()
            strategy.bid = currency.minPrice.add(BigDecimal.TEN)
            strategy.sum = currency.minPrice
            validateAndCheckError(container, StrategyDefects.weekBudgetLessThan())
        }
    }

    @Test
    fun `should be null if payForConversion enabled`() {
        (strategy as? StrategyWithPayForConversion)?.let {
            val container = createContainer()
            strategy.isPayForConversionEnabled = true
            strategy.bid = currency.minPrice
            when (strategy) {
                is AutobudgetAvgCpi, is AutobudgetAvgCpa, is AutobudgetAvgCpaPerCamp ->
                    validateAndCheckError(container, CommonDefects.isNull())
                else ->
                    validateAndCheckOk(container)
            }
        }
    }

    private fun createContainer(): StrategyAddOperationContainer {
        val container = StrategyAddOperationContainer(1, ClientId.fromLong(3L), 1L, 1L)
        container.currency = currency
        return container
    }

    private fun validateAndCheckOk(container: StrategyAddOperationContainer) {
        val validator = StrategyWithBidValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        Assert.assertThat(result, Matchers.hasNoDefectsDefinitions())
    }

    private fun validateAndCheckError(container: StrategyAddOperationContainer, defect: Defect<*>) {
        val validator = StrategyWithBidValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    path(field(StrategyWithBid.BID)),
                    defect
                )
            )
        )
    }
}
