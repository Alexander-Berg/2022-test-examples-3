package ru.yandex.direct.grid.processing.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.time.model.TimeInterval;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdContentLanguage;
import ru.yandex.direct.grid.model.campaign.GdCpmYndxFrontpageCampaignShowType;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.GdEshowsRate;
import ru.yandex.direct.grid.model.campaign.GdEshowsSettings;
import ru.yandex.direct.grid.model.campaign.GdEshowsVideoType;
import ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignDeviceTypeTargeting;
import ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignNetworkTargeting;
import ru.yandex.direct.grid.model.campaign.GdPriceFlightTargetingsSnapshot;
import ru.yandex.direct.grid.model.campaign.GdPriceFlightViewType;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.processing.model.campaign.facelift.GdAddUpdateCampaignAdditionalData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCmpBannerCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddContentPromotionCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCpmPriceCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCpmYndxFrontpageCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddDynamicCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddMcBannerCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddMobileContentCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddSmartCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddTextCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdBroadMatchRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignEmailSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignNotificationRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignSmsSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils;

import static java.util.Collections.emptyList;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_COMPANY_INFO;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_CPC_HOLD;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_EXCLUDE_PAUSED_COMPETING_ADS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdImpressionStandardTime;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategy.DIFFERENT_PLACES;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;

public class TestGdAddCampaigns {

    public static final Long DEFAULT_CPI_GOAL_ID = 4L;
    public static final String TEST_HREF = "https://yandex.ru";
    public static final String TEST_EMAIL = "test-email@yandex.ru";

