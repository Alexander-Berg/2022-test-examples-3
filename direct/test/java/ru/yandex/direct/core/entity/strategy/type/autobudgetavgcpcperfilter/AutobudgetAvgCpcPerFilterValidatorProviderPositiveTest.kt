package ru.yandex.direct.core.entity.strategy.type.autobudgetavgcpcperfilter

import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.mock
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpcPerFilter
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import java.math.BigDecimal
import java.util.IdentityHashMap

@RunWith(Parameterized::class)
class AutobudgetAvgCpcPerFilterValidatorProviderPositiveTest(
    private val testName: String,
    private val strategy: AutobudgetAvgCpcPerFilter,
    private val campaign: CampaignWithPackageStrategy?
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Collection<Array<Any?>> = listOf(
            arrayOf(
                "validation ok when filter_avg_bid is equal to min",
                AutobudgetAvgCpcPerFilter()
                    .withFilterAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice),
                null,
            ),
            arrayOf(
                "validation ok when filter_avg_bid is equal to min for performance",
                AutobudgetAvgCpcPerFilter()
                    .withFilterAvgBid(CurrencyRub.getInstance().minCpcCpaPerformance),
                SmartCampaign().withId(1L).withType(CampaignType.PERFORMANCE),
            ),
            arrayOf(
                "validation ok when filter_avg_bid is equal to max",
                AutobudgetAvgCpcPerFilter()
                    .withFilterAvgBid(CurrencyRub.getInstance().maxAutobudgetBid),
                null,
            ),
            arrayOf(
                "validation ok for all specified values",
                AutobudgetAvgCpcPerFilter()
                    .withFilterAvgBid(CurrencyRub.getInstance().minAutobudgetAvgPrice)
                    .withSum(CurrencyRub.getInstance().minAutobudgetAvgPrice.add(BigDecimal.TEN))
                    .withBid(CurrencyRub.getInstance().minAutobudgetAvgPrice.add(BigDecimal.TEN)),
                null
            ),
        )
    }

    @Test
    fun shouldValidate() {
        val container = StrategyAddOperationContainer(1, mock(ClientId::class.java), 1L, 1L)
        container.currency = CurrencyRub.getInstance()
        container.typedCampaignsMap = mapOf(strategy to listOf(campaign).filterNotNull()).toMap(IdentityHashMap())
        val validator = AutobudgetAvgCpcPerFilterValidatorProvider.createStrategyValidator(container)
        val result = validator.apply(strategy)
        assertThat(result, hasNoDefectsDefinitions())
    }
}
