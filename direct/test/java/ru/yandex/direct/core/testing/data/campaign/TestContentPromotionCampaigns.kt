package ru.yandex.direct.core.testing.data.campaign

import ru.yandex.direct.common.util.RepositoryUtils.NOW_PLACEHOLDER
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonClientFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForActiveCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForDraftCampaign
import ru.yandex.direct.test.utils.RandomNumberUtils

object TestContentPromotionCampaigns {

    @JvmStatic
    fun clientContentPromotionCampaign(): ContentPromotionCampaign {
        val campaign = ContentPromotionCampaign()
        fillContentPromotionCampaignClientFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullContentPromotionCampaign(): ContentPromotionCampaign {
        val campaign = clientContentPromotionCampaign()
        fillContentPromotionCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftContentPromotionCampaign(): ContentPromotionCampaign {
        val campaign = fullContentPromotionCampaign()

        fillCommonSystemFieldsForDraftCampaign(campaign)
        return campaign
    }

    private fun fillContentPromotionCampaignClientFields(campaign: ContentPromotionCampaign) {
        fillCommonClientFields(campaign)
        campaign
            .withMetrikaCounters(emptyList())
            .withName("default name " + RandomNumberUtils.nextPositiveLong())
            .withType(CampaignType.CONTENT_PROMOTION)
            .withStrategy(TestCampaigns.manualSearchStrategy())
            .withDayBudget(CampaignConstants.DEFAULT_DAY_BUDGET)
            .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
            .withHasExtendedGeoTargeting(false)
            .withUseCurrentRegion(false)
            .withUseRegularRegion(false)
            .withEnablePausedByDayBudgetEvent(false)
    }

    private fun fillContentPromotionCampaignSystemFields(campaign: ContentPromotionCampaign) {
        fillCommonSystemFieldsForActiveCampaign(campaign)
        campaign
            .withProductId(TestTextCampaigns.TEXT_CAMPAIGN_PRODUCT_ID)
            .withDayBudgetLastChange(NOW_PLACEHOLDER)
            .withDayBudgetDailyChangeCount(0)
            .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._60)
            .withEnableCheckPositionEvent(false);
    }
}
