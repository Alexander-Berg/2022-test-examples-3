package ru.yandex.direct.core.entity.strategy.validation.validators

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import java.math.BigDecimal
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.strategy.container.AbstractStrategyOperationContainer
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy
import ru.yandex.direct.core.entity.strategy.validation.AbstractStrategyValidatorProvider
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.ModelProperty
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

abstract class FilterAvgCpaOrCpiValidationBaseTest<T : BaseStrategy> {

    companion object {

        private val currency = Currencies.getCurrency(CurrencyCode.RUB)

        data class TestData(
            val campaignType: CampaignType,
            val increasedCpaLimitForPayForConversionEnabled: Boolean,
            val sum: Long?,
            val payForConversionEnabled: Boolean,
            val avgCpaOrCpiValue: Long?,
            val expectedDefects: List<Defect<*>>
        ) {
            override fun toString(): String {
                return "TestData(campaignType=$campaignType, increasedCpaLimitForPayForConversionEnabled=$increasedCpaLimitForPayForConversionEnabled, sum=$sum, payForConversionEnabled=$payForConversionEnabled, avgCpaValue=$avgCpaOrCpiValue)"
            }
        }

        @JvmStatic
        fun testData(): List<List<TestData>> =
            listOf(
                //increasedCpaLimitForPayForConversionEnabled=false, pay_for_conversion = false
                TestData(CampaignType.TEXT, false, null, false, null, listOf(CommonDefects.notNull())),
                TestData(CampaignType.TEXT, false, null, false, 1, listOf()),
                TestData(
                    CampaignType.TEXT,
                    false,
                    null,
                    false,
                    0,
                    listOf(NumberDefects.greaterThanOrEqualTo(currency.minAutobudgetAvgCpa))
                ),
                TestData(
                    CampaignType.TEXT,
                    false,
                    null,
                    false,
                    31000,
                    listOf(NumberDefects.lessThanOrEqualTo(currency.autobudgetAvgCpaWarning))
                ),
                TestData(CampaignType.TEXT, false, 500, false, 500, listOf()),
                TestData(CampaignType.TEXT, false, 500, false, 1000, listOf(StrategyDefects.weekBudgetLessThan())),
                //increasedCpaLimitForPayForConversionEnabled=false, pay_for_conversion = true
                TestData(CampaignType.TEXT, false, null, true, null, listOf(CommonDefects.notNull())),
                TestData(CampaignType.TEXT, false, null, true, 1, listOf()),
                TestData(
                    CampaignType.TEXT,
                    false,
                    null,
                    true,
                    0,
                    listOf(NumberDefects.greaterThanOrEqualTo(currency.minAutobudgetAvgCpa))
                ),
                TestData(
                    CampaignType.TEXT,
                    false,
                    null,
                    true,
                    6000,
                    listOf(NumberDefects.lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarning))
                ),
                TestData(CampaignType.TEXT, false, 500, true, 500, listOf()),
                TestData(CampaignType.TEXT, false, 500, true, 1000, listOf(StrategyDefects.weekBudgetLessThan())),
                //increasedCpaLimitForPayForConversionEnabled=true, pay_for_conversion = false
                TestData(CampaignType.TEXT, true, null, false, null, listOf(CommonDefects.notNull())),
                TestData(CampaignType.TEXT, true, null, false, 1, listOf()),
                TestData(
                    CampaignType.TEXT,
                    true,
                    null,
                    false,
                    0,
                    listOf(NumberDefects.greaterThanOrEqualTo(currency.minAutobudgetAvgCpa))
                ),
                TestData(
                    CampaignType.TEXT,
                    true,
                    null,
                    false,
                    31000,
                    listOf(NumberDefects.lessThanOrEqualTo(currency.autobudgetAvgCpaWarning))
                ),
                TestData(CampaignType.TEXT, true, 500, false, 500, listOf()),
                TestData(CampaignType.TEXT, true, 500, false, 1000, listOf(StrategyDefects.weekBudgetLessThan())),
                //increasedCpaLimitForPayForConversionEnabled=true, pay_for_conversion = true
                TestData(CampaignType.TEXT, true, null, true, null, listOf(CommonDefects.notNull())),
                TestData(CampaignType.TEXT, true, null, true, 1, listOf()),
                TestData(
                    CampaignType.TEXT,
                    true,
                    null,
                    true,
                    0,
                    listOf(NumberDefects.greaterThanOrEqualTo(currency.minAutobudgetAvgCpa))
                ),
                TestData(
                    CampaignType.TEXT,
                    true,
                    null,
                    true,
                    16000,
                    listOf(NumberDefects.lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarningIncreased))
                ),
                TestData(CampaignType.TEXT, true, 500, true, 500, listOf()),
                TestData(CampaignType.TEXT, true, 500, true, 1000, listOf(StrategyDefects.weekBudgetLessThan())),

