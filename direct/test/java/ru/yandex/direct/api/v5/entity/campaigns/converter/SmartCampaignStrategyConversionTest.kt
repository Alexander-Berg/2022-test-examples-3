package ru.yandex.direct.api.v5.entity.campaigns.converter

import com.yandex.direct.api.v5.campaigns.ObjectFactory
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.SmartCampaignStrategy
import com.yandex.direct.api.v5.campaigns.StrategyAverageRoi
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class SmartCampaignStrategyConversionTest {
    private val factory = ObjectFactory()

    fun automaticSearchStrategies() = listOf(
        arrayOf(
            "search = AVERAGE_ROI, network = SERVING_OFF",
            SmartCampaignStrategy().apply {
                search = SmartCampaignSearchStrategy().apply {
                    biddingStrategyType = SmartCampaignSearchStrategyTypeEnum.AVERAGE_ROI
                    averageRoi = StrategyAverageRoi().apply {
                        reserveReturn = 10
                        roiCoef = 100 * 1_000_000
                        goalId = 1234
                        weeklySpendLimit = factory.createStrategyAverageRoiWeeklySpendLimit(300 * 1_000_000)
                        bidCeiling = factory.createStrategyAverageRoiBidCeiling(20 * 1_000_000)
                        profitability = factory.createStrategyAverageRoiProfitability(500 * 1_000_000)
                    }
                }
                network = SmartCampaignNetworkStrategy().apply {
                    biddingStrategyType = SmartCampaignNetworkStrategyTypeEnum.SERVING_OFF

                }
            },
        ),
    )

    fun automaticNetworkStrategies() = listOf(
        arrayOf(
            "search = SERVING_OFF, network = AVERAGE_ROI",
            SmartCampaignStrategy().apply {
                search = SmartCampaignSearchStrategy().apply {
                    biddingStrategyType = SmartCampaignSearchStrategyTypeEnum.SERVING_OFF
                }
                network = SmartCampaignNetworkStrategy().apply {
                    biddingStrategyType = SmartCampaignNetworkStrategyTypeEnum.AVERAGE_ROI
                    averageRoi = StrategyAverageRoi().apply {
                        reserveReturn = 10
                        roiCoef = 100 * 1_000_000
                        goalId = 1234
                        weeklySpendLimit = factory.createStrategyAverageRoiWeeklySpendLimit(300 * 1_000_000)
                        bidCeiling = factory.createStrategyAverageRoiBidCeiling(20 * 1_000_000)
                        profitability = factory.createStrategyAverageRoiProfitability(500 * 1_000_000)
                    }
                }
            },
        ),
    )

    @Test
    @Parameters(method = "automaticSearchStrategies, automaticNetworkStrategies")
    @TestCaseName("{0}")
    fun `external strategy to internal and back to external`(
        @Suppress("UNUSED_PARAMETER") name: String,
        apiStrategy: SmartCampaignStrategy,
    ) {
        val dbStrategy = SmartCampaignStrategyConverter.toCampaignStrategy(apiStrategy)
        val contextLimit = apiStrategy.network?.networkDefault?.limitPercent
        val convertedApiStrategy = toSmartCampaignExternalStrategy(dbStrategy, contextLimit)

        assertThat(convertedApiStrategy)
            .usingRecursiveComparison()
            .isEqualTo(apiStrategy)
    }
}
