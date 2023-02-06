package ru.yandex.direct.api.v5.entity.campaigns.converter

import com.yandex.direct.api.v5.campaigns.MobileAppCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignStrategy
import com.yandex.direct.api.v5.campaigns.ObjectFactory
import com.yandex.direct.api.v5.campaigns.StrategyAverageCpc
import com.yandex.direct.api.v5.campaigns.StrategyMaximumClicks
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class MobileAppCampaignStrategyConversionTest {
    private val factory = ObjectFactory()

    fun manualStrategies() = listOf(
        arrayOf(
            "search = HIGHEST_POSITION, network = SERVING_OFF",
            MobileAppCampaignStrategy().apply {
                search = MobileAppCampaignSearchStrategy().apply {
                    biddingStrategyType = MobileAppCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                }
                network = MobileAppCampaignNetworkStrategy().apply {
                    biddingStrategyType = MobileAppCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
        arrayOf(
            "search = HIGHEST_POSITION, network = MAXIMUM_COVERAGE",
            MobileAppCampaignStrategy().apply {
                search = MobileAppCampaignSearchStrategy().apply {
                    biddingStrategyType = MobileAppCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                }
                network = MobileAppCampaignNetworkStrategy().apply {
                    biddingStrategyType = MobileAppCampaignNetworkStrategyTypeEnum.MAXIMUM_COVERAGE
                }
            },
        ),
        arrayOf(
            "search = SERVING_OFF, network = MAXIMUM_COVERAGE",
            MobileAppCampaignStrategy().apply {
                search = MobileAppCampaignSearchStrategy().apply {
                    biddingStrategyType = MobileAppCampaignSearchStrategyTypeEnum.SERVING_OFF
                }
                network = MobileAppCampaignNetworkStrategy().apply {
                    biddingStrategyType = MobileAppCampaignNetworkStrategyTypeEnum.MAXIMUM_COVERAGE
                }
            },
        ),
    )

    fun automaticSearchStrategies() = listOf(
        arrayOf(
            "search = AVERAGE_CPC, network = SERVING_OFF",
            MobileAppCampaignStrategy().apply {
                search = MobileAppCampaignSearchStrategy().apply {
                    biddingStrategyType = MobileAppCampaignSearchStrategyTypeEnum.AVERAGE_CPC
                    averageCpc = StrategyAverageCpc().apply {
                        averageCpc = 20 * 1_000_000
                        weeklySpendLimit = factory.createStrategyAverageCpcWeeklySpendLimit(300 * 1_000_000)
                    }
                }
                network = MobileAppCampaignNetworkStrategy().apply {
                    biddingStrategyType = MobileAppCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
        arrayOf(
            "search = WB_MAXIMUM_CLICKS, network = SERVING_OFF",
            MobileAppCampaignStrategy().apply {
                search = MobileAppCampaignSearchStrategy().apply {
                    biddingStrategyType = MobileAppCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS
                    wbMaximumClicks = StrategyMaximumClicks().apply {
                        weeklySpendLimit = 300 * 1_000_000
                        bidCeiling = factory.createStrategyWeeklyBudgetBaseBidCeiling(1000 * 1_000_000)
                    }
                }
                network = MobileAppCampaignNetworkStrategy().apply {
                    biddingStrategyType = MobileAppCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
    )

    fun automaticNetworkStrategies() = listOf(
        arrayOf(
            "search = SERVING_OFF, network = AVERAGE_CPC",
            MobileAppCampaignStrategy().apply {
                search = MobileAppCampaignSearchStrategy().apply {
                    biddingStrategyType = MobileAppCampaignSearchStrategyTypeEnum.SERVING_OFF
                }
                network = MobileAppCampaignNetworkStrategy().apply {
                    biddingStrategyType = MobileAppCampaignNetworkStrategyTypeEnum.AVERAGE_CPC
                    averageCpc = StrategyAverageCpc().apply {
                        averageCpc = 20 * 1_000_000
                        weeklySpendLimit = factory.createStrategyAverageCpcWeeklySpendLimit(300 * 1_000_000)
                    }
                }
            },
        ),
        arrayOf(
            "search = SERVING_OFF, network = WB_MAXIMUM_CLICKS",
            MobileAppCampaignStrategy().apply {
                search = MobileAppCampaignSearchStrategy().apply {
                    biddingStrategyType = MobileAppCampaignSearchStrategyTypeEnum.SERVING_OFF
                }
                network = MobileAppCampaignNetworkStrategy().apply {
                    biddingStrategyType = MobileAppCampaignNetworkStrategyTypeEnum.WB_MAXIMUM_CLICKS
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
        apiStrategy: MobileAppCampaignStrategy,
    ) {
        val dbStrategy = MobileAppCampaignStrategyConverter.toCampaignStrategy(apiStrategy)
        val contextLimit = apiStrategy.network?.networkDefault?.limitPercent
        val convertedApiStrategy = toMobileAppCampaignExternalStrategy(dbStrategy, contextLimit)

        assertThat(convertedApiStrategy)
            .usingRecursiveComparison()
            .isEqualTo(apiStrategy)
    }
}
