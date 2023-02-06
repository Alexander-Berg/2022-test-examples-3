package ru.yandex.direct.api.v5.entity.campaignsext.converter

import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSearchStrategy
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignStrategy
import com.yandex.direct.api.v5.campaignsext.ObjectFactory
import com.yandex.direct.api.v5.campaignsext.StrategyAverageCpc
import com.yandex.direct.api.v5.campaignsext.StrategyMaximumClicks
import com.yandex.direct.api.v5.campaignsext.StrategyWeeklyClickPackage
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class ContentPromotionCampaignStrategyConversionTest {
    private val factory = ObjectFactory()

    fun manualStrategies() = listOf(
        arrayOf(
            "search = HIGHEST_POSITION, network = SERVING_OFF",
            ContentPromotionCampaignStrategy().apply {
                search = ContentPromotionCampaignSearchStrategy().apply {
                    biddingStrategyType = ContentPromotionCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                }
                network = ContentPromotionCampaignNetworkStrategy().apply {
                    biddingStrategyType = ContentPromotionCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
    )

    fun automaticSearchStrategies() = listOf(
        arrayOf(
            "search = AVERAGE_CPC, network = SERVING_OFF",
            ContentPromotionCampaignStrategy().apply {
                search = ContentPromotionCampaignSearchStrategy().apply {
                    biddingStrategyType = ContentPromotionCampaignSearchStrategyTypeEnum.AVERAGE_CPC
                    averageCpc = StrategyAverageCpc().apply {
                        averageCpc = 20 * 1_000_000
                        weeklySpendLimit = factory.createStrategyAverageCpcWeeklySpendLimit(300 * 1_000_000)
                    }
                }
                network = ContentPromotionCampaignNetworkStrategy().apply {
                    biddingStrategyType = ContentPromotionCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
        arrayOf(
            "search = WB_MAXIMUM_CLICKS, network = SERVING_OFF",
            ContentPromotionCampaignStrategy().apply {
                search = ContentPromotionCampaignSearchStrategy().apply {
                    biddingStrategyType = ContentPromotionCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS
                    wbMaximumClicks = StrategyMaximumClicks().apply {
                        weeklySpendLimit = 300 * 1_000_000
                        bidCeiling = factory.createStrategyWeeklyBudgetBaseBidCeiling(1000 * 1_000_000)
                    }
                }
                network = ContentPromotionCampaignNetworkStrategy().apply {
                    biddingStrategyType = ContentPromotionCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
        arrayOf(
            "search = WEEKLY_CLICK_PACKAGE, network = SERVING_OFF",
            ContentPromotionCampaignStrategy().apply {
                search = ContentPromotionCampaignSearchStrategy().apply {
                    biddingStrategyType = ContentPromotionCampaignSearchStrategyTypeEnum.WEEKLY_CLICK_PACKAGE
                    weeklyClickPackage = StrategyWeeklyClickPackage().apply {
                        clicksPerWeek = 300
                        averageCpc = factory.createStrategyWeeklyClickPackageAverageCpc(40_000_000)
                        bidCeiling = factory.createStrategyWeeklyBudgetBaseBidCeiling(1000 * 1_000_000)
                    }
                }
                network = ContentPromotionCampaignNetworkStrategy().apply {
                    biddingStrategyType = ContentPromotionCampaignNetworkStrategyTypeEnum.SERVING_OFF
                }
            },
        ),
    )

    @Test
    @Parameters(method = "manualStrategies, automaticSearchStrategies")
    @TestCaseName("{0}")
    fun `external strategy to internal and back to external`(
        @Suppress("UNUSED_PARAMETER") name: String,
        apiStrategy: ContentPromotionCampaignStrategy,
    ) {
        val dbStrategy = ContentPromotionCampaignStrategyConverter.toCampaignStrategy(apiStrategy)
        val convertedApiStrategy = toContentPromotionCampaignExternalStrategy(dbStrategy)

        assertThat(convertedApiStrategy)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(""".*\.name\.*""", """.*\.scope\.*""") // поля из JAXBElement
            .isEqualTo(apiStrategy)
    }
}
