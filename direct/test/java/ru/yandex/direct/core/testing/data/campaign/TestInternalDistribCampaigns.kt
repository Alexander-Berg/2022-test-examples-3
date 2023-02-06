package ru.yandex.direct.core.testing.data.campaign

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignWithForbiddenStrategyAddOperationSupport
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonClientFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForActiveCampaign
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils
import ru.yandex.direct.libs.timetarget.TimeTargetUtils

object TestInternalDistribCampaigns {
    const val INTERNAL_DISTRIB_CAMPAIGN_PRODUCT_ID = 509692L

    @JvmStatic
    fun clientInternalDistribCampaign(): InternalDistribCampaign {
        val campaign = InternalDistribCampaign()
        fillInternalDistribCampaignClientFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullInternalDistribCampaign(): InternalDistribCampaign {
        val campaign = clientInternalDistribCampaign()
        fillInternalDistribCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftInternalDistribCampaign(): InternalDistribCampaign {
        val campaign = fullInternalDistribCampaign()

        TestCampaigns.fillCommonSystemFieldsForDraftCampaign(campaign)
        return campaign
    }

    private fun fillInternalDistribCampaignClientFields(campaign: InternalDistribCampaign) {
        fillCommonClientFields(campaign)
        campaign
            .withType(CampaignType.INTERNAL_DISTRIB)
            .withRotationGoalId(12345L)
            .withName("Internal Distrib campaign name")
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

    private fun fillInternalDistribCampaignSystemFields(campaign: InternalDistribCampaign) {
        fillCommonSystemFieldsForActiveCampaign(campaign)
        campaign
            .withProductId(INTERNAL_DISTRIB_CAMPAIGN_PRODUCT_ID)
            .withEnableCpcHold(false)
            .withHasTitleSubstitution(false)
            .withEnableCompanyInfo(false)
            .withIsVirtual(false)
            .withHasTurboApp(false)
    }
}
