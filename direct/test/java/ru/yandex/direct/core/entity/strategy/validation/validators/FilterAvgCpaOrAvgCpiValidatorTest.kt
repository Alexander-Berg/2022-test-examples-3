package ru.yandex.direct.core.entity.strategy.validation.validators

import java.math.BigDecimal
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.weekBudgetLessThan
import ru.yandex.direct.core.entity.campaign.service.validation.type.bean.strategy.StrategyValidatorConstantsBuilder
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName.INCREASED_CPA_LIMIT_FOR_PAY_FOR_CONVERSION
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo
import ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper

@RunWith(JUnitParamsRunner::class)
internal class FilterAvgCpaOrAvgCpiValidatorTest {

    fun testData(): List<List<TestData>> = listOf(
        //increasedCpaLimitForPayForConversionEnabled=false, pay_for_conversion = false
        TestData(CampaignType.TEXT, false, null, false, null, listOf(notNull())),
        TestData(CampaignType.TEXT, false, null, false, 1, listOf()),
        TestData(CampaignType.TEXT, false, null, false, 0, listOf(greaterThanOrEqualTo(currency.minAutobudgetAvgCpa))),
        TestData(
            CampaignType.TEXT,
            false,
            null,
            false,
            31000,
            listOf(lessThanOrEqualTo(currency.autobudgetAvgCpaWarning))
        ),
        TestData(CampaignType.TEXT, false, 500, false, 500, listOf()),
        TestData(CampaignType.TEXT, false, 500, false, 1000, listOf(weekBudgetLessThan())),
        //increasedCpaLimitForPayForConversionEnabled=false, pay_for_conversion = true
        TestData(CampaignType.TEXT, false, null, true, null, listOf(notNull())),
        TestData(CampaignType.TEXT, false, null, true, 1, listOf()),
        TestData(CampaignType.TEXT, false, null, true, 0, listOf(greaterThanOrEqualTo(currency.minAutobudgetAvgCpa))),
        TestData(
            CampaignType.TEXT,
            false,
            null,
            true,
            6000,
            listOf(lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarning))
        ),
        TestData(CampaignType.TEXT, false, 500, true, 500, listOf()),
        TestData(CampaignType.TEXT, false, 500, true, 1000, listOf(weekBudgetLessThan())),
        //increasedCpaLimitForPayForConversionEnabled=true, pay_for_conversion = false
        TestData(CampaignType.TEXT, true, null, false, null, listOf(notNull())),
        TestData(CampaignType.TEXT, true, null, false, 1, listOf()),
        TestData(CampaignType.TEXT, true, null, false, 0, listOf(greaterThanOrEqualTo(currency.minAutobudgetAvgCpa))),
        TestData(
            CampaignType.TEXT,
            true,
            null,
            false,
            31000,
            listOf(lessThanOrEqualTo(currency.autobudgetAvgCpaWarning))
        ),
        TestData(CampaignType.TEXT, true, 500, false, 500, listOf()),
        TestData(CampaignType.TEXT, true, 500, false, 1000, listOf(weekBudgetLessThan())),
        //increasedCpaLimitForPayForConversionEnabled=true, pay_for_conversion = true
        TestData(CampaignType.TEXT, true, null, true, null, listOf(notNull())),
        TestData(CampaignType.TEXT, true, null, true, 1, listOf()),
        TestData(CampaignType.TEXT, true, null, true, 0, listOf(greaterThanOrEqualTo(currency.minAutobudgetAvgCpa))),
        TestData(
            CampaignType.TEXT,
            true,
            null,
            true,
            16000,
            listOf(lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarningIncreased))
        ),
        TestData(CampaignType.TEXT, true, 500, true, 500, listOf()),
        TestData(CampaignType.TEXT, true, 500, true, 1000, listOf(weekBudgetLessThan())),

        //CampaignType=Performance
        //increasedCpaLimitForPayForConversionEnabled=false, pay_for_conversion = false
        TestData(CampaignType.PERFORMANCE, false, null, false, null, listOf(notNull())),
        TestData(CampaignType.PERFORMANCE, false, null, false, 1, listOf()),
        TestData(
            CampaignType.PERFORMANCE,
            false,
            null,
            false,
            0,
            listOf(greaterThanOrEqualTo(currency.minCpcCpaPerformance))
        ),
        TestData(
            CampaignType.PERFORMANCE,
            false,
            null,
            false,
            31000,
            listOf(lessThanOrEqualTo(currency.autobudgetAvgCpaWarning))
        ),
        TestData(CampaignType.PERFORMANCE, false, 500, false, 500, listOf()),
        TestData(CampaignType.PERFORMANCE, false, 500, false, 1000, listOf(weekBudgetLessThan())),
        //increasedCpaLimitForPayForConversionEnabled=false, pay_for_conversion = true
        TestData(CampaignType.PERFORMANCE, false, null, true, null, listOf(notNull())),
        TestData(CampaignType.PERFORMANCE, false, null, true, 1, listOf()),
        TestData(
            CampaignType.PERFORMANCE,
            false,
            null,
            true,
            0,
            listOf(greaterThanOrEqualTo(currency.minCpcCpaPerformance))
        ),
        TestData(
            CampaignType.PERFORMANCE,
            false,
            null,
            true,
            6000,
            listOf(lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarning))
        ),
        TestData(CampaignType.PERFORMANCE, false, 500, true, 500, listOf()),
        TestData(CampaignType.PERFORMANCE, false, 500, true, 1000, listOf(weekBudgetLessThan())),
        //increasedCpaLimitForPayForConversionEnabled=true, pay_for_conversion = false
        TestData(CampaignType.PERFORMANCE, true, null, false, null, listOf(notNull())),
        TestData(CampaignType.PERFORMANCE, true, null, false, 1, listOf()),
        TestData(
            CampaignType.PERFORMANCE,
            true,
            null,
            false,
            0,
            listOf(greaterThanOrEqualTo(currency.minCpcCpaPerformance))
        ),
        TestData(
            CampaignType.PERFORMANCE,
            true,
            null,
            false,
            31000,
            listOf(lessThanOrEqualTo(currency.autobudgetAvgCpaWarning))
        ),
        TestData(CampaignType.PERFORMANCE, true, 500, false, 500, listOf()),
        TestData(CampaignType.PERFORMANCE, true, 500, false, 1000, listOf(weekBudgetLessThan())),
        //increasedCpaLimitForPayForConversionEnabled=true, pay_for_conversion = true
        TestData(CampaignType.PERFORMANCE, true, null, true, null, listOf(notNull())),
        TestData(CampaignType.PERFORMANCE, true, null, true, 1, listOf()),
        TestData(
            CampaignType.PERFORMANCE,
            true,
            null,
            true,
            0,
            listOf(greaterThanOrEqualTo(currency.minCpcCpaPerformance))
        ),
        TestData(
            CampaignType.PERFORMANCE,
            true,
            null,
            true,
            16000,
            listOf(lessThanOrEqualTo(currency.autobudgetPayForConversionAvgCpaWarningIncreased))
        ),
        TestData(CampaignType.PERFORMANCE, true, 500, true, 500, listOf()),
        TestData(CampaignType.PERFORMANCE, true, 500, true, 1000, listOf(weekBudgetLessThan())),
    ).map { listOf(it) }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun test(testData: TestData) {
        val validator = FilterAvgCpaOrAvgCpiValidator(testData.container)
        val vr = validator.apply(testData.filterAvgCpaOrCpiValue?.toBigDecimal())
        if (testData.expectedDefects.isEmpty()) {
            vr.check(Matchers.hasNoDefectsDefinitions())
        } else {
            testData.expectedDefects.forEach {
                vr.check(matcher(it))
            }
        }
    }

    private fun matcher(defect: Defect<*>) = Matchers.hasDefectDefinitionWith<BigDecimal?>(
        Matchers.validationError(
            PathHelper.emptyPath(),
            defect
        )
    )

    companion object {
        private val currency = Currencies.getCurrency(CurrencyCode.RUB)

        data class TestData(
            val campaignType: CampaignType,
            val increasedCpaLimitForPayForConversionEnabled: Boolean,
            val sum: Long?,
            val payForConversionEnabled: Boolean,
            val filterAvgCpaOrCpiValue: Long?,
            val expectedDefects: List<Defect<*>>
        ) {
            override fun toString(): String {
                return "TestData(campaignType=$campaignType, increasedCpaLimitForPayForConversionEnabled=$increasedCpaLimitForPayForConversionEnabled, sum=$sum, payForConversionEnabled=$payForConversionEnabled, avgCpaValue=$filterAvgCpaOrCpiValue)"
            }

            val container =
                FilterAvgCpaOrAvgCpiValidator.Companion.ValidationContainer(
                    currency,
                    payForConversionEnabled,
                    if (increasedCpaLimitForPayForConversionEnabled) setOf(INCREASED_CPA_LIMIT_FOR_PAY_FOR_CONVERSION) else emptySet(),
                    StrategyValidatorConstantsBuilder.build(campaignType, currency),
                    sum?.toBigDecimal()
                )
        }
    }

}
