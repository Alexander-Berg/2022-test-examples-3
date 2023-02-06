package ru.yandex.direct.core.entity.strategy.type.withdaybudget

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.dayBudgetShowModeOverridenByWallet
import ru.yandex.direct.core.entity.strategy.model.StrategyDayBudgetShowMode
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbschema.ppc.enums.CampaignsDayBudgetShowMode
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class DayBudgetShowModeValidatorTest {

    fun testData(): List<List<TestData>> = listOf(
        TestData(null),
        TestData(
            StrategyDayBudgetShowMode.STRETCHED,
            walletDayBudgetShowMode = CampaignsDayBudgetShowMode.default_,
            campaignType = CampaignType.TEXT,
            dayBudget = 10L,
            defect = dayBudgetShowModeOverridenByWallet()
        ),
        TestData(
            StrategyDayBudgetShowMode.DEFAULT_,
            walletDayBudgetShowMode = CampaignsDayBudgetShowMode.stretched,
            campaignType = CampaignType.TEXT,
            dayBudget = 10L,
            defect = dayBudgetShowModeOverridenByWallet()
        ),
        TestData(
            StrategyDayBudgetShowMode.STRETCHED,
            walletDayBudgetShowMode = CampaignsDayBudgetShowMode.stretched,
            campaignType = CampaignType.TEXT,
            dayBudget = 10L
        ),
        TestData(
            StrategyDayBudgetShowMode.DEFAULT_,
            walletDayBudgetShowMode = CampaignsDayBudgetShowMode.default_,
            campaignType = CampaignType.TEXT,
            dayBudget = 10L
        ),
    ).map { listOf(it) }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun test(testData: TestData) {
        val validator = DayBudgetShowModeValidator(testData.validationContainer())
        val validationResult = validator.apply(testData.showMode)
        validationResult.check(testData.matcher())
    }

    companion object {
        private val currency = Currencies.getCurrency(CurrencyCode.RUB)
        private fun mockDayBudgetShowMode(showMode: CampaignsDayBudgetShowMode): Campaign {
            val m = mock<Campaign>()
            whenever(m.dayBudgetShowMode).thenReturn(showMode)
            whenever(m.dayBudget).thenReturn(currency.minDayBudget)
            return m
        }

        data class TestData(
            val showMode: StrategyDayBudgetShowMode?,
            val campaignType: CampaignType? = null,
            val dayBudget: Long? = null,
            val walletDayBudgetShowMode: CampaignsDayBudgetShowMode? = null,
            val defect: Defect<*>? = null
        ) {
            fun validationContainer() =
                DayBudgetShowModeValidator.Companion.ValidationContainer(
                    walletDayBudgetShowMode?.let { mockDayBudgetShowMode(it) },
                    dayBudget?.toBigDecimal(),
                    campaignType
                )

            fun matcher(): Matcher<ValidationResult<StrategyDayBudgetShowMode?, Defect<*>>> = if (defect != null) {
                Matchers.hasDefectDefinitionWith<StrategyDayBudgetShowMode?>(
                    Matchers.validationError(
                        PathHelper.emptyPath(),
                        defect
                    )
                )
            } else {
                Matchers.hasNoDefectsDefinitions()
            }

        }
    }

}
