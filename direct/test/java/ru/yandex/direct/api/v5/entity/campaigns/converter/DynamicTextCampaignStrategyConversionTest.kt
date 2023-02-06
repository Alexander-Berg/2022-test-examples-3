package ru.yandex.direct.api.v5.entity.campaigns.converter

import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignStrategy
import com.yandex.direct.api.v5.campaigns.ObjectFactory
import com.yandex.direct.api.v5.campaigns.StrategyAverageCpc
import com.yandex.direct.api.v5.campaigns.StrategyAverageRoi
import com.yandex.direct.api.v5.campaigns.StrategyMaximumClicks
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class DynamicTextCampaignStrategyConversionTest {
    private val factory = ObjectFactory()

    fun manualStrategies() = listOf(
        arrayOf(
            "search = HIGHEST_POSITION, network = SERVING_OFF",
            DynamicTextCampaignStrategy().apply {
                search = DynamicTextCampaignSearchStrategy().apply {
                    biddingStrategyType = DynamicTextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                }
                network = DynamicTextCampaignNetworkStrategy().apply {
                    biddingStrategyType = DynamicTextCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
    )

    fun automaticSearchStrategies() = listOf(
        arrayOf(
            "search = AVERAGE_CPC, network = SERVING_OFF",
            DynamicTextCampaignStrategy().apply {
                search = DynamicTextCampaignSearchStrategy().apply {
                    biddingStrategyType = DynamicTextCampaignSearchStrategyTypeEnum.AVERAGE_CPC
                    averageCpc = StrategyAverageCpc().apply {
                        averageCpc = 20 * 1_000_000
                        weeklySpendLimit = factory.createStrategyAverageCpcWeeklySpendLimit(300 * 1_000_000)
                    }
                }
                network = DynamicTextCampaignNetworkStrategy().apply {
                    biddingStrategyType = DynamicTextCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
        arrayOf(
            "search = AVERAGE_ROI, network = SERVING_OFF",
            DynamicTextCampaignStrategy().apply {
                search = DynamicTextCampaignSearchStrategy().apply {
                    biddingStrategyType = DynamicTextCampaignSearchStrategyTypeEnum.AVERAGE_ROI
                    averageRoi = StrategyAverageRoi().apply {
                        reserveReturn = 10
                        roiCoef = 100 * 1_000_000
                        goalId = 1234
                        weeklySpendLimit = factory.createStrategyAverageRoiWeeklySpendLimit(300 * 1_000_000)
                        bidCeiling = factory.createStrategyAverageRoiBidCeiling(20 * 1_000_000)
                        profitability = factory.createStrategyAverageRoiProfitability(500 * 1_000_000)
                    }
                }
                network = DynamicTextCampaignNetworkStrategy().apply {
                    biddingStrategyType = DynamicTextCampaignNetworkStrategyTypeEnum.SERVING_OFF

                }
            },
        ),
        arrayOf(
            "search = WB_MAXIMUM_CLICKS, network = SERVING_OFF",
            DynamicTextCampaignStrategy().apply {
                search = DynamicTextCampaignSearchStrategy().apply {
                    biddingStrategyType = DynamicTextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS
                    wbMaximumClicks = StrategyMaximumClicks().apply {
                        weeklySpendLimit = 300 * 1_000_000
                        bidCeiling = factory.createStrategyWeeklyBudgetBaseBidCeiling(1000 * 1_000_000)
                    }
                }
                network = DynamicTextCampaignNetworkStrategy().apply {
                    biddingStrategyType = DynamicTextCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
    )

    @Test
    @Parameters(method = "manualStrategies, automaticSearchStrategies")
    @TestCaseName("{0}")
    fun `external strategy to internal and back to external`(
        @Suppress("UNUSED_PARAMETER") name: String,
        apiStrategy: DynamicTextCampaignStrategy,
    ) {
        val dbStrategy = DynamicTextCampaignStrategyConverter.toCampaignStrategy(apiStrategy)
        val convertedApiStrategy = toDynamicTextCampaignExternalStrategy(dbStrategy)

        assertThat(convertedApiStrategy)
            .usingRecursiveComparison()
            .isEqualTo(apiStrategy)
    }
}
