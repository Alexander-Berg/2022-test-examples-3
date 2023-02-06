package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.CampaignGetItem
import com.yandex.direct.api.v5.campaigns.CampaignTypeGetEnum
import com.yandex.direct.api.v5.campaigns.CampaignsSelectionCriteria
import com.yandex.direct.api.v5.campaigns.GetResponse
import com.yandex.direct.api.v5.campaigns.PriorityGoalsArray
import com.yandex.direct.api.v5.campaigns.SmartCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.SmartCampaignGetItem
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.SmartCampaignStrategy
import com.yandex.direct.api.v5.campaigns.StrategyAverageRoi
import com.yandex.direct.api.v5.general.AttributionModelEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.data.campaign.TestSmartCampaigns
import ru.yandex.direct.core.testing.info.campaign.SmartCampaignInfo

@Api5Test
@RunWith(SpringRunner::class)
class GetSmartCampaignDelegateTest : GetCampaignsDelegateBaseTest() {

    @Test
    fun `get smart campaign specific fields`() {
        val hrefParams = "utm_param=value"
        val campaignInfo = createSmartCampaign {
            strategy = TestCampaignsStrategy.defaultAutobudgetRoiStrategy(0L, false)
            attributionModel = CampaignAttributionModel.FIRST_CLICK
            meaningfulGoals = listOf(MEANINGFUL_GOAL)
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
            smartCampaignFieldNames = listOf(
                SmartCampaignFieldEnum.ATTRIBUTION_MODEL,
                SmartCampaignFieldEnum.BIDDING_STRATEGY,
                SmartCampaignFieldEnum.COUNTER_ID,
                SmartCampaignFieldEnum.PRIORITY_GOALS,
                SmartCampaignFieldEnum.TRACKING_PARAMS
            )
        }

        val expectedResponse = GetResponse().apply {
            limitedBy = null
            campaigns = listOf(
                CampaignGetItem().apply {
                    type = CampaignTypeGetEnum.SMART_CAMPAIGN
                    smartCampaign = SmartCampaignGetItem().apply {
                        biddingStrategy = SmartCampaignStrategy().apply {
                            search = SmartCampaignSearchStrategy().apply {
                                biddingStrategyType = SmartCampaignSearchStrategyTypeEnum.AVERAGE_ROI
                                // у стратегии много настроек, нет смысла проверять их в юнит-тестах
                                averageRoi = StrategyAverageRoi()
                            }
                            network = SmartCampaignNetworkStrategy().apply {
                                biddingStrategyType = SmartCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT
                            }
                        }
                        counterId = campaign.metrikaCounters?.firstOrNull()
                        attributionModel = AttributionModelEnum.FC
                        priorityGoals = FACTORY.createSmartCampaignGetItemPriorityGoals(
                            PriorityGoalsArray().withItems(EXPECTED_PRIORITY_GOAL)
                        )
                        trackingParams = FACTORY.createSmartCampaignGetItemTrackingParams(hrefParams)
                    }
                }
            )
        }

        assertThat(actualResponse)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*averageRoi.*")
            .isEqualTo(expectedResponse)
        assertThat(actualResponse.campaigns[0].smartCampaign.biddingStrategy.search.averageRoi)
            .isNotNull
    }

    private fun createSmartCampaign(block: SmartCampaign.() -> Unit): SmartCampaignInfo {
        val typedCampaign = TestSmartCampaigns.fullSmartCampaign().apply(block)
        return steps.smartCampaignSteps().createCampaign(clientInfo, typedCampaign)
    }
}
