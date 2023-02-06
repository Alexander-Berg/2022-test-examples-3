package ru.yandex.direct.core.testing.data.campaign

import ru.yandex.direct.common.util.RepositoryUtils.NOW_PLACEHOLDER
import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.testing.data.TestCampaigns.DEFAULT_CONTEXT_PRICE_COEF
import ru.yandex.direct.core.testing.data.TestCampaigns.TEXT_CAMPAIGN_PRODUCT_ID
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultStrategy
import ru.yandex.direct.test.utils.RandomNumberUtils

object TestMcBannerCampaigns {

    @JvmStatic
    fun clientMcBannerCampaign(): McBannerCampaign {
        val campaign = McBannerCampaign()
        fillMcBannerCampaignClientFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullMcBannerCampaign(): McBannerCampaign {
        val campaign = clientMcBannerCampaign()
        fillMcBannerCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftMcBannerCampaign(): McBannerCampaign {
        val campaign = fullMcBannerCampaign()

        TestCampaigns.fillCommonSystemFieldsForDraftCampaign(campaign)
        return campaign
    }

    private fun fillMcBannerCampaignClientFields(campaign: McBannerCampaign) {
        TestCampaigns.fillCommonClientFields(campaign)
        campaign
            .withMetrikaCounters(emptyList())
            .withName("default mcbanner campaign " + RandomNumberUtils.nextPositiveLong())
            .withType(CampaignType.MCBANNER)
            .withHasTitleSubstitution(false)
            .withHasAddOpenstatTagToUrl(false)
            .withHasAddMetrikaTagToUrl(false)
            .withHasExtendedGeoTargeting(false)
            .withUseCurrentRegion(false)
            .withUseRegularRegion(false)
            .withEnableCompanyInfo(false)
            .withStrategy(defaultStrategy()
                .withPlatform(CampaignsPlatform.SEARCH)
                .withStrategy(null) as DbStrategy)
            .withEnableOfflineStatNotice(false)
            .withHasSiteMonitoring(true)
            .withEnablePausedByDayBudgetEvent(false)
            .withContextLimit(CampaignConstants.DEFAULT_CONTEXT_LIMIT)
            .withContextPriceCoef(DEFAULT_CONTEXT_PRICE_COEF)
            .withEnableCpcHold(CampaignConstants.DEFAULT_ENABLE_CPC_HOLD)
            .withDayBudget(CampaignConstants.DEFAULT_DAY_BUDGET)
            .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
            .withDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY)
            .withBrandSafetyCategories(emptyList())
            .withMinusKeywords(emptyList())
    }

    private fun fillMcBannerCampaignSystemFields(campaign: McBannerCampaign) {
        TestCampaigns.fillCommonSystemFieldsForActiveCampaign(campaign)
        campaign
            .withProductId(TEXT_CAMPAIGN_PRODUCT_ID)
            .withDayBudgetDailyChangeCount(0)
            .withDayBudgetLastChange(NOW_PLACEHOLDER)
            .withIsOrderPhraseLengthPrecedenceEnabled(false)
            .withAutobudgetForecastDate(null)
    }
}
