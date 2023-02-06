package ru.yandex.direct.core.entity.strategy.type.withdaybudget

import com.nhaarman.mockitokotlin2.anyOrNull
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
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_DAY_BUDGET_DAILY_CHANGE_COUNT
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.dayBudgetOverridenByWallet
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.dayBudgetShowModeOverridenByWallet
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.tooManyDayBudgetDailyChanges
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.model.StrategyDayBudgetShowMode
import ru.yandex.direct.core.entity.strategy.model.StrategyWithDayBudget
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbschema.ppc.enums.CampaignsDayBudgetShowMode
import ru.yandex.direct.model.ModelProperty
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.NumberDefects.inInterval
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult
import java.math.BigDecimal

@RunWith(JUnitParamsRunner::class)
internal class StrategyWithDayBudgetValidatorProviderTest {
    private val currency = Currencies.getCurrency(CurrencyCode.RUB)
    private val walletWithDefaultMinDayBudget =
        mockCampaignWithDayBudget(currency.minDayBudget, CampaignsDayBudgetShowMode.default_)
    private val campaignWithDayBudgetChangeCountLimitExceeded =
        mockDayBudgetChangeCount(MAX_DAY_BUDGET_DAILY_CHANGE_COUNT + 1)
    private val campaignWithDayBudgetChangeCount = mockDayBudgetChangeCount(MAX_DAY_BUDGET_DAILY_CHANGE_COUNT)

    fun testData(): List<List<TestData>> = listOf(
        TestData(
            currency.minDayBudget,
            StrategyDayBudgetShowMode.DEFAULT_,
            CampaignType.TEXT,
            true,
            1,
            emptyList(),
            walletWithDefaultMinDayBudget
        ),
        TestData(
            currency.minDayBudget,
            StrategyDayBudgetShowMode.DEFAULT_,
            CampaignType.TEXT,
            true,
            1,
            listOf(campaignWithDayBudgetChangeCount),
            walletWithDefaultMinDayBudget
        ),
        TestData(
            currency.minDayBudget.plus(BigDecimal.TEN),
            StrategyDayBudgetShowMode.DEFAULT_,
            CampaignType.TEXT,
            true,
            1,
            emptyList(),
            walletWithDefaultMinDayBudget,
            DefectWithField(dayBudgetOverridenByWallet(), StrategyWithDayBudget.DAY_BUDGET)
        ),
        TestData(
            currency.minDayBudget,
            StrategyDayBudgetShowMode.STRETCHED,
            CampaignType.TEXT,
            true,
            1,
            emptyList(),
            walletWithDefaultMinDayBudget,
            DefectWithField(dayBudgetShowModeOverridenByWallet(), StrategyWithDayBudget.DAY_BUDGET_SHOW_MODE)
        ),
        TestData(
            currency.minDayBudget,
            StrategyDayBudgetShowMode.DEFAULT_,
            CampaignType.TEXT,
            true,
            4,
            emptyList(),
            walletWithDefaultMinDayBudget,
            DefectWithField(tooManyDayBudgetDailyChanges(), StrategyWithDayBudget.DAY_BUDGET)
        ),
        TestData(
            currency.minDayBudget,
            StrategyDayBudgetShowMode.DEFAULT_,
            CampaignType.TEXT,
            true,
            4,
            listOf(campaignWithDayBudgetChangeCountLimitExceeded),
            walletWithDefaultMinDayBudget,
            DefectWithField(tooManyDayBudgetDailyChanges(), StrategyWithDayBudget.DAY_BUDGET)
        ),
        TestData(
            currency.minDayBudget.minus(BigDecimal.ONE),
            StrategyDayBudgetShowMode.DEFAULT_,
            CampaignType.TEXT,
            true,
            1,
            emptyList(),
            walletWithDefaultMinDayBudget,
            DefectWithField(
                inInterval(currency.minDayBudget, currency.maxDailyBudgetAmount),
                StrategyWithDayBudget.DAY_BUDGET
            )
        ),
        TestData(
            currency.maxDailyBudgetAmount.plus(BigDecimal.ONE),
            StrategyDayBudgetShowMode.DEFAULT_,
            CampaignType.TEXT,
            true,
            1,
            emptyList(),
            walletWithDefaultMinDayBudget,
            DefectWithField(
                inInterval(currency.minDayBudget, currency.maxDailyBudgetAmount),
                StrategyWithDayBudget.DAY_BUDGET
            )
        )
    ).map { listOf(it) }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun `validation is correct`(testData: TestData) {
        val container = testData.mockAddContainer()
        val validator = StrategyWithDayBudgetValidatorProvider.createAddStrategyValidator(container)
        val strategy = TestDefaultManualStrategy.clientDefaultManualStrategy()
            .withDayBudget(testData.dayBudget)
            .withDayBudgetShowMode(testData.showMode)
            .withDayBudgetDailyChangeCount(testData.dailyBudgetChangeCount)

        val validationResult = validator.apply(strategy)

        validationResult.check(testData.matcher())
    }

    companion object {
        private val currency = Currencies.getCurrency(CurrencyCode.RUB)
        private fun mockCampaignWithDayBudget(dayBudget: BigDecimal, showMode: CampaignsDayBudgetShowMode): Campaign {
            val m = mock<Campaign>()
            whenever(m.dayBudget).thenReturn(dayBudget)
            whenever(m.dayBudgetShowMode).thenReturn(showMode)
            return m
        }

        private fun mockDayBudgetChangeCount(changeCount: Int): TextCampaign {
            val m = mock<TextCampaign>()
            whenever(m.dayBudgetDailyChangeCount).thenReturn(changeCount)
            return m
        }

        data class DefectWithField(val defect: Defect<*>, val field: ModelProperty<*, *>)

        data class TestData(
            val dayBudget: BigDecimal?,
            val showMode: StrategyDayBudgetShowMode?,
            val campaignType: CampaignType? = null,
            val isDifferentPlacesEnabled: Boolean = false,
            val dailyBudgetChangeCount: Int = 0,
            val linkedCampaigns: List<CampaignWithPackageStrategy> = emptyList(),
            val wallet: Campaign,
            val defect: DefectWithField? = null
        ) {
            fun mockAddContainer(): StrategyAddOperationContainer {
                val m = mock<StrategyAddOperationContainer>()
                whenever(m.campaignType(anyOrNull())).thenReturn(campaignType)
                whenever(m.currency).thenReturn(currency)
                whenever(m.wallet).thenReturn(wallet)
                whenever(m.campaigns(anyOrNull())).thenReturn(linkedCampaigns)
                return m
            }

            fun matcher(): Matcher<ValidationResult<StrategyWithDayBudget, Defect<*>>> = if (defect != null) {
                Matchers.hasDefectDefinitionWith(
                    Matchers.validationError(
                        PathHelper.path(PathHelper.field(defect.field)),
                        defect.defect
                    )
                )
            } else {
                Matchers.hasNoDefectsDefinitions()
            }

            override fun toString(): String {
                return "TestData(dayBudget=$dayBudget, showMode=$showMode, campaignType=$campaignType, isDifferentPlacesEnabled=$isDifferentPlacesEnabled, dailyBudgetChangeCount=$dailyBudgetChangeCount)"
            }
        }
    }
}
