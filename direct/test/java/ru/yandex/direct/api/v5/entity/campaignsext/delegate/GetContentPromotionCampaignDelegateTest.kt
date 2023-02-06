package ru.yandex.direct.api.v5.entity.campaignsext.delegate

import com.yandex.direct.api.v5.campaignsext.CampaignFieldEnum
import com.yandex.direct.api.v5.campaignsext.CampaignGetItem
import com.yandex.direct.api.v5.campaignsext.CampaignTypeGetEnum
import com.yandex.direct.api.v5.campaignsext.CampaignsSelectionCriteria
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignFieldEnum
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignGetItem
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSearchStrategy
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignStrategy
import com.yandex.direct.api.v5.campaignsext.GetResponse
import com.yandex.direct.api.v5.general.AttributionModelEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo

@Api5Test
@RunWith(SpringRunner::class)
class GetContentPromotionCampaignDelegateTest : GetCampaignsExtDelegateBaseTest() {

    @Test
    fun `get content promotion campaign specific fields`() {
        val campaignInfo = createContentPromotionCampaign {
            strategy = TestCampaignsStrategy.defaultStrategy()
            attributionModel = CampaignAttributionModel.FIRST_CLICK
        }
        val campaign = campaignInfo.typedCampaign

        val actualResponse = doGetRequest {
            selectionCriteria = CampaignsSelectionCriteria().apply {
                ids = listOf(campaign.id)
            }
            fieldNames = listOf(
                CampaignFieldEnum.TYPE,
            )
            contentPromotionCampaignFieldNames = listOf(
                ContentPromotionCampaignFieldEnum.ATTRIBUTION_MODEL,
                ContentPromotionCampaignFieldEnum.BIDDING_STRATEGY,
                ContentPromotionCampaignFieldEnum.COUNTER_IDS,
            )
        }

        val expectedResponse = GetResponse().apply {
            limitedBy = null
            campaigns = listOf(
                CampaignGetItem().apply {
                    type = CampaignTypeGetEnum.CONTENT_PROMOTION_CAMPAIGN
                    contentPromotionCampaign = ContentPromotionCampaignGetItem().apply {
                        biddingStrategy = ContentPromotionCampaignStrategy().apply {
                            search = ContentPromotionCampaignSearchStrategy().apply {
                                biddingStrategyType = ContentPromotionCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                            }
                            network = ContentPromotionCampaignNetworkStrategy().apply {
                                biddingStrategyType = ContentPromotionCampaignNetworkStrategyTypeEnum.SERVING_OFF
                            }
                        }
                        counterIds = FACTORY.createContentPromotionCampaignBaseCounterIds(null)
                        attributionModel = AttributionModelEnum.FC
                    }
                }
            )
        }

        assertThat(actualResponse)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(""".*\.name\.*""", """.*\.scope\.*""") // поля из JAXBElement
            .isEqualTo(expectedResponse)
    }

    private fun createContentPromotionCampaign(block: ContentPromotionCampaign.() -> Unit): ContentPromotionCampaignInfo {
        val typedCampaign = TestContentPromotionCampaigns.fullContentPromotionCampaign().apply(block)
        return steps.contentPromotionCampaignSteps().createCampaign(clientInfo, typedCampaign)
    }
}
