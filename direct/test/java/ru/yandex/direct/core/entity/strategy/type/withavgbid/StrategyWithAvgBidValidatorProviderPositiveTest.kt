package ru.yandex.direct.core.entity.strategy.type.withavgbid

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.mock
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgClick
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerCamp
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgBid
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.testing.matchers.validation.Matchers
import java.util.IdentityHashMap

@RunWith(Parameterized::class)
class StrategyWithAvgBidValidatorProviderPositiveTest(
    private val testName: String,
    private val strategy: StrategyWithAvgBid,
    private val campaign: CampaignWithPackageStrategy?
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "[AutobudgetAvgClick] avg_bid with min value",
                AutobudgetAvgClick()
                    .withType(StrategyName.AUTOBUDGET_AVG_CLICK)
                    .withAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice),
                null,
            ),
            arrayOf(
                "[AutobudgetAvgCpcPerCamp] avg_bid with min value",
                AutobudgetAvgCpcPerCamp()
                    .withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                    .withAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice),
                null,
            ),
            arrayOf(
                "[AutobudgetWeekBundle] avg_bid with min value",
                AutobudgetWeekBundle()
                    .withType(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
                    .withAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice),
                null,
            ),
            arrayOf(
                "[AutobudgetAvgClick] avg_bid with min value for performance",
                AutobudgetAvgClick()
                    .withType(StrategyName.AUTOBUDGET_AVG_CLICK)
                    .withAvgBid(CurrencyRub.getInstance().minCpcCpaPerformance),
                SmartCampaign().withId(1).withType(CampaignType.PERFORMANCE),
            ),
            arrayOf(
                "[AutobudgetAvgCpcPerCamp] avg_bid with min value for performance",
                AutobudgetAvgCpcPerCamp()
                    .withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                    .withAvgBid(CurrencyRub.getInstance().minCpcCpaPerformance),
                SmartCampaign().withId(1).withType(CampaignType.PERFORMANCE),
            ),
            arrayOf(
                "[AutobudgetWeekBundle] avg_bid with min value for performance",
                AutobudgetWeekBundle()
                    .withType(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
                    .withAvgBid(CurrencyRub.getInstance().minCpcCpaPerformance),
                SmartCampaign().withId(1).withType(CampaignType.PERFORMANCE),
            ),
            arrayOf(
                "[AutobudgetAvgClick] avg_bid with max value",
                AutobudgetAvgClick()
                    .withType(StrategyName.AUTOBUDGET_AVG_CLICK)
                    .withAvgBid(CurrencyRub.getInstance().maxAutobudgetBid),
                null,
            ),
            arrayOf(
                "[AutobudgetAvgCpcPerCamp] avg_bid with max value",
                AutobudgetAvgCpcPerCamp()
                    .withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                    .withAvgBid(CurrencyRub.getInstance().maxAutobudgetBid),
                null,
            ),
            arrayOf(
                "[AutobudgetWeekBundle] avg_bid with max value",
                AutobudgetWeekBundle()
                    .withType(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
                    .withAvgBid(CurrencyRub.getInstance().maxAutobudgetBid),
                null,
            ),
            arrayOf(
                "[AutobudgetWeekBundle] avg_bid with null value",
                AutobudgetWeekBundle().withType(StrategyName.AUTOBUDGET_WEEK_BUNDLE),
                null,
            ),
        )
    }

    @Test
    fun shouldValidate() {
        val container = StrategyAddOperationContainer(1, mock(ClientId::class.java), 1L, 1L)
        container.currency = CurrencyRub.getInstance()
        container.typedCampaignsMap = mapOf(strategy to listOf(campaign).filterNotNull()).toMap(IdentityHashMap())
        val validator = StrategyWithAvgBidValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        Assert.assertThat(result, Matchers.hasNoDefectsDefinitions())
    }
}
