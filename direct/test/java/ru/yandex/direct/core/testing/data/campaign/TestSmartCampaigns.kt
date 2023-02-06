package ru.yandex.direct.core.testing.data.campaign

import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.DEFAULT_CONTEXT_PRICE_COEF
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonClientFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForActiveCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForDraftCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpcPerCamp

object TestSmartCampaigns {

    @JvmStatic
    fun clientSmartCampaign(): SmartCampaign {
        val campaign = SmartCampaign()
        fillSmartCampaignClientFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullSmartCampaign(): SmartCampaign {
        val campaign = clientSmartCampaign()
        fillSmartCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftSmartCampaign(): SmartCampaign {
        val campaign = fullSmartCampaign()

        fillCommonSystemFieldsForDraftCampaign(campaign)
        return campaign
    }

    private fun fillSmartCampaignClientFields(campaign: SmartCampaign) {
        fillCommonClientFields(campaign)
        campaign
            .withType(CampaignType.PERFORMANCE)
            .withPlacementTypes(emptySet())
            .withMetrikaCounters(emptyList())
            .withName("new Smart Campaign")
            .withHasAddOpenstatTagToUrl(false)
            .withHasAddMetrikaTagToUrl(false)
            .withHasExtendedGeoTargeting(false)
            .withUseCurrentRegion(false)
            .withUseRegularRegion(false)
            .withEnablePausedByDayBudgetEvent(false)
            .withEnableOfflineStatNotice(false)
            .withEnableCompanyInfo(true)
            .withEnableSendAccountNews(false)
            .withStrategy(defaultAutobudgetAvgCpcPerCamp())
            .withContextLimit(CampaignConstants.DEFAULT_CONTEXT_LIMIT)
            .withIsAloneTrafaretAllowed(CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
            .withHasTurboSmarts(CampaignConstants.DEFAULT_HAS_TURBO_SMARTS)
            .withContextPriceCoef(DEFAULT_CONTEXT_PRICE_COEF)
            .withHasSiteMonitoring(CampaignConstants.DEFAULT_HAS_SITE_MONITORING)
            .withDayBudget(CampaignConstants.DEFAULT_DAY_BUDGET)
            .withDayBudgetShowMode(CampaignConstants.DEFAULT_DAY_BUDGET_SHOW_MODE)
            .withDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY)
            .withEnableCpcHold(false)
            .withEnableCompanyInfo(true)
            .withHasTitleSubstitution(true)
    }

    private fun fillSmartCampaignSystemFields(campaign: SmartCampaign) {
        fillCommonSystemFieldsForActiveCampaign(campaign)
        campaign
            .withProductId(TestTextCampaigns.TEXT_CAMPAIGN_PRODUCT_ID)
            .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._30)
            .withEnableCheckPositionEvent(true)
            .withDayBudgetDailyChangeCount(0)
    }
}
