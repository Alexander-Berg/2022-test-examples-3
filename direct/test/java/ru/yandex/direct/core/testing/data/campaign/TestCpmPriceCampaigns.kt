package ru.yandex.direct.core.testing.data.campaign

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel.LAST_SIGNIFICANT_CLICK
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval
import ru.yandex.direct.core.entity.campaign.model.ContentLanguage
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect
import ru.yandex.direct.core.entity.campaign.model.PriceFlightTargetingsSnapshot
import ru.yandex.direct.core.entity.campaign.service.CampaignWithPricePackageUtils
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsWithGeo
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonClientFields
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForActiveCampaign
import ru.yandex.direct.core.testing.data.campaign.TestCampaigns.fillCommonSystemFieldsForDraftCampaign
import ru.yandex.direct.libs.timetarget.TimeTargetUtils
import ru.yandex.direct.utils.ListUtils
import java.math.BigDecimal

object TestCpmPriceCampaigns {
    const val CPM_PRICE_CAMPAIGN_PRODUCT_ID = 509998L

    @JvmStatic
    fun clientCpmPriceCampaign(pricePackage: PricePackage?): CpmPriceCampaign {
        val campaign = CpmPriceCampaign()
        fillCpmPriceCampaignClientFields(campaign)

        if (pricePackage != null) {
            enrichCampaignWithPricePackage(campaign, pricePackage)
        }
        return campaign
    }

    @JvmStatic
    fun fullCpmPriceCampaign(pricePackage: PricePackage?): CpmPriceCampaign {
        val campaign = clientCpmPriceCampaign(pricePackage)
        fillCpmPriceCampaignSystemFields(campaign)
        return campaign
    }

    @JvmStatic
    fun fullDraftCpmPriceCampaign(pricePackage: PricePackage?): CpmPriceCampaign {
        val campaign = fullCpmPriceCampaign(pricePackage)

        fillCommonSystemFieldsForDraftCampaign(campaign)
        campaign
            .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
            .withFlightStatusCorrect(PriceFlightStatusCorrect.NEW)
        return campaign
    }

    fun enrichCampaignWithPricePackage(campaign: CpmPriceCampaign, pricePackage: PricePackage) {
        val flightOrderVolume: Long = (pricePackage.orderVolumeMin + pricePackage.orderVolumeMax) / 2
        // в качестве гео-таргетингов выбираем таргетинги, в которых geo непустой
        val targetingsWithGeo: TargetingsWithGeo = if (pricePackage.targetingsFixed.geo != null)
            pricePackage.targetingsFixed else pricePackage.targetingsCustom
        campaign
            .withStartDate(pricePackage.dateStart)
            .withEndDate(pricePackage.dateEnd)
            .withPricePackageId(pricePackage.id)
            // Этого не достаточно нужна ещё логика из CampaignWithGeoAddOperationSupport
            // К примеру 225L, должно конвертироваться в 225L, 977L (добавляется Крым)
            .withGeo(if (targetingsWithGeo.geoExpanded == null) null
            else ListUtils.longToIntegerSet(targetingsWithGeo.geoExpanded))
            .withFlightTargetingsSnapshot(PriceFlightTargetingsSnapshot()
                .withGeoType(targetingsWithGeo.geoType)
                .withGeoExpanded(targetingsWithGeo.geoExpanded)
                .withViewTypes(pricePackage.targetingsFixed.viewTypes)
                .withAllowExpandedDesktopCreative(
                    pricePackage.targetingsFixed.allowExpandedDesktopCreative))
            .withFlightOrderVolume(flightOrderVolume)
            .withIsDraftApproveAllowed(pricePackage.isDraftApproveAllowed)
        campaign.withStrategy(CampaignWithPricePackageUtils.getStrategy(campaign, pricePackage, false))
    }

    private fun fillCpmPriceCampaignClientFields(campaign: CpmPriceCampaign) {
        fillCommonClientFields(campaign)
        campaign
            .withType(CampaignType.CPM_PRICE)
            .withName("Campaign name")
            .withMetrikaCounters(null)
            .withHasAddMetrikaTagToUrl(CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL)
            .withHasAddOpenstatTagToUrl(CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
            .withContentLanguage(ContentLanguage.RU)
            .withAttributionModel(LAST_SIGNIFICANT_CLICK)
            .withFio("FIO")
            .withEnableSendAccountNews(false)
            .withEnablePausedByDayBudgetEvent(false)
            .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._30)
            .withEnableOfflineStatNotice(false)
            .withEnableCheckPositionEvent(true)
            .withFlightReasonIncorrect(null)
            .withTimeTarget(TimeTargetUtils.timeTarget24x7())
            .withBrandSafetyCategories(emptyList())
            .withRequireFiltrationByDontShowDomains(false)
            .withHasTitleSubstitution(false)
            .withHasExtendedGeoTargeting(false)
            .withUseCurrentRegion(false)
            .withUseRegularRegion(false)
            .withEnableCompanyInfo(false)
            .withEnableCpcHold(false)
            .withHasTurboApp(false)
            .withIsVirtual(false)
            .withIsAllowedOnAdultContent(false)
            .withMeasurers(emptyList())
            .withIsCpmGlobalAbSegment(false)
            .withIsSearchLiftEnabled(false)
            .withIsBrandLiftHidden(false)
    }

    private fun fillCpmPriceCampaignSystemFields(campaign: CpmPriceCampaign) {
        fillCommonSystemFieldsForActiveCampaign(campaign)
        campaign
            .withProductId(CPM_PRICE_CAMPAIGN_PRODUCT_ID)
            .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._60)
            .withFlightStatusApprove(PriceFlightStatusApprove.YES)
            .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
            .withFlightReasonIncorrect(null)
            .withRequireFiltrationByDontShowDomains(false)
            .withAutobudgetForecastDate(null)
            .withStatusShow(false)
            .withSum(BigDecimal.valueOf(100L))
            .withSumToPay(BigDecimal.valueOf(100L))
            .withSumLast(BigDecimal.valueOf(100L))
            .withOrderId(100L)
            .withSumSpent(BigDecimal.valueOf(100L))
    }
}
