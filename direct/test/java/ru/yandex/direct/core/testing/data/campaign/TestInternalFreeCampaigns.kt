package ru.yandex.direct.core.testing.data.campaign

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.InternalCampaignRestrictionType
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignWithForbiddenStrategyAddOperationSupport
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonClientFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForActiveCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForDraftCampaign
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils
import ru.yandex.direct.libs.timetarget.TimeTargetUtils

object TestInternalFreeCampaigns {
    const val INTERNAL_FREE_CAMPAIGN_PRODUCT_ID = 509693L

    @JvmStatic
    fun clientInternalFreeCampaign(): InternalFreeCampaign {
        val campaign = InternalFreeCampaign()
        fillInternalFreeCampaignClientFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullInternalFreeCampaign(): InternalFreeCampaign {
        val campaign = clientInternalFreeCampaign()
        fillInternalFreeCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftInternalAutobudgetCampaign(): InternalFreeCampaign {
        val campaign = fullInternalFreeCampaign()

        fillCommonSystemFieldsForDraftCampaign(campaign)
        return campaign
    }

    private fun fillInternalFreeCampaignClientFields(campaign: InternalFreeCampaign) {
        fillCommonClientFields(campaign)
        campaign
            .withType(CampaignType.INTERNAL_FREE)
            .withRestrictionValue(123L)
            .withRestrictionType(InternalCampaignRestrictionType.CLICKS)
            .withName("Internal Free campaign name")
            .withStrategy(CampaignWithForbiddenStrategyAddOperationSupport.defaultStrategy())
            .withEnableOfflineStatNotice(false)
            .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._15)
            .withTimeTarget(TimeTargetUtils.timeTarget24x7())
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

    private fun fillInternalFreeCampaignSystemFields(campaign: InternalFreeCampaign) {
        fillCommonSystemFieldsForActiveCampaign(campaign)
        campaign
            .withProductId(INTERNAL_FREE_CAMPAIGN_PRODUCT_ID)
            .withEnableCpcHold(false)
            .withHasTitleSubstitution(false)
            .withEnableCompanyInfo(false)
            .withIsVirtual(false)
            .withHasTurboApp(false)
    }
}
