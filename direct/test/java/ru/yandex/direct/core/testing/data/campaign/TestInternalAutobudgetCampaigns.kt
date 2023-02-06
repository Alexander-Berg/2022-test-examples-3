package ru.yandex.direct.core.testing.data.campaign

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonClientFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForActiveCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetStrategy
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils
import ru.yandex.direct.libs.timetarget.TimeTargetUtils.timeTarget24x7

object TestInternalAutobudgetCampaigns {

    const val INTERNAL_AUTOBUDGET_CAMPAIGN_PRODUCT_ID = 511422L

    @JvmStatic
    fun clientInternalAutobudgetCampaign(): InternalAutobudgetCampaign {
        val campaign = InternalAutobudgetCampaign()
        fillInternalAutobudgetCampaignClientFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullInternalAutobudgetCampaign(): InternalAutobudgetCampaign {
        val campaign = clientInternalAutobudgetCampaign()
        fillInternalAutobudgetCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftInternalAutobudgetCampaign(): InternalAutobudgetCampaign {
        val campaign = fullInternalAutobudgetCampaign()

        TestCampaigns.fillCommonSystemFieldsForDraftCampaign(campaign)
        return campaign
    }

    private fun fillInternalAutobudgetCampaignClientFields(campaign: InternalAutobudgetCampaign) {
        fillCommonClientFields(campaign)
        campaign
            .withType(CampaignType.INTERNAL_AUTOBUDGET)
            .withName("Internal Autobudget campaign name")
            .withEnableOfflineStatNotice(false)
            .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._15)
            .withTimeTarget(timeTarget24x7())
            .withStrategy(defaultAutobudgetStrategy())
            .withMetrikaCounters(emptyList())
            .withMeaningfulGoals(emptyList())
            .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
            .withIsMobile(false)
            .withPlaceId(TemplatePlaceRepositoryMockUtils.PLACE_1)
            .withPageId(emptyList())
            .withImpressionRateIntervalDays(1)
            .withImpressionRateCount(100)
            .withEnablePausedByDayBudgetEvent(false)
            .withEnableCheckPositionEvent(false)
            .withHasExtendedGeoTargeting(false)
            .withUseCurrentRegion(false)
            .withUseRegularRegion(false)
    }

    private fun fillInternalAutobudgetCampaignSystemFields(campaign: InternalAutobudgetCampaign) {
        fillCommonSystemFieldsForActiveCampaign(campaign)
        campaign
            .withProductId(INTERNAL_AUTOBUDGET_CAMPAIGN_PRODUCT_ID)
            .withEnableCpcHold(false)
            .withHasTitleSubstitution(false)
            .withEnableCompanyInfo(false)
            .withIsVirtual(false)
            .withHasTurboApp(false)

    }
}
