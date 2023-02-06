package ru.yandex.direct.api.v5.entity.campaigns.converter

import com.yandex.direct.api.v5.campaigns.ObjectFactory
import com.yandex.direct.api.v5.campaigns.StrategyAverageCpc
import com.yandex.direct.api.v5.campaigns.StrategyAverageRoi
import com.yandex.direct.api.v5.campaigns.StrategyMaximumClicks
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignStrategy
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class TextCampaignStrategyConversionTest {
    private val factory = ObjectFactory()

    fun manualStrategies() = listOf(
        arrayOf(
            "search = HIGHEST_POSITION, network = SERVING_OFF",
            TextCampaignStrategy().apply {
                search = TextCampaignSearchStrategy().apply {
                    biddingStrategyType = TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                }
                network = TextCampaignNetworkStrategy().apply {
                    biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
        arrayOf(
            "search = HIGHEST_POSITION, network = MAXIMUM_COVERAGE",
            TextCampaignStrategy().apply {
                search = TextCampaignSearchStrategy().apply {
                    biddingStrategyType = TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                }
                network = TextCampaignNetworkStrategy().apply {
                    biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.MAXIMUM_COVERAGE
                }
            },
        ),
        arrayOf(
            "search = SERVING_OFF, network = MAXIMUM_COVERAGE",
            TextCampaignStrategy().apply {
                search = TextCampaignSearchStrategy().apply {
                    biddingStrategyType = TextCampaignSearchStrategyTypeEnum.SERVING_OFF
                }
                network = TextCampaignNetworkStrategy().apply {
                    biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.MAXIMUM_COVERAGE
                }
            },
        ),
    )

    fun automaticSearchStrategies() = listOf(
        arrayOf(
            "search = AVERAGE_CPC, network = SERVING_OFF",
            TextCampaignStrategy().apply {
                search = TextCampaignSearchStrategy().apply {
                    biddingStrategyType = TextCampaignSearchStrategyTypeEnum.AVERAGE_CPC
                    averageCpc = StrategyAverageCpc().apply {
                        averageCpc = 20 * 1_000_000
                        weeklySpendLimit = factory.createStrategyAverageCpcWeeklySpendLimit(300 * 1_000_000)
                    }
                }
                network = TextCampaignNetworkStrategy().apply {
                    biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
        arrayOf(
            "search = AVERAGE_ROI, network = SERVING_OFF",
            TextCampaignStrategy().apply {
                search = TextCampaignSearchStrategy().apply {
                    biddingStrategyType = TextCampaignSearchStrategyTypeEnum.AVERAGE_ROI
                    averageRoi = StrategyAverageRoi().apply {
                        reserveReturn = 10
                        roiCoef = 100 * 1_000_000
                        goalId = 1234
                        weeklySpendLimit = factory.createStrategyAverageRoiWeeklySpendLimit(300 * 1_000_000)
                        bidCeiling = factory.createStrategyAverageRoiBidCeiling(20 * 1_000_000)
                        profitability = factory.createStrategyAverageRoiProfitability(500 * 1_000_000)
                    }
                }
                network = TextCampaignNetworkStrategy().apply {
                    biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.SERVING_OFF

                }
            },
        ),
        arrayOf(
            "search = WB_MAXIMUM_CLICKS, network = SERVING_OFF",
            TextCampaignStrategy().apply {
                search = TextCampaignSearchStrategy().apply {
                    biddingStrategyType = TextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS
                    wbMaximumClicks = StrategyMaximumClicks().apply {
                        weeklySpendLimit = 300 * 1_000_000
                        bidCeiling = factory.createStrategyWeeklyBudgetBaseBidCeiling(1000 * 1_000_000)
                    }
                }
                network = TextCampaignNetworkStrategy().apply {
                    biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
    )

    fun automaticNetworkStrategies() = listOf(
        arrayOf(
            "search = SERVING_OFF, network = AVERAGE_CPC",
            TextCampaignStrategy().apply {
                search = TextCampaignSearchStrategy().apply {
                    biddingStrategyType = TextCampaignSearchStrategyTypeEnum.SERVING_OFF
                }
                network = TextCampaignNetworkStrategy().apply {
                    biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.AVERAGE_CPC
                    averageCpc = StrategyAverageCpc().apply {
                        averageCpc = 20 * 1_000_000
                        weeklySpendLimit = factory.createStrategyAverageCpcWeeklySpendLimit(300 * 1_000_000)
                    }
                }
            },
        ),
        arrayOf(
            "search = SERVING_OFF, network = AVERAGE_ROI",
            TextCampaignStrategy().apply {
                search = TextCampaignSearchStrategy().apply {
                    biddingStrategyType = TextCampaignSearchStrategyTypeEnum.SERVING_OFF
                }
                network = TextCampaignNetworkStrategy().apply {
                    biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.AVERAGE_ROI
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
        arrayOf(
            "search = SERVING_OFF, network = WB_MAXIMUM_CLICKS",
            TextCampaignStrategy().apply {
                search = TextCampaignSearchStrategy().apply {
                    biddingStrategyType = TextCampaignSearchStrategyTypeEnum.SERVING_OFF
                }
                network = TextCampaignNetworkStrategy().apply {
                    biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.WB_MAXIMUM_CLICKS
                    wbMaximumClicks = StrategyMaximumClicks().apply {
                        weeklySpendLimit = 300 * 1_000_000
                        bidCeiling = factory.createStrategyWeeklyBudgetBaseBidCeiling(1000 * 1_000_000)
                    }
                }
            },
        ),
    )

    @Test
    @Parameters(method = "manualStrategies, automaticSearchStrategies, automaticNetworkStrategies")
    @TestCaseName("{0}")
    fun `external strategy to internal and back to external`(
        @Suppress("UNUSED_PARAMETER") name: String,
        apiStrategy: TextCampaignStrategy,
    ) {
        val dbStrategy = TextCampaignStrategyConverter.toCampaignStrategy(apiStrategy)
        val contextLimit = apiStrategy.network?.networkDefault?.limitPercent
        val convertedApiStrategy = toTextCampaignExternalStrategy(dbStrategy, contextLimit)

        assertThat(convertedApiStrategy)
            .usingRecursiveComparison()
            .isEqualTo(apiStrategy)
    }
}
