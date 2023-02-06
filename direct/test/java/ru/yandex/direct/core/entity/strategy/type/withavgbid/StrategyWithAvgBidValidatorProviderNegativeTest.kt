package ru.yandex.direct.core.entity.strategy.type.withavgbid

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgClick
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerCamp
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.model.StrategyWithAvgBid
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
class StrategyWithAvgBidValidatorProviderNegativeTest(
    private val testName: String,
    private val strategy: StrategyWithAvgBid,
    private val campaign: CampaignWithPackageStrategy?,
    private val defect: Defect<*>
) {
    companion object {
        private val PATH = path(field(StrategyWithAvgBid.AVG_BID))

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "[AutobudgetAvgClick] avg_bid less than min",
                AutobudgetAvgClick()
                    .withType(StrategyName.AUTOBUDGET_AVG_CLICK)
                    .withAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice.subtract(BigDecimal.ONE)),
                null,
                NumberDefects.greaterThanOrEqualTo(CurrencyRub.getInstance().minAutobudgetAvgPrice),
            ),
            arrayOf(
                "[AutobudgetAvgCpcPerCamp] avg_bid less than min",
                AutobudgetAvgCpcPerCamp()
                    .withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                    .withAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice.subtract(BigDecimal.ONE)),
                null,
                NumberDefects.greaterThanOrEqualTo(CurrencyRub.getInstance().minAutobudgetAvgPrice),
            ),
            arrayOf(
                "[AutobudgetWeekBundle] avg_bid less than min",
                AutobudgetWeekBundle()
                    .withType(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
                    .withAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice.subtract(BigDecimal.ONE)),
                null,
                NumberDefects.greaterThanOrEqualTo(CurrencyRub.getInstance().minAutobudgetAvgPrice),
            ),
            arrayOf(
                "[AutobudgetAvgClick] avg_bid less than min for performance campaign",
                AutobudgetAvgClick()
                    .withType(StrategyName.AUTOBUDGET_AVG_CLICK)
                    .withAvgBid(CurrencyRub.getInstance().minCpcCpaPerformance.subtract(BigDecimal.ONE)),
                SmartCampaign().withId(1L).withType(CampaignType.PERFORMANCE),
                NumberDefects.greaterThanOrEqualTo(CurrencyRub.getInstance().minCpcCpaPerformance),
            ),
            arrayOf(
                "[AutobudgetAvgCpcPerCamp] avg_bid less than min for performance campaign",
                AutobudgetAvgCpcPerCamp()
                    .withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                    .withAvgBid(CurrencyRub.getInstance().minCpcCpaPerformance.subtract(BigDecimal.ONE)),
                SmartCampaign().withId(1L).withType(CampaignType.PERFORMANCE),
                NumberDefects.greaterThanOrEqualTo(CurrencyRub.getInstance().minCpcCpaPerformance),
            ),
            arrayOf(
                "[AutobudgetWeekBundle] avg_bid less than min for performance campaign",
                AutobudgetWeekBundle()
                    .withType(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
                    .withAvgBid(CurrencyRub.getInstance().minCpcCpaPerformance.subtract(BigDecimal.ONE)),
                SmartCampaign().withId(1L).withType(CampaignType.PERFORMANCE),
                NumberDefects.greaterThanOrEqualTo(CurrencyRub.getInstance().minCpcCpaPerformance),
            ),
            arrayOf(
                "[AutobudgetAvgClick] avg_bid greater than max",
                AutobudgetAvgClick()
                    .withType(StrategyName.AUTOBUDGET_AVG_CLICK)
                    .withAvgBid(CurrencyRub.getInstance().maxAutobudgetBid.add(BigDecimal.ONE)),
                null,
                NumberDefects.lessThanOrEqualTo(CurrencyRub.getInstance().maxAutobudgetBid),
            ),
            arrayOf(
                "[AutobudgetAvgCpcPerCamp] avg_bid greater than max",
                AutobudgetAvgCpcPerCamp()
                    .withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                    .withAvgBid(CurrencyRub.getInstance().maxAutobudgetBid.add(BigDecimal.ONE)),
                null,
                NumberDefects.lessThanOrEqualTo(CurrencyRub.getInstance().maxAutobudgetBid),
            ),
            arrayOf(
                "[AutobudgetWeekBundle] avg_bid greater than max",
                AutobudgetWeekBundle()
                    .withType(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
                    .withAvgBid(CurrencyRub.getInstance().maxAutobudgetBid.add(BigDecimal.ONE)),
                null,
                NumberDefects.lessThanOrEqualTo(CurrencyRub.getInstance().maxAutobudgetBid),
            ),
            arrayOf(
                "[AutobudgetAvgClick] avg_bid greater than sum",
                AutobudgetAvgClick()
                    .withType(StrategyName.AUTOBUDGET_AVG_CLICK)
                    .withAvgBid(CurrencyRub.getInstance().maxAutobudgetBid.subtract(BigDecimal.ONE))
                    .withSum(BigDecimal.TEN),
                null,
                StrategyDefects.weekBudgetLessThan()
            ),
            arrayOf(
                "[AutobudgetAvgCpcPerCamp] avg_bid greater than sum",
                AutobudgetAvgCpcPerCamp()
                    .withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                    .withAvgBid(CurrencyRub.getInstance().maxAutobudgetBid.subtract(BigDecimal.ONE))
                    .withSum(BigDecimal.TEN),
                null,
                StrategyDefects.weekBudgetLessThan()
            ),
            arrayOf(
                "[AutobudgetAvgClick] avg_bid is null",
                AutobudgetAvgClick().withType(StrategyName.AUTOBUDGET_AVG_CLICK),
                null,
                CommonDefects.notNull()
            ),
            arrayOf(
                "[AutobudgetAvgCpcPerCamp] avg_bid is null",
                AutobudgetAvgCpcPerCamp().withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP),
                null,
                CommonDefects.notNull()
            ),
            arrayOf(
                "[AutobudgetAvgCpcPerCamp] avg_bid greater than bid",
                AutobudgetAvgCpcPerCamp()
                    .withType(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                    .withAvgBid(CurrencyRub.getInstance().maxAutobudgetBid.subtract(BigDecimal.ONE))
                    .withBid(BigDecimal.TEN),
                null,
                StrategyDefects.bidLessThanAvgBid()
            ),
            arrayOf(
                "[AutobudgetWeekBundle] avg_bid and bid in the same time",
                AutobudgetWeekBundle()
                    .withType(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
                    .withAvgBid(CurrencyRub.getInstance().maxAutobudgetBid.subtract(BigDecimal.ONE))
                    .withBid(BigDecimal.TEN),
                null,
                StrategyDefects.avgBidAndBidTogetherAreProhibited()
            ),
        )
    }

    @Test
    fun shouldValidate() {
        val clientId: Long = 3
        val container = StrategyAddOperationContainer(1, ClientId.fromLong(clientId), 1L, 1L)
        campaign?.clientId = clientId
        container.currency = CurrencyRub.getInstance()
        container.typedCampaignsMap = mapOf(strategy to listOf(campaign).filterNotNull()).toMap(IdentityHashMap())
        val validator = StrategyWithAvgBidValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                Matchers.validationError(
                    PATH,
                    defect
                )
            )
        )
    }
}
