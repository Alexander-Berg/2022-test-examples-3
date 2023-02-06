package ru.yandex.direct.core.testing.data.campaign

import ru.yandex.direct.common.util.RepositoryUtils.NOW_PLACEHOLDER
import ru.yandex.direct.core.entity.campaign.model.BroadMatch
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonClientFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForActiveCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForDraftCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultStrategy
import ru.yandex.direct.test.utils.RandomNumberUtils

object TestTextCampaigns {
    const val TEXT_CAMPAIGN_PRODUCT_ID = 503162L

    @JvmStatic
    fun clientTextCampaign(): TextCampaign {
        val campaign = TextCampaign()
        fillTextCampaignClientFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullTextCampaign(): TextCampaign {
        val campaign = clientTextCampaign()
        fillTextCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftTextCampaign(): TextCampaign {
        val campaign = fullTextCampaign()

        fillCommonSystemFieldsForDraftCampaign(campaign)
        return campaign
    }

    private fun fillTextCampaignClientFields(campaign: TextCampaign) {
        fillCommonClientFields(campaign)
        campaign
            .withMetrikaCounters(listOf())
            .withName("default name " + RandomNumberUtils.nextPositiveLong())
            .withType(CampaignType.TEXT)
            .withPlacementTypes(emptySet())
            .withHasTitleSubstitution(false)
            .withHasAddOpenstatTagToUrl(false)
            .withExcludePausedCompetingAds(false)
            .withHasAddMetrikaTagToUrl(false)
            .withHasExtendedGeoTargeting(false)
            .withUseCurrentRegion(false)
            .withUseRegularRegion(false)
            .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
            .withBroadMatch(BroadMatch()
                .withBroadMatchFlag(true)
                .withBroadMatchLimit(5))
            .withEnableCompanyInfo(false)
            .withStrategy(defaultStrategy())
            .withEnableOfflineStatNotice(false)
            .withEnablePausedByDayBudgetEvent(false)
            .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._30)
            .withEnableCheckPositionEvent(true)
            .withHasSiteMonitoring(true)
            .withEnableSendAccountNews(false)
            .withContextLimit(CampaignConstants.DEFAULT_CONTEXT_LIMIT)
            .withEnableCpcHold(CampaignConstants.DEFAULT_ENABLE_CPC_HOLD)
            .withIsAloneTrafaretAllowed(CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
            .withDayBudget(CampaignConstants.DEFAULT_DAY_BUDGET)
            .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
            .withDayBudgetDailyChangeCount(0)
            .withDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY)
            .withDayBudgetLastChange(NOW_PLACEHOLDER)
            .withContextPriceCoef(TestCampaigns.DEFAULT_CONTEXT_PRICE_COEF)
    }

    private fun fillTextCampaignSystemFields(campaign: TextCampaign) {
        fillCommonSystemFieldsForActiveCampaign(campaign)

        campaign.productId = TEXT_CAMPAIGN_PRODUCT_ID
    }
}
