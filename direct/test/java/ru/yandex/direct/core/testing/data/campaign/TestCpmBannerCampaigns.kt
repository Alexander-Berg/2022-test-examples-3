package ru.yandex.direct.core.testing.data.campaign

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.EshowsSettings
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_DAY_BUDGET
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonClientFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForActiveCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForDraftCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultCpmStrategyData
import ru.yandex.direct.test.utils.RandomNumberUtils

object TestCpmBannerCampaigns {
    const val CPM_BANNER_CAMPAIGN_PRODUCT_ID = 508587L

    @JvmStatic
    fun clientCpmBannerCampaign(): CpmBannerCampaign {
        val campaign = CpmBannerCampaign()
        fillCpmBannerCampaignClientFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullCpmBannerCampaign(): CpmBannerCampaign {
        val campaign = clientCpmBannerCampaign()
        fillCpmBannerCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftCpmBannerCampaign(): CpmBannerCampaign {
        val campaign = fullCpmBannerCampaign()

        fillCommonSystemFieldsForDraftCampaign(campaign)
        return campaign
    }

    private fun fillCpmBannerCampaignClientFields(campaign: CpmBannerCampaign) {
        fillCommonClientFields(campaign)
        campaign
            .withType(CampaignType.CPM_BANNER)
            .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
            .withMetrikaCounters(emptyList())
            .withName("default name " + RandomNumberUtils.nextPositiveLong())
            .withHasAddOpenstatTagToUrl(false)
            .withHasAddMetrikaTagToUrl(false)
            .withEnableOfflineStatNotice(false)
            .withHasSiteMonitoring(true)
            .withDayBudget(DEFAULT_DAY_BUDGET)
            .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
            .withStrategy(defaultCpmStrategyData())
            .withHasExtendedGeoTargeting(false)
            .withUseCurrentRegion(false)
            .withUseRegularRegion(false)
            .withEnablePausedByDayBudgetEvent(false)
    }

    private fun fillCpmBannerCampaignSystemFields(campaign: CpmBannerCampaign) {
        fillCommonSystemFieldsForActiveCampaign(campaign)
        campaign
            .withProductId(CPM_BANNER_CAMPAIGN_PRODUCT_ID)
            .withIsServiceRequested(false)
            .withDayBudgetDailyChangeCount(0)
            .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._60)
            .withEnableCheckPositionEvent(false)
            .withDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY)
            .withEshowsSettings(EshowsSettings().withVideoType(EshowsVideoType.COMPLETES))
    }
}
