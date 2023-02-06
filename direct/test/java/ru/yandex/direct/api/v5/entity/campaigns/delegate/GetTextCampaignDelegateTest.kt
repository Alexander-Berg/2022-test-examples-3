package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.CampaignGetItem
import com.yandex.direct.api.v5.campaigns.CampaignTypeGetEnum
import com.yandex.direct.api.v5.campaigns.CampaignsSelectionCriteria
import com.yandex.direct.api.v5.campaigns.GetResponse
import com.yandex.direct.api.v5.campaigns.PriorityGoalsArray
import com.yandex.direct.api.v5.campaigns.RelevantKeywordsSetting
import com.yandex.direct.api.v5.campaigns.TextCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignGetItem
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategy
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyTypeEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignStrategy
import com.yandex.direct.api.v5.general.AttributionModelEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.core.entity.campaign.model.BroadMatch
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo

@Api5Test
@RunWith(SpringRunner::class)
class GetTextCampaignDelegateTest : GetCampaignsDelegateBaseTest() {

    @Test
    fun `get text campaign specific fields`() {
        val hrefParams = "utm_param=value"
        val campaignInfo = createTextCampaign {
            strategy = TestCampaignsStrategy.defaultStrategy()
            attributionModel = CampaignAttributionModel.FIRST_CLICK
            meaningfulGoals = listOf(MEANINGFUL_GOAL)
            broadMatch = BroadMatch().apply {
                broadMatchFlag = true
                broadMatchLimit = 100
                broadMatchGoalId = 0
            }
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
            textCampaignFieldNames = listOf(
                TextCampaignFieldEnum.ATTRIBUTION_MODEL,
                TextCampaignFieldEnum.BIDDING_STRATEGY,
                TextCampaignFieldEnum.COUNTER_IDS,
                TextCampaignFieldEnum.PRIORITY_GOALS,
                TextCampaignFieldEnum.RELEVANT_KEYWORDS,
                TextCampaignFieldEnum.TRACKING_PARAMS
            )
        }

        val expectedResponse = GetResponse().apply {
            limitedBy = null
            campaigns = listOf(
                CampaignGetItem().apply {
                    type = CampaignTypeGetEnum.TEXT_CAMPAIGN
                    textCampaign = TextCampaignGetItem().apply {
                        biddingStrategy = TextCampaignStrategy().apply {
                            search = TextCampaignSearchStrategy().apply {
                                biddingStrategyType = TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION
                            }
                            network = TextCampaignNetworkStrategy().apply {
                                biddingStrategyType = TextCampaignNetworkStrategyTypeEnum.MAXIMUM_COVERAGE
                            }
                        }
                        counterIds = FACTORY.createTextCampaignBaseCounterIds(null)
                        attributionModel = AttributionModelEnum.FC
                        priorityGoals = FACTORY.createTextCampaignGetItemPriorityGoals(
                            PriorityGoalsArray().withItems(EXPECTED_PRIORITY_GOAL)
                        )
                        relevantKeywords = FACTORY.createTextCampaignBaseRelevantKeywords(
                            RelevantKeywordsSetting().apply {
                                budgetPercent = 100
                                optimizeGoalId = FACTORY.createRelevantKeywordsSettingOptimizeGoalId(0)
                            }
                        )
                        trackingParams = FACTORY.createTextCampaignGetItemTrackingParams(hrefParams)
                    }
                }
            )
        }

        assertThat(actualResponse)
            .usingRecursiveComparison()
            .isEqualTo(expectedResponse)
    }

    private fun createTextCampaign(block: TextCampaign.() -> Unit): TextCampaignInfo {
        val typedCampaign = TestTextCampaigns.fullTextCampaign().apply(block)
        return steps.textCampaignSteps().createCampaign(clientInfo, typedCampaign)
    }
}
