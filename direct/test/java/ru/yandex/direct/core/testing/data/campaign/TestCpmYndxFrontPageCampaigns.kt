package ru.yandex.direct.core.testing.data.campaign

import com.google.common.collect.ImmutableSet
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.ContentLanguage
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonClientFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForActiveCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForDraftCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultCpmStrategyData
import ru.yandex.direct.libs.timetarget.TimeTargetUtils.timeTarget24x7
import ru.yandex.direct.regions.Region

object TestCpmYndxFrontPageCampaigns {
    const val CPM_YNDX_FRONTPAGE_CAMPAIGN_PRODUCT_ID = 509641L

    @JvmStatic
    fun clientCpmYndxFrontpageCampaign(): CpmYndxFrontpageCampaign {
        val campaign = CpmYndxFrontpageCampaign()
        fillCpmYndxFrontpageCampaignClientFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullCpmYndxFrontpageCampaign(): CpmYndxFrontpageCampaign {
        val campaign = clientCpmYndxFrontpageCampaign()
        fillCpmYndxFrontpageCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftCpmYndxFrontpageCampaign(): CpmYndxFrontpageCampaign {
        val campaign = fullCpmYndxFrontpageCampaign()

        fillCommonSystemFieldsForDraftCampaign(campaign)
        return campaign
    }

    private fun fillCpmYndxFrontpageCampaignClientFields(campaign: CpmYndxFrontpageCampaign) {
        fillCommonClientFields(campaign)
        campaign
            .withType(CampaignType.CPM_YNDX_FRONTPAGE)
            .withAllowedFrontpageType(ImmutableSet.of(FrontpageCampaignShowType.FRONTPAGE))
            .withName("Campaign name")
            .withStrategy(defaultCpmStrategyData())
            .withMetrikaCounters(null)
            .withHasSiteMonitoring(CampaignConstants.DEFAULT_HAS_SITE_MONITORING)
            .withHasAddMetrikaTagToUrl(CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL)
            .withHasAddOpenstatTagToUrl(CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
            .withContentLanguage(ContentLanguage.RU)
            .withAttributionModel(CampaignAttributionModel.LAST_SIGNIFICANT_CLICK)
            .withEnableOfflineStatNotice(false)
            .withGeo(setOf(Region.MOSCOW_REGION_ID.toInt(), Region.SAINT_PETERSBURG_REGION_ID.toInt()))
            .withTimeTarget(timeTarget24x7())
            .withBrandSafetyCategories(emptyList())
            .withMeasurers(emptyList())
            .withEnablePausedByDayBudgetEvent(false)
            .withHasTitleSubstitution(false)
            .withHasExtendedGeoTargeting(false)
            .withUseCurrentRegion(false)
            .withUseRegularRegion(false)
            .withEnableCompanyInfo(false)
            .withEnableCpcHold(false)
            .withHasTurboApp(false)
            .withIsVirtual(false)
            .withIsCpmGlobalAbSegment(false)
            .withIsSearchLiftEnabled(false)
            .withIsBrandLiftHidden(false)
            .withDayBudget(CampaignConstants.DEFAULT_DAY_BUDGET)
            .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
    }

    private fun fillCpmYndxFrontpageCampaignSystemFields(campaign: CpmYndxFrontpageCampaign) {
        fillCommonSystemFieldsForActiveCampaign(campaign)
        campaign
            .withProductId(CPM_YNDX_FRONTPAGE_CAMPAIGN_PRODUCT_ID)
            .withDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY)
            .withDayBudgetDailyChangeCount(0)
    }
}
