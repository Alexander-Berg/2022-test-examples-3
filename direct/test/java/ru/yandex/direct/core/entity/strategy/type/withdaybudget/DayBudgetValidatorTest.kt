package ru.yandex.direct.core.entity.strategy.type.withdaybudget

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.math.BigDecimal
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDayBudget
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_DAY_BUDGET_DAILY_CHANGE_COUNT
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.dayBudgetNotSupportedWithStrategy
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.dayBudgetOverridenByWallet
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.tooManyDayBudgetDailyChanges
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.NumberDefects.inInterval
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class DayBudgetValidatorTest {
    private val minDayBudget = currency.minDayBudget
    private val maxDayBudget = currency.maxDailyBudgetAmount

    fun testData(): List<List<TestData>> = listOf(
        TestData(null, campaignType = null),
        TestData(minDayBudget, campaignType = CampaignType.MCB, defect = dayBudgetNotSupportedWithStrategy()),
        TestData(
            minDayBudget,
            campaignType = CampaignType.TEXT,
            dailyBudgetChangeCount = MAX_DAY_BUDGET_DAILY_CHANGE_COUNT + 1,
            defect = tooManyDayBudgetDailyChanges()
        ),
        TestData(
            minDayBudget,
            campaignType = CampaignType.TEXT,
            linkedCampaigns = listOf(MAX_DAY_BUDGET_DAILY_CHANGE_COUNT + 1),
            defect = tooManyDayBudgetDailyChanges()
        ),
        TestData(
            minDayBudget.minus(BigDecimal.TEN),
            campaignType = CampaignType.TEXT,
            defect = inInterval(minDayBudget, maxDayBudget)
        ),
        TestData(
            maxDayBudget.plus(BigDecimal.TEN),
            campaignType = CampaignType.TEXT,
            defect = inInterval(minDayBudget, maxDayBudget)
        ),
        TestData(
            minDayBudget.plus(BigDecimal.TEN),
            campaignType = CampaignType.TEXT,
            walletBudget = minDayBudget,
            defect = dayBudgetOverridenByWallet()
        ),
        TestData(minDayBudget, campaignType = CampaignType.TEXT, walletBudget = minDayBudget),
        TestData(minDayBudget, campaignType = CampaignType.TEXT, walletBudget = minDayBudget.plus(BigDecimal.TEN)),
        TestData(
            minDayBudget,
            campaignType = CampaignType.TEXT,
            linkedCampaigns = listOf(2),
            walletBudget = minDayBudget.plus(BigDecimal.TEN)
        ),
        TestData(
            minDayBudget,
            campaignType = CampaignType.TEXT,
            dailyBudgetChangeCount = 2,
            walletBudget = minDayBudget.plus(BigDecimal.TEN)
        ),
    ).map { listOf(it) }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun test(testData: TestData) {
        val validator = DayBudgetValidator(testData.validationContainer())
        val validationResult = validator.apply(testData.dayBudget)
        validationResult.check(testData.matcher())
    }

    companion object {
        private val currency = Currencies.getCurrency(CurrencyCode.RUB)
        private fun mockCampaignWithDayBudget(dayBudget: BigDecimal): Campaign {
            val m = mock<Campaign>()
            whenever(m.dayBudget).thenReturn(dayBudget)
            return m
        }

        private fun mockDayBudgetChangeCount(changeCount: Int): CampaignWithDayBudget {
            val m = mock<CampaignWithDayBudget>()
            whenever(m.dayBudgetDailyChangeCount).thenReturn(changeCount)
            return m
        }

        data class TestData(
            val dayBudget: BigDecimal?,
            val campaignType: CampaignType? = null,
            val dailyBudgetChangeCount: Int = 0,
            val linkedCampaigns: List<Int> = emptyList(),
            val walletBudget: BigDecimal? = null,
            val defect: Defect<*>? = null
        ) {
            fun validationContainer() =
                DayBudgetValidator.Companion.ValidationContainer(
                    campaignType,
                    currency,
                    dailyBudgetChangeCount,
                    linkedCampaigns.map { mockDayBudgetChangeCount(it) },
                    walletBudget?.let { mockCampaignWithDayBudget(it) }
                )

            fun matcher(): Matcher<ValidationResult<BigDecimal?, Defect<*>>> = if (defect != null) {
                Matchers.hasDefectDefinitionWith<BigDecimal?>(
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