    public static GdAddTextCampaign defaultTextCampaign(CampaignAttributionModel defaultAttributionModel) {
        return new GdAddTextCampaign()
                .withName("new Camp")
                .withStartDate(LocalDate.now().plusDays(1))
                .withMetrikaCounters(emptyList())
                .withHasTitleSubstitute(false)
                .withHasSiteMonitoring(false)
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                        )
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN))))
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withSum(BigDecimal.valueOf(5000))))
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withBroadMatch(new GdBroadMatchRequest()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(5))
                .withEnableCompanyInfo(DEFAULT_ENABLE_COMPANY_INFO)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withExcludePausedCompetingAds(DEFAULT_EXCLUDE_PAUSED_COMPETING_ADS)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(TEST_HREF))
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withBrandSafetyCategories(emptyList());
    }

    public static GdAddSmartCampaign defaultSmartCampaign(List<Integer> counterIds,
                                                          CampaignAttributionModel defaultAttributionModel) {
        return defaultSmartCampaign(false, counterIds, defaultAttributionModel);
    }

    public static GdAddSmartCampaign defaultSmartCampaign(Boolean hasTurboSmarts, List<Integer> counterIds,
                                                          CampaignAttributionModel defaultAttributionModel) {
        return new GdAddSmartCampaign()
                .withName("new Smart Campaign")
                .withStartDate(LocalDate.now())
                .withMetrikaCounters(counterIds)
                .withEndDate(LocalDate.now())
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail("1@1.ru")
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(false)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(49)
                        )
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(new TimeInterval()
                                        .withStartHour(1).withStartMinute(30)
                                        .withEndHour(1).withEndMinute(15)))
                                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MODERATION))))
                .withBiddingStrategy(new GdCampaignBiddingStrategy()
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_ROI)
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withRoiCoef(BigDecimal.ONE)
                                .withReserveReturn(20L)
                                .withProfitability(BigDecimal.valueOf(20))
                                .withGoalId(0L)))
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withHasAddMetrikaTagToUrl(false)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withHasTurboSmarts(hasTurboSmarts)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(TEST_HREF))
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withBrandSafetyCategories(emptyList());
    }

    public static GdAddDynamicCampaign defaultDynamicCampaign(CampaignAttributionModel defaultAttributionModel) {
        Set<GdCampaignSmsEvent> enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);
        return new GdAddDynamicCampaign()
                .withName("new Camp")
                .withStartDate(LocalDate.now().plusDays(1))
                .withMetrikaCounters(emptyList())
                .withHasTitleSubstitute(false)
                .withHasSiteMonitoring(false)
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail(TEST_EMAIL)
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                        )
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(enableEvents)))
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withSum(BigDecimal.valueOf(5000))))
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withEnableCompanyInfo(DEFAULT_ENABLE_COMPANY_INFO)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(TEST_HREF))
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withBrandSafetyCategories(emptyList());
    }

    public static GdAddMobileContentCampaign defaultMobileContentCampaign(Long mobileAppId) {
        return new GdAddMobileContentCampaign()
                .withName("New Mobile Campaign")
                .withStartDate(LocalDate.now().plusDays(1))
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail("1@1.ru")
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                        )
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MODERATION))))
                .withBiddingStrategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withSum(BigDecimal.valueOf(5000))))
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withBrandSafetyCategories(emptyList())
                .withMobileAppId(mobileAppId)
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withDisabledPlaces(List.of("good.ru", "great.ru"))
                .withDisabledIps(List.of("1.2.3.4", "5.6.7.8"))
                .withDeviceTypeTargeting(Set.of(GdMobileContentCampaignDeviceTypeTargeting.PHONE,
                        GdMobileContentCampaignDeviceTypeTargeting.TABLET))
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(TEST_HREF))
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withNetworkTargeting(Set.of(GdMobileContentCampaignNetworkTargeting.CELLULAR,
                        GdMobileContentCampaignNetworkTargeting.WI_FI));
    }

    public static GdAddMcBannerCampaign defaultMcBannerCampaign() {
        return new GdAddMcBannerCampaign()
                .withName("new McBanner Campaign")
                .withStartDate(LocalDate.now().plusDays(1))
                .withMetrikaCounters(emptyList())
                .withHasSiteMonitoring(false)
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail("test@yandex.ru")
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                        )
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN,
                                        GdCampaignSmsEvent.MODERATION, GdCampaignSmsEvent.MONITORING))))
                .withBiddingStrategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withSum(BigDecimal.valueOf(5000))))
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(TEST_HREF))
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withBrandSafetyCategories(emptyList());
    }

    public static GdAddContentPromotionCampaign defaultContentPromotionCampaign(CampaignAttributionModel defaultAttributionModel) {
        Set<GdCampaignSmsEvent> enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);
        return new GdAddContentPromotionCampaign()
                .withName("new Camp")
                .withStartDate(LocalDate.now().plusDays(1))
                .withMetrikaCounters(null)
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail(TEST_EMAIL)
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                        )
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(enableEvents)))
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withSum(BigDecimal.valueOf(5000))))
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(TEST_HREF))
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withBrandSafetyCategories(List.of(4_294_967_297L));
    }

    public static GdAddCpmPriceCampaign defaultCpmPriceCampaign(LocalDate startDate,
                                                                LocalDate endDate,
                                                                Long pricePackageId,
                                                                CampaignAttributionModel defaultAttributionModel) {
        Set<GdCampaignSmsEvent> enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);
        return new GdAddCpmPriceCampaign()
                .withName("New Cpm Price Campaign")
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withMetrikaCounters(emptyList())
                .withHasAddMetrikaTagToUrl(true)
                .withHasAddOpenstatTagToUrl(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail(TEST_EMAIL)
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE))
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(enableEvents)))
                .withContentLanguage(GdContentLanguage.RU)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withPricePackageId(pricePackageId)
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(TEST_HREF))
                .withFlightOrderVolume(50000L)
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withFlightTargetingsSnapshot(new GdPriceFlightTargetingsSnapshot()
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(List.of(GdPriceFlightViewType.DESKTOP, GdPriceFlightViewType.MOBILE,
                                GdPriceFlightViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true));

    }

    public static GdAddCmpBannerCampaign defaultCpmBannerCampaign(LocalDate startDate, LocalDate endDate,
                                                                  CampaignAttributionModel defaultAttributionModel) {
        var enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);
        var cpmBannerCampaign = new GdAddCmpBannerCampaign()
                .withName("New Cpm Banner Campaign")
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withMetrikaCounters(emptyList())
                .withHasAddMetrikaTagToUrl(true)
                .withHasAddOpenstatTagToUrl(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail(TEST_EMAIL)
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE))
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(enableEvents)))
                .withContentLanguage(GdContentLanguage.RU)
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(TEST_HREF))
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withImpressionStandardTime(toGdImpressionStandardTime(CampaignConstants.DEFAULT_IMPRESSION_STANDARD_TIME))
                .withHasExtendedGeoTargeting(false)
                .withDayBudget(new BigDecimal(400))
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withHasSiteMonitoring(false)
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.CONTEXT)
                        .withStrategy(DIFFERENT_PLACES)
                        .withStrategyName(GdCampaignStrategyName.CPM_DEFAULT)
                        .withStrategyData(new GdCampaignStrategyData())
                )
                .withEshowsSettings(
                        new GdEshowsSettings()
                                .withBannerRate(GdEshowsRate.OFF)
                                .withVideoRate(GdEshowsRate.ON)
                                .withVideoType(GdEshowsVideoType.COMPLETES)
                );
        return cpmBannerCampaign;
    }

    public static GdAddCpmYndxFrontpageCampaign defaultCpmYndxFrontpageCampaign(LocalDate startDate,
                                                                                LocalDate endDate,
                                                                                CampaignAttributionModel defaultAttributionModel) {
        var enableEvents = Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN);
        return new GdAddCpmYndxFrontpageCampaign()
                .withName("New Cpm Yndx Frontpage Campaign")
                .withStartDate(startDate)
                .withAllowedFrontpageType(ImmutableSet.of(GdCpmYndxFrontpageCampaignShowType.FRONTPAGE))
                .withEndDate(endDate)
                .withMetrikaCounters(emptyList())
                .withHasSiteMonitoring(false)
                .withHasAddMetrikaTagToUrl(true)
                .withHasAddOpenstatTagToUrl(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail(TEST_EMAIL)
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE))
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(enableEvents)))
                .withContentLanguage(GdContentLanguage.RU)
                .withStrategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.CONTEXT)
                        .withStrategy(GdCampaignStrategy.DIFFERENT_PLACES)
                        .withStrategyName(GdCampaignStrategyName.CPM_DEFAULT)
                        .withStrategyData(new GdCampaignStrategyData()
                        ))
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(TEST_HREF))
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel));
    }

}
