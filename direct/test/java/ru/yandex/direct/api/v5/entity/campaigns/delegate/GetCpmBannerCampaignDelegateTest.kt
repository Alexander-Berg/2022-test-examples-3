package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.CampaignGetItem
import com.yandex.direct.api.v5.campaigns.CampaignTypeGetEnum
import com.yandex.direct.api.v5.campaigns.CampaignsSelectionCriteria
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignGetItem
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignStrategy
import com.yandex.direct.api.v5.campaigns.FrequencyCapSetting
import com.yandex.direct.api.v5.campaigns.GetResponse
import com.yandex.direct.api.v5.campaigns.ObjectFactory
import com.yandex.direct.api.v5.general.VideoTargetEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.campaign.model.EshowsSettings
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns
import ru.yandex.direct.core.testing.info.campaign.CpmBannerCampaignInfo

@Api5Test
@RunWith(SpringRunner::class)
class GetCpmBannerCampaignDelegateTest : GetCampaignsDelegateBaseTest() {

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Test
    fun `get cpm banner campaign specific fields`() {
        val campaignInfo = createCpmBannerCampaign {
            strategy = TestCampaignsStrategy.defaultCpmStrategyData()
            impressionRateCount = 2
            impressionRateIntervalDays = 2
            eshowsSettings = EshowsSettings().withVideoType(EshowsVideoType.LONG_CLICKS)
        }
        val campaign = campaignTypedRepository
            .getTyped(campaignInfo.shard, listOf(campaignInfo.id))
            .single() as CpmBannerCampaign

        val actualResponse = doGetRequest {
            selectionCriteria = CampaignsSelectionCriteria().apply {
                ids = listOf(campaign.id)
            }
            fieldNames = listOf(
                CampaignFieldEnum.TYPE,
            )
            cpmBannerCampaignFieldNames = listOf(
                CpmBannerCampaignFieldEnum.BIDDING_STRATEGY,
                CpmBannerCampaignFieldEnum.FREQUENCY_CAP,
                CpmBannerCampaignFieldEnum.VIDEO_TARGET,
                CpmBannerCampaignFieldEnum.COUNTER_IDS,
            )
        }

        val expectedResponse = GetResponse().apply {
            limitedBy = null
            campaigns = listOf(
                CampaignGetItem().apply {
                    type = CampaignTypeGetEnum.CPM_BANNER_CAMPAIGN
                    cpmBannerCampaign = CpmBannerCampaignGetItem().apply {
                        biddingStrategy = CpmBannerCampaignStrategy().apply {
                            search = CpmBannerCampaignSearchStrategy().apply {
                                biddingStrategyType = CpmBannerCampaignSearchStrategyTypeEnum.SERVING_OFF
                            }
                            network = CpmBannerCampaignNetworkStrategy().apply {
                                biddingStrategyType = CpmBannerCampaignNetworkStrategyTypeEnum.MANUAL_CPM
                            }
                        }
                        frequencyCap = factory.createCpmBannerCampaignBaseFrequencyCap(
                            FrequencyCapSetting().withImpressions(2).withPeriodDays(2)
                        )
                        videoTarget = factory.createCpmBannerCampaignGetItemVideoTarget(VideoTargetEnum.CLICKS)
                        counterIds = FACTORY.createCpmBannerCampaignBaseCounterIds(null)
                    }
                }
            )
        }

        assertThat(actualResponse)
            .usingRecursiveComparison()
            .isEqualTo(expectedResponse)
    }

    private fun createCpmBannerCampaign(block: CpmBannerCampaign.() -> Unit): CpmBannerCampaignInfo {
        val typedCampaign = TestCpmBannerCampaigns.fullCpmBannerCampaign().apply(block)
        return steps.cpmBannerCampaignSteps().createCampaign(clientInfo, typedCampaign)
    }

    private companion object {
        private val factory = ObjectFactory()
    }
}
