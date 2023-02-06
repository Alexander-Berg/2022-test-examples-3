package ru.yandex.direct.api.v5.entity.campaigns.converter

import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignStrategy
import com.yandex.direct.api.v5.campaigns.StrategyWbMaximumImpressions
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class CpmBannerCampaignStrategyConversionTest {
    fun manualStrategies() = listOf(
        arrayOf(
            "search = SERVING_OFF, network = MANUAL_CPM",
            CpmBannerCampaignStrategy().apply {
                search = CpmBannerCampaignSearchStrategy().apply {
                    biddingStrategyType = CpmBannerCampaignSearchStrategyTypeEnum.SERVING_OFF
                }
                network = CpmBannerCampaignNetworkStrategy().apply {
                    biddingStrategyType = CpmBannerCampaignNetworkStrategyTypeEnum.MANUAL_CPM
                }
            },
        ),
    )

    fun automaticNetworkStrategies() = listOf(
        arrayOf(
            "search = SERVING_OFF, network = WB_MAXIMUM_IMPRESSIONS",
            CpmBannerCampaignStrategy().apply {
                search = CpmBannerCampaignSearchStrategy().apply {
                    biddingStrategyType = CpmBannerCampaignSearchStrategyTypeEnum.SERVING_OFF
                }
                network = CpmBannerCampaignNetworkStrategy().apply {
                    biddingStrategyType = CpmBannerCampaignNetworkStrategyTypeEnum.WB_MAXIMUM_IMPRESSIONS
                    wbMaximumImpressions = StrategyWbMaximumImpressions().apply {
                        averageCpm = 300 * 1_000_000
                        spendLimit = 1000 * 1_000_000
                    }
                }
            },
        ),
    )

    @Test
    @Parameters(method = "manualStrategies, automaticNetworkStrategies")
    @TestCaseName("{0}")
    fun `external strategy to internal and back to external`(
        @Suppress("UNUSED_PARAMETER") name: String,
        apiStrategy: CpmBannerCampaignStrategy,
    ) {
        val dbStrategy = CpmBannerCampaignStrategyConverter.toCampaignStrategy(apiStrategy)
        val convertedApiStrategy = toCpmBannerCampaignExternalStrategy(dbStrategy)

        assertThat(convertedApiStrategy)
            .usingRecursiveComparison()
            .isEqualTo(apiStrategy)
    }
}