                //CampaignType=Performance
                //increasedCpaLimitForPayForConversionEnabled=false, pay_for_conversion = false
                TestData(CampaignType.PERFORMANCE, false, null, false, null, listOf(CommonDefects.notNull())),
                TestData(CampaignType.PERFORMANCE, false, null, false, 1, listOf()),
                TestData(
                    CampaignType.PERFORMANCE,
                    false,
                    null,
                    false,
                    0,
                    listOf(NumberDefects.greaterThanOrEqualTo(currency.minCpcCpaPerformance))
                ),
                TestData(
                    CampaignType.PERFORMANCE,
                    false,
                    null,
                    false,
                    31000,
                    listOf(NumberDefects.lessThanOrEqualTo(currency.autobudgetAvgCpaWarning))
                ),
                TestData(CampaignType.PERFORMANCE, false, 500, false, 500, listOf()),
                TestData(
                    CampaignType.PERFORMANCE,
                    false,
                    500,
                    false,
                    1000,
                    listOf(StrategyDefects.weekBudgetLessThan())
                ),
                //increasedCpaLimitForPayForConversionEnabled=false, pay_for_conversion = true
                TestData(CampaignType.PERFORMANCE, false, null, true, null, listOf(CommonDefects.notNull())),
                TestData(CampaignType.PERFORMANCE, false, null, true, 1, listOf()),
                TestData(
                    CampaignType.PERFORMANCE,
                    false,
                    null,
                    true,
                    0,
                    listOf(NumberDefects.greaterThanOrEqualTo(currency.minCpcCpaPerformance))
                ),
                TestData(
                    CampaignType.PERFORMANCE,
                    false,
                    null,
                    true,
                    6000,
                    listOf(NumberDefects.lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarning))
                ),
                TestData(CampaignType.PERFORMANCE, false, 500, true, 500, listOf()),
                TestData(
                    CampaignType.PERFORMANCE,
                    false,
                    500,
                    true,
                    1000,
                    listOf(StrategyDefects.weekBudgetLessThan())
                ),
                //increasedCpaLimitForPayForConversionEnabled=true, pay_for_conversion = false
                TestData(CampaignType.PERFORMANCE, true, null, false, null, listOf(CommonDefects.notNull())),
                TestData(CampaignType.PERFORMANCE, true, null, false, 1, listOf()),
                TestData(
                    CampaignType.PERFORMANCE,
                    true,
                    null,
                    false,
                    0,
                    listOf(NumberDefects.greaterThanOrEqualTo(currency.minCpcCpaPerformance))
                ),
                TestData(
                    CampaignType.PERFORMANCE,
                    true,
                    null,
                    false,
                    31000,
                    listOf(NumberDefects.lessThanOrEqualTo(currency.autobudgetAvgCpaWarning))
                ),
                TestData(CampaignType.PERFORMANCE, true, 500, false, 500, listOf()),
                TestData(
                    CampaignType.PERFORMANCE,
                    true,
                    500,
                    false,
                    1000,
                    listOf(StrategyDefects.weekBudgetLessThan())
                ),
                //increasedCpaLimitForPayForConversionEnabled=true, pay_for_conversion = true
                TestData(CampaignType.PERFORMANCE, true, null, true, null, listOf(CommonDefects.notNull())),
                TestData(CampaignType.PERFORMANCE, true, null, true, 1, listOf()),
                TestData(
                    CampaignType.PERFORMANCE,
                    true,
                    null,
                    true,
                    0,
                    listOf(NumberDefects.greaterThanOrEqualTo(currency.minCpcCpaPerformance))
                ),
                TestData(
                    CampaignType.PERFORMANCE,
                    true,
                    null,
                    true,
                    16000,
                    listOf(NumberDefects.lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarningIncreased))
                ),
                TestData(CampaignType.PERFORMANCE, true, 500, true, 500, listOf()),
                TestData(CampaignType.PERFORMANCE, true, 500, true, 1000, listOf(StrategyDefects.weekBudgetLessThan())),
            ).map { listOf(it) }
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "testData")
    fun test(testData: TestData) {
        val strategy = strategy(testData)
        val container = createContainer(testData)
        val validator = validatorProvider().createStrategyValidator(container)

        val vr = validator.apply(strategy)
        if (testData.expectedDefects.isEmpty()) {
            vr.check(Matchers.hasNoDefectsDefinitions())
        } else {
            testData.expectedDefects.forEach {
                vr.check(matcher(it))
            }
        }
    }

    private fun matcher(defect: Defect<*>) = Matchers.hasDefectDefinitionWith<T>(
        Matchers.validationError(
            PathHelper.path(PathHelper.field(testedModelProperty())),
            defect
        )
    )


    private fun createContainer(testData: TestData): AbstractStrategyOperationContainer {
        val container = Mockito.mock(AbstractStrategyOperationContainer::class.java)
        val availableFeatures = if (testData.increasedCpaLimitForPayForConversionEnabled) {
            setOf(FeatureName.INCREASED_CPA_LIMIT_FOR_PAY_FOR_CONVERSION)
        } else {
            setOf()
        }
        whenever(container.campaignType(any())).thenReturn(testData.campaignType)
        whenever(container.availableFeatures).thenReturn(availableFeatures)
        whenever(container.currency).thenReturn(currency)
        return container
    }

    protected abstract fun strategy(testData: TestData): T

    protected abstract fun testedModelProperty(): ModelProperty<*, BigDecimal>

    protected abstract fun validatorProvider(): AbstractStrategyValidatorProvider<T>

}
