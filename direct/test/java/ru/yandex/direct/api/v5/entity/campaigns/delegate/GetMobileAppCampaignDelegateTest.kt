package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.CampaignGetItem
import com.yandex.direct.api.v5.campaigns.CampaignTypeGetEnum
import com.yandex.direct.api.v5.campaigns.CampaignsSelectionCriteria
import com.yandex.direct.api.v5.campaigns.GetResponse
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignGetItem
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignStrategy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.entity.campaigns.converter.toApiDate
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.data.campaign.TestMobileContentCampaigns
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo

@Api5Test
@RunWith(SpringRunner::class)
class GetMobileAppCampaignDelegateTest : GetCampaignsDelegateBaseTest() {

    @Test
    fun `get mobile app campaign specific fields`() {
        val campaignInfo = createMobileAppCampaign {
            strategy = TestCampaignsStrategy.defaultStrategy()
        }
        val campaign = campaignInfo.typedCampaign

        val actualResponse = doGetRequest {
            selectionCriteria = CampaignsSelectionCriteria().apply {
                ids = listOf(campaign.id)
            }
            fieldNames = listOf(
                CampaignFieldEnum.TYPE,
            )
            mobileAppCampaignFieldNames = listOf(
                MobileAppCampaignFieldEnum.BIDDING_STRATEGY,
            )
        }

        val expectedResponse = GetResponse().apply {
            limitedBy = null
            campaigns = listOf(
                CampaignGetItem().apply {
                    type = CampaignTypeGetEnum.MOBILE_APP_CAMPAIGN
                    mobileAppCampaign = MobileAppCampaignGetItem().apply {
                        biddingStrategy = MobileAppCampaignStrategy().apply {
                            search = MobileAppCampaignSearchStrategy().apply {
                                biddingStrategyType = MobileAppCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                            }
                            network = MobileAppCampaignNetworkStrategy().apply {
                                biddingStrategyType = MobileAppCampaignNetworkStrategyTypeEnum.MAXIMUM_COVERAGE
                            }
                        }
                    }
                }
            )
        }

        assertThat(actualResponse)
            .usingRecursiveComparison()
            .isEqualTo(expectedResponse)
    }

    private fun createMobileAppCampaign(block: MobileContentCampaign.() -> Unit): MobileContentCampaignInfo {
        val typedCampaign = TestMobileContentCampaigns.fullMobileContentCampaign(13).apply(block)
        return steps.mobileContentCampaignSteps().createCampaign(clientInfo, typedCampaign)
    }
}
