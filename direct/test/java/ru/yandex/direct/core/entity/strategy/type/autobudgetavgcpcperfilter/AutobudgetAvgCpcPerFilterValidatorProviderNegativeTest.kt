package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpcperfilter

import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerFilter
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path
import java.math.BigDecimal
import java.util.IdentityHashMap

@RunWith(Parameterized::class)
class AutobudgetAvgCpcPerFilterValidatorProviderNegativeTest(
    private val testName: String,
    private val strategy: AutobudgetAvgCpcPerFilter,
    private val campaign: CampaignWithPackageStrategy?,
    private val defect: Defect<*>
) {

    companion object {
        private val PATH = path(field(AutobudgetAvgCpcPerFilter.FILTER_AVG_BID))

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "validation error when filter_avg_bid is null",
                AutobudgetAvgCpcPerFilter(),
                null,
                CommonDefects.notNull(),
            ),
            arrayOf(
                "validation error when filter_avg_bid is less than min value",
                AutobudgetAvgCpcPerFilter()
                    .withFilterAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice.subtract(BigDecimal.ONE)),
                null,
                NumberDefects.greaterThanOrEqualTo(CurrencyRub.getInstance().minAutobudgetAvgPrice),
            ),
            arrayOf(
                "validation error when filter_avg_bid is less than min value for performance",
                AutobudgetAvgCpcPerFilter()
                    .withFilterAvgBid(CurrencyRub.getInstance().minCpcCpaPerformance.subtract(BigDecimal.ONE)),
                SmartCampaign().withId(1L).withType(CampaignType.PERFORMANCE),
                NumberDefects.greaterThanOrEqualTo(CurrencyRub.getInstance().minCpcCpaPerformance),
            ),
            arrayOf(
                "validation error when filter_avg_bid is greater than max value",
                AutobudgetAvgCpcPerFilter()
                    .withFilterAvgBid(CurrencyRub.getInstance().maxAutobudgetBid.add(BigDecimal.ONE)),
                null,
                NumberDefects.lessThanOrEqualTo(CurrencyRub.getInstance().maxAutobudgetBid),
            ),
            arrayOf(
                "validation error when filter_avg_bid is greater than sum",
                AutobudgetAvgCpcPerFilter()
                    .withSum(CurrencyRub.getInstance().minAutobudgetAvgPrice.add(BigDecimal.ONE))
                    .withFilterAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice.add(BigDecimal.TEN)),
                null,
                StrategyDefects.weekBudgetLessThan(),
            ),
            arrayOf(
                "validation error when filter_avg_bid is greater than bid",
                AutobudgetAvgCpcPerFilter()
                    .withBid(CurrencyRub.getInstance().minAutobudgetAvgPrice)
                    .withFilterAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice.add(BigDecimal.ONE)),
                null,
                StrategyDefects.bidLessThanAvgBid()
            ),
        )
    }

    @Test
    fun shouldValidate() {
        val container = StrategyAddOperationContainer(1, ClientId.fromLong(3L), 1L, 1L)
        container.currency = CurrencyRub.getInstance()
        campaign?.clientId = 3L
        container.typedCampaignsMap = mapOf(strategy to listOf(campaign).filterNotNull()).toMap(IdentityHashMap())
        val validator = AutobudgetAvgCpcPerFilterValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        assertThat(result, hasDefectDefinitionWith(validationError(PATH, defect)))
    }
}
