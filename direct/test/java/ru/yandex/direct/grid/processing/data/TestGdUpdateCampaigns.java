package ru.yandex.direct.grid.processing.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.time.model.TimeInterval;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignDeviceTypeTargeting;
import ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignNetworkTargeting;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignEmailSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignNotificationRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignSmsSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateDynamicCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateMcBannerCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateMobileContentCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateSmartCampaign;
import ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils;

import static java.util.Collections.emptyList;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_COMPANY_INFO;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_CPC_HOLD;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;

public class TestGdUpdateCampaigns {

    public static GdUpdateDynamicCampaign defaultDynamicCampaign(Long campaignId,
                                                                 CampaignAttributionModel defaultAttributionModel) {
        return new GdUpdateDynamicCampaign()
                .withId(campaignId)
                .withName("new Dynamic Campaign")
                .withStartDate(LocalDate.now().plusDays(1))
                .withMetrikaCounters(emptyList())
                .withHasTitleSubstitute(false)
                .withHasSiteMonitoring(false)
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail("test@yandex.ru")
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
                        .withPlatform(GdCampaignPlatform.BOTH)
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
                .withBrandSafetyCategories(emptyList());
    }

    public static GdUpdateSmartCampaign defaultSmartCampaign(Long campaignId, List<Integer> counterIds,
                                                             CampaignAttributionModel defaultAttributionModel) {
        return defaultSmartCampaign(campaignId, false, counterIds, defaultAttributionModel);
    }

    public static GdUpdateSmartCampaign defaultSmartCampaign(Long campaignId,
                                                             Boolean hasTurboSmarts,
                                                             List<Integer> counterIds,
                                                             CampaignAttributionModel defaultAttributionModel) {
        return new GdUpdateSmartCampaign()
                .withId(campaignId)
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
                .withBrandSafetyCategories(emptyList());
    }

    public static GdUpdateMobileContentCampaign defaultMobileContentCampaign(Long campaignId,
                                                                             Long mobileAppId) {
        return new GdUpdateMobileContentCampaign()
                .withId(campaignId)
                .withName("Mobile Campaign")
                .withStartDate(LocalDate.now().plusDays(2))
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail("mail@ya.ru")
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
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withSum(BigDecimal.valueOf(5500))))
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withBrandSafetyCategories(emptyList())
                .withMobileAppId(mobileAppId)
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withDeviceTypeTargeting(Set.of(GdMobileContentCampaignDeviceTypeTargeting.TABLET))
                .withNetworkTargeting(Set.of(GdMobileContentCampaignNetworkTargeting.WI_FI))
                .withIsSkadNetworkEnabled(null);
    }

    public static GdUpdateMcBannerCampaign defaultMcBannerCampaign(Long campaignId) {
        return new GdUpdateMcBannerCampaign()
                .withId(campaignId)
                .withName("updated McBanner Campaign")
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
                .withBrandSafetyCategories(emptyList());
    }
}
