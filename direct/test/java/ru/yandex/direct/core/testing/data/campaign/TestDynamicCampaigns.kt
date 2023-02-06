package ru.yandex.direct.core.testing.data.campaign

import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonClientFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForActiveCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForDraftCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultStrategy
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.TEXT_CAMPAIGN_PRODUCT_ID
import ru.yandex.direct.test.utils.RandomNumberUtils

object TestDynamicCampaigns {

    @JvmStatic
    fun clientDynamicCampaign(): DynamicCampaign {
        val campaign = DynamicCampaign()
        fillDynamicCampaignClientFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDynamicCampaign(): DynamicCampaign {
        val campaign = clientDynamicCampaign()
        fillDynamicCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftDynamicCampaign(): DynamicCampaign {
        val campaign = fullDynamicCampaign()

        fillCommonSystemFieldsForDraftCampaign(campaign)
        return campaign
    }

    private fun fillDynamicCampaignClientFields(campaign: DynamicCampaign) {
        fillCommonClientFields(campaign)
        campaign
            .withType(CampaignType.DYNAMIC)
            .withPlacementTypes(emptySet())
            .withMetrikaCounters(emptyList())
            .withName("default name " + RandomNumberUtils.nextPositiveLong())
            .withHasTitleSubstitution(false)
            .withHasAddOpenstatTagToUrl(false)
            .withHasAddMetrikaTagToUrl(false)
            .withHasExtendedGeoTargeting(false)
            .withUseCurrentRegion(false)
            .withUseRegularRegion(false)
            .withEnableCompanyInfo(false)
            .withEnablePausedByDayBudgetEvent(false)
            .withEnableCpcHold(CampaignConstants.DEFAULT_ENABLE_CPC_HOLD)
            .withStrategy(
                defaultStrategy()
                    // ДО бывают только в сетях
                    .withPlatform(CampaignsPlatform.SEARCH) as DbStrategy
            )
            .withEnableCheckPositionEvent(true)
            .withEnableOfflineStatNotice(false)
            .withIsAloneTrafaretAllowed(CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
            .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._30)
            .withHasSiteMonitoring(true)
            .withDayBudget(CampaignConstants.DEFAULT_DAY_BUDGET)
            .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
            .withDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY)
    }

    private fun fillDynamicCampaignSystemFields(campaign: DynamicCampaign) {
        fillCommonSystemFieldsForActiveCampaign(campaign)
        campaign
            .withProductId(TEXT_CAMPAIGN_PRODUCT_ID)
            .withDayBudgetDailyChangeCount(0)
    }
}
