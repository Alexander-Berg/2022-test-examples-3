package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.CampaignGetItem
import com.yandex.direct.api.v5.campaigns.CampaignTypeGetEnum
import com.yandex.direct.api.v5.campaigns.CampaignsSelectionCriteria
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignGetItem
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignStrategy
import com.yandex.direct.api.v5.campaigns.GetResponse
import com.yandex.direct.api.v5.campaigns.PlacementTypeArray
import com.yandex.direct.api.v5.campaigns.PlacementTypesEnum
import com.yandex.direct.api.v5.campaigns.PriorityGoalsArray
import com.yandex.direct.api.v5.general.AttributionModelEnum
import com.yandex.direct.api.v5.general.YesNoEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.PlacementType
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.data.campaign.TestDynamicCampaigns
import ru.yandex.direct.core.testing.info.campaign.DynamicCampaignInfo

@Api5Test
@RunWith(SpringRunner::class)
class GetDynamicTextCampaignDelegateTest : GetCampaignsDelegateBaseTest() {

    @Test
    fun `get dynamic campaign default fields`() {
        val hrefParams = "utm_param=value"
        val campaignInfo = createDynamicTextCampaign {
            strategy = TestCampaignsStrategy.defaultStrategy()
            attributionModel = CampaignAttributionModel.FIRST_CLICK
            meaningfulGoals = listOf(MEANINGFUL_GOAL)
            placementTypes = setOf(PlacementType.SEARCH_PAGE)
            bannerHrefParams = hrefParams
        }
        val campaign = campaignInfo.typedCampaign

        val actualResponse = doGetRequest {
            selectionCriteria = CampaignsSelectionCriteria().apply {
                ids = listOf(campaign.id)
            }
            fieldNames = listOf(
                CampaignFieldEnum.TYPE,
            )
            dynamicTextCampaignFieldNames = listOf(
                DynamicTextCampaignFieldEnum.ATTRIBUTION_MODEL,
                DynamicTextCampaignFieldEnum.BIDDING_STRATEGY,
                DynamicTextCampaignFieldEnum.COUNTER_IDS,
                DynamicTextCampaignFieldEnum.PLACEMENT_TYPES,
                DynamicTextCampaignFieldEnum.PRIORITY_GOALS,
                DynamicTextCampaignFieldEnum.TRACKING_PARAMS
            )
        }

        val expectedResponse = GetResponse().apply {
            limitedBy = null
            campaigns = listOf(
                CampaignGetItem().apply {
                    type = CampaignTypeGetEnum.DYNAMIC_TEXT_CAMPAIGN
                    dynamicTextCampaign = DynamicTextCampaignGetItem().apply {
                        biddingStrategy = DynamicTextCampaignStrategy().apply {
                            search = DynamicTextCampaignSearchStrategy().apply {
                                biddingStrategyType = DynamicTextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                            }
                            network = DynamicTextCampaignNetworkStrategy().apply {
                                biddingStrategyType = DynamicTextCampaignNetworkStrategyTypeEnum.SERVING_OFF
                            }
                        }
                        counterIds = FACTORY.createDynamicTextCampaignBaseCounterIds(null)
                        attributionModel = AttributionModelEnum.FC
                        priorityGoals = FACTORY.createDynamicTextCampaignGetItemPriorityGoals(
                            PriorityGoalsArray().withItems(EXPECTED_PRIORITY_GOAL)
                        )
                        placementTypes = PlacementTypeArray().withItems(
                            com.yandex.direct.api.v5.campaigns.PlacementType().apply {
                                type = PlacementTypesEnum.SEARCH_RESULTS
                                value = YesNoEnum.YES
                            },
                            com.yandex.direct.api.v5.campaigns.PlacementType().apply {
                                type = PlacementTypesEnum.PRODUCT_GALLERY
                                value = YesNoEnum.NO
                            }
                        )
                        trackingParams = FACTORY.createDynamicTextCampaignGetItemTrackingParams(hrefParams)
                    }
                }
            )
        }

        assertThat(actualResponse)
            .usingRecursiveComparison()
            .isEqualTo(expectedResponse)
    }

    private fun createDynamicTextCampaign(block: DynamicCampaign.() -> Unit): DynamicCampaignInfo {
        val typedCampaign = TestDynamicCampaigns.fullDynamicCampaign().apply(block)
        return steps.dynamicCampaignSteps().createCampaign(clientInfo, typedCampaign)
    }
}
