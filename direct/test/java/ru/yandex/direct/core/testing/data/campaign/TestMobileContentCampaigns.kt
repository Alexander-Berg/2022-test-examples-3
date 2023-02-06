package ru.yandex.direct.core.testing.data.campaign

import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.DEFAULT_CONTEXT_PRICE_COEF
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForDraftCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultStrategy
import ru.yandex.direct.test.utils.RandomNumberUtils
import java.util.EnumSet

object TestMobileContentCampaigns {

    @JvmStatic
    fun clientMobileContentCampaign(mobileAppId: Long?): MobileContentCampaign {
        val campaign = MobileContentCampaign()
            .withNetworkTargeting(EnumSet.of(MobileAppNetworkTargeting.WI_FI))
            .withDeviceTypeTargeting(EnumSet.of(MobileAppDeviceTypeTargeting.PHONE))
            .withMobileAppId(mobileAppId)
        fillMobileContentCampaignClientFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullMobileContentCampaign(mobileAppId: Long?): MobileContentCampaign {
        val campaign = clientMobileContentCampaign(mobileAppId)
        fillMobileContentCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftMobileContentCampaign(mobileAppId: Long?): MobileContentCampaign {
        val campaign = fullMobileContentCampaign(mobileAppId)

        fillCommonSystemFieldsForDraftCampaign(campaign)
        return campaign
    }

    private fun fillMobileContentCampaignClientFields(campaign: MobileContentCampaign) {
        TestCampaigns.fillCommonClientFields(campaign)
        campaign
            .withType(CampaignType.MOBILE_CONTENT)
            .withName("default mobile content campaign name " + RandomNumberUtils.nextPositiveLong())
            .withStrategy(defaultStrategy())
            .withHasExtendedGeoTargeting(false)
            .withUseCurrentRegion(false)
            .withUseRegularRegion(false)
            .withEnablePausedByDayBudgetEvent(false)
            .withEnableSendAccountNews(false)
            .withEnableOfflineStatNotice(false)
            .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._30)
            .withEnableCheckPositionEvent(true)
            .withContextLimit(CampaignConstants.DEFAULT_CONTEXT_LIMIT)
            .withEnableCpcHold(CampaignConstants.DEFAULT_ENABLE_CPC_HOLD)
            .withIsAloneTrafaretAllowed(CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
            .withDayBudget(CampaignConstants.DEFAULT_DAY_BUDGET)
            .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
            .withDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY)
            .withContextPriceCoef(DEFAULT_CONTEXT_PRICE_COEF)
    }

    private fun fillMobileContentCampaignSystemFields(campaign: MobileContentCampaign) {
        TestCampaigns.fillCommonSystemFieldsForActiveCampaign(campaign)
        campaign
            .withProductId(TestTextCampaigns.TEXT_CAMPAIGN_PRODUCT_ID)
            .withNetworkTargeting(EnumSet.of(MobileAppNetworkTargeting.WI_FI))
            .withDeviceTypeTargeting(EnumSet.of(MobileAppDeviceTypeTargeting.PHONE))
            .withDayBudgetDailyChangeCount(0)
            .withIsInstalledApp(true)
    }
}
