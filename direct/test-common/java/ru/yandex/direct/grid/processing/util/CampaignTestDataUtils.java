package ru.yandex.direct.grid.processing.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdCampaignAccess;
import ru.yandex.direct.grid.model.campaign.GdCampaignAction;
import ru.yandex.direct.grid.model.campaign.GdCampaignAttributionModel;
import ru.yandex.direct.grid.model.campaign.GdCampaignFeature;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus;
import ru.yandex.direct.grid.model.campaign.GdCampaignStatus;
import ru.yandex.direct.grid.model.campaign.GdCampaignStatusModerate;
import ru.yandex.direct.grid.model.campaign.GdCampaignTruncated;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.GdInternalFreeCampaignRestrictionType;
import ru.yandex.direct.grid.model.campaign.GdPriceCampaign;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudget;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudgetPeriod;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyAvgCpa;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyManual;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyPeriodFixBid;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyType;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyType;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilter;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderBy;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderByField;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddAbstractInternalCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddDynamicCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddInternalAutobudgetCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddInternalDistribCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddInternalFreeCampaign;
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
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdMeaningfulGoalRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateAbstractInternalCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateDynamicCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateInternalAutobudgetCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateInternalDistribCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateInternalFreeCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateSmartCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateTextCampaign;
import ru.yandex.direct.grid.processing.model.client.GdClientAutoOverdraftInfo;
import ru.yandex.direct.grid.processing.model.strategy.query.GdPackageStrategiesContainer;
import ru.yandex.direct.grid.processing.model.strategy.query.GdPackageStrategyFilter;
import ru.yandex.direct.grid.processing.model.strategy.query.GdPackageStrategyOrderBy;
import ru.yandex.direct.grid.processing.model.strategy.query.GdPackageStrategyOrderByField;
import ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.utils.Counter;

import static java.util.Collections.emptyList;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_COMPANY_INFO;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_CPC_HOLD;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_EXCLUDE_PAUSED_COMPETING_ADS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toCampaignType;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toGdAttributionModel;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultStatRequirements;

@ParametersAreNonnullByDefault
public class CampaignTestDataUtils {

    public static final List<Integer> DEFAULT_METRIKA_COUNTERS = List.of(123);

    public static GdClientAutoOverdraftInfo defaultGdClientAutoOverdraftInfo() {
        return new GdClientAutoOverdraftInfo()
                .withDebt(BigDecimal.ZERO)
                .withStatusBalanceBanned(false)
                .withOverdraftLimit(BigDecimal.ZERO)
                .withOverdraftLimitWithNds(BigDecimal.ZERO)
                .withOverdraftRest(BigDecimal.ZERO)
                .withAutoOverdraftLimit(BigDecimal.ZERO);
    }

    public static GdCampaign defaultGdCampaign() {
        return defaultGdCampaign(RandomNumberUtils.nextPositiveLong());
    }

    public static GdCampaign defaultGdCampaign(long cid) {
        return new GdTextCampaign()
                .withId(cid)
                .withType(GdCampaignType.TEXT)
                .withFeatures(Set.of(GdCampaignFeature.HAS_NOT_ARCHIVED_ADS))
                .withAccess(defaultGdCampaignAccess())
                .withStatus(defaultGdCampaignStatus())
                .withFlatStrategy(defaultGdCampaignStrategyManual());
    }

    public static GdPriceCampaign gdPriceCampaign() {
        return new GdPriceCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withType(GdCampaignType.CPM_PRICE)
                .withAccess(defaultGdCampaignAccess())
                .withStatus(defaultGdCampaignStatus())
                .withFlatStrategy(defaulGdCampaignStrategyPeriodFixBid());
    }

    public static GdiCampaign toGdiCampaign(GdCampaign campaign) {
        return new GdiCampaign()
                .withId(campaign.getId())
                .withType(toCampaignType(campaign.getType()));
    }

    public static List<GdCampaignTruncated> generateCampaignTruncatedList(int size) {
        Counter counter = new Counter();
        return StreamEx.generate(() -> defaultGdCampaign(counter.next()))
                .limit(size)
                .map(GdCampaignTruncated.class::cast)
                .toList();
    }

    public static GdCampaignAccess defaultGdCampaignAccess() {
        return new GdCampaignAccess()
                .withActions(Collections.singleton(GdCampaignAction.EDIT_CAMP))
                .withCanEdit(true);
    }

    public static GdCampaignsContainer getDefaultCampaignsContainerInput() {
        return new GdCampaignsContainer()
                .withFilter(new GdCampaignFilter()
                        .withArchived(false))
                .withOrderBy(Collections.singletonList(new GdCampaignOrderBy()
                        .withField(GdCampaignOrderByField.ID)
                        .withOrder(Order.ASC))
                )
                .withStatRequirements(getDefaultStatRequirements())
                .withLimitOffset(getDefaultLimitOffset());
    }

    public static GdPackageStrategiesContainer getDefaultPackageStrategiesContainerInput() {
        return new GdPackageStrategiesContainer()
                .withFilter(new GdPackageStrategyFilter().withIsPublic(true))
                .withOrderBy(Collections.singletonList(new GdPackageStrategyOrderBy()
                        .withField(GdPackageStrategyOrderByField.NAME)
                        .withOrder(Order.ASC))
                )
                .withLimitOffset(getDefaultLimitOffset());
    }

    public static GdCampaignStatus defaultGdCampaignStatus() {
        return new GdCampaignStatus()
                .withReadOnly(false)
                .withOver(false)
                .withActivating(false)
                .withArchived(false)
                .withDraft(false)
                .withWaitingForUnArchiving(false)
                .withWaitingForArchiving(false)
                .withWaitingForPayment(false)
                .withNeedsNewPayment(false)
                .withMoneyBlocked(false)
                .withAllowDomainMonitoring(true)
                .withModerationStatus(GdCampaignStatusModerate.YES)
                .withPrimaryStatus(GdCampaignPrimaryStatus.ACTIVE);
    }

    public static GdCampaignStrategyManual defaultGdCampaignStrategyManual() {
        return new GdCampaignStrategyManual()
                .withType(GdCampaignStrategyType.DEFAULT)
                .withIsAutoBudget(false)
                .withPlatform(GdCampaignPlatform.BOTH)
                .withBudget(new GdCampaignBudget()
                        .withSum(BigDecimal.ZERO)
                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT)
                        .withPeriod(GdCampaignBudgetPeriod.DAY))
                .withSeparateBidding(true);
    }

    private static GdCampaignStrategyPeriodFixBid defaulGdCampaignStrategyPeriodFixBid() {
        return new GdCampaignStrategyPeriodFixBid()
                .withStrategyType(GdStrategyType.PERIOD_FIX_BID)
                .withType(GdCampaignStrategyType.PERIOD_FIX_BID)
                .withPlatform(GdCampaignPlatform.CONTEXT)
                .withBudget(new GdCampaignBudget()
                        .withSum(BigDecimal.valueOf(10000000, 3))
                        .withPeriod(GdCampaignBudgetPeriod.CUSTOM)
                        .withShowMode(GdCampaignBudgetShowMode.DEFAULT)
                        .withAutoProlongation(true))
                .withIsAutoBudget(false);
    }

    public static GdCampaignStrategyAvgCpa defaultGdCampaignStrategyAvgCpa() {
        return new GdCampaignStrategyAvgCpa()
                .withType(GdCampaignStrategyType.AVG_CPA)
                .withIsAutoBudget(true);
    }

    public static GdAddTextCampaign defaultGdAddTextCampaign(CampaignAttributionModel defaultAttributionModel) {
        return new GdAddTextCampaign()
                .withName("new Camp")
                .withStartDate(LocalDate.now().plusDays(1))
                .withMetrikaCounters(emptyList())
                .withHasTitleSubstitute(false)
                .withHasSiteMonitoring(false)
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(defaultGdTimeTarget())
                .withNotification(defaultNotification())
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
                .withExcludePausedCompetingAds(DEFAULT_EXCLUDE_PAUSED_COMPETING_ADS)
                .withBrandSafetyCategories(emptyList());
    }

    private static GdCampaignNotificationRequest defaultNotification() {
        return new GdCampaignNotificationRequest()
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
                        .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN)));
    }

    public static GdUpdateTextCampaign defaultGdUpdateTextCampaign(TextCampaign textCampaign,
                                                                   CampaignAttributionModel defaultAttributionModel) {
        return new GdUpdateTextCampaign()
                .withId(textCampaign.getId())
                .withName(textCampaign.getName())
                .withEndDate(textCampaign.getEndDate())
                .withStartDate(textCampaign.getStartDate())
                .withTimeTarget(defaultGdTimeTarget())
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategy(GdCampaignStrategy.DIFFERENT_PLACES)
                        .withStrategyData(new GdCampaignStrategyData()))
                .withDisabledPlaces(textCampaign.getDisabledDomains())
                .withDisabledIps(textCampaign.getDisabledIps())
                .withBroadMatch(new GdBroadMatchRequest()
                        .withBroadMatchFlag(false)
                        .withBroadMatchLimit(70))
                .withNotification(defaultNotification())
                .withHasTitleSubstitute(false)
                .withContentLanguage(null)
                .withAttributionModel(null)
                .withHasExtendedGeoTargeting(true)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withHasSiteMonitoring(false)
                .withClientDialogId(textCampaign.getClientDialogId())
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withEnableCompanyInfo(true)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withExcludePausedCompetingAds(false)
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel));
    }

    public static GdAddSmartCampaign defaultGdAddSmartCampaign(CampaignAttributionModel defaultAttributionModel) {
        return new GdAddSmartCampaign()
                .withName("new campaign")
                .withStartDate(LocalDate.now())
                .withContextLimit(100)
                .withTimeTarget(defaultGdTimeTarget())
                .withMetrikaCounters(DEFAULT_METRIKA_COUNTERS)
                .withBiddingStrategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withFilterAvgBid(BigDecimal.TEN)))
                .withNotification(defaultNotification())
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL);
    }

    public static GdAddDynamicCampaign defaultGdAddDynamicCampaign(CampaignAttributionModel defaultAttributionModel) {
        return new GdAddDynamicCampaign()
                .withName("new campaign")
                .withStartDate(LocalDate.now())
                .withTimeTarget(defaultGdTimeTarget())
                .withMetrikaCounters(List.of())
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withSum(BigDecimal.valueOf(5000))))
                .withNotification(defaultNotification())
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withHasExtendedGeoTargeting(false)
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withHasSiteMonitoring(false)
                .withHasTitleSubstitute(false)
                .withEnableCompanyInfo(DEFAULT_ENABLE_COMPANY_INFO)
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withDayBudget(BigDecimal.ZERO);
    }

    public static GdUpdateSmartCampaign defaultGdUpdateSmartCampaign(SmartCampaign campaign,
                                                                     CampaignAttributionModel defaultAttributionModel) {
        return new GdUpdateSmartCampaign()
                .withId(campaign.getId())
                .withName(campaign.getName())
                .withStartDate(campaign.getStartDate())
                .withContextLimit(campaign.getContextLimit())
                .withTimeTarget(defaultGdTimeTarget())
                .withMetrikaCounters(DEFAULT_METRIKA_COUNTERS)
                .withBiddingStrategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withFilterAvgBid(BigDecimal.TEN)))
                .withNotification(defaultNotification())
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL);
    }

    public static GdUpdateDynamicCampaign defaultGdUpdateDynamicCampaign(DynamicCampaign campaign,
                                                                         CampaignAttributionModel defaultAttributionModel) {
        return new GdUpdateDynamicCampaign()
                .withId(campaign.getId())
                .withName(campaign.getName())
                .withStartDate(campaign.getStartDate())
                .withEndDate(campaign.getEndDate())
                .withTimeTarget(defaultGdTimeTarget())
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withSum(BigDecimal.valueOf(5000))))
                .withNotification(defaultNotification())
                .withAttributionModel(toGdAttributionModel(defaultAttributionModel))
                .withHasAddMetrikaTagToUrl(campaign.getHasAddMetrikaTagToUrl())
                .withHasAddOpenstatTagToUrl(campaign.getHasAddOpenstatTagToUrl())
                .withHasExtendedGeoTargeting(campaign.getHasExtendedGeoTargeting())
                .withEnableCpcHold(campaign.getEnableCpcHold())
                .withHasSiteMonitoring(campaign.getHasSiteMonitoring())
                .withHasTitleSubstitute(campaign.getHasTitleSubstitution())
                .withEnableCompanyInfo(campaign.getEnableCompanyInfo())
                .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
                .withDayBudget(campaign.getDayBudget());
    }

    public static GdUpdateInternalDistribCampaign getGdUpdateInternalDistribCampaignRequest(Long campaignId) {
        return getCommonUpdateInternalCampaignRequest(campaignId, GdUpdateInternalDistribCampaign::new)
                .withName("Update distrib campaign " + RandomStringUtils.randomAlphanumeric(5))
                .withRotationGoalId(RandomNumberUtils.nextPositiveLong());
    }

    public static GdUpdateInternalAutobudgetCampaign getGdUpdateInternalAutobudgetCampaignRequest(Long campaignId) {
        return getCommonUpdateInternalCampaignRequest(campaignId, GdUpdateInternalAutobudgetCampaign::new)
                .withName("Update autobudget campaign " + RandomStringUtils.randomAlphanumeric(5))
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.SEARCH)
                        .withStrategy(GdCampaignStrategy.AUTOBUDGET_AVG_CPA_PER_FILTER)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withSum(BigDecimal.valueOf(2345))));
    }

    public static GdUpdateInternalFreeCampaign getGdUpdateInternalFreeCampaignRequest(Long campaignId) {
        return getCommonUpdateInternalCampaignRequest(campaignId, GdUpdateInternalFreeCampaign::new)
                .withName("Update free campaign " + RandomStringUtils.randomAlphanumeric(5))
                .withRestrictionValue(RandomNumberUtils.nextPositiveLong())
                .withRestrictionType(GdInternalFreeCampaignRestrictionType.SHOWS);
    }

    /**
     * Метод создает запрос на обновление кампании внутренней рекламы и заполняет общие поля из
     * GdUpdateAbstractInternalCampaign
     */
    public static <T extends GdUpdateAbstractInternalCampaign> T getCommonUpdateInternalCampaignRequest(
            Long campaignId,
            Supplier<T> model) {
        //noinspection unchecked
        return (T) model.get()
                .withId(campaignId)
                .withPageId(List.of(RandomNumberUtils.nextPositiveLong()))
                .withAttributionModel(GdCampaignAttributionModel.FIRST_CLICK)
                .withStartDate(LocalDate.now())
                .withEndDate(LocalDate.now().plusMonths(1))
                .withMetrikaCounters(emptyList())
                .withMeaningfulGoals(List.of(new GdMeaningfulGoalRequest()
                        .withConversionValue(BigDecimal.TEN)
                        .withGoalId(9L)))
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE))
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN))))
                .withTimeTarget(defaultGdTimeTarget());
    }

    public static GdAddInternalAutobudgetCampaign getGdAddInternalAutobudgetCampaignRequest() {
        return getCommonAddInternalCampaignRequest(GdAddInternalAutobudgetCampaign::new)
                .withName("Add autobudget campaign " + RandomStringUtils.randomAlphanumeric(5))
                .withBiddingStategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withSum(BigDecimal.valueOf(1000L))));
    }

    public static GdAddInternalDistribCampaign getGdAddInternalDistribCampaignRequest() {
        return getCommonAddInternalCampaignRequest(GdAddInternalDistribCampaign::new)
                .withName("Add distrib campaign " + RandomStringUtils.randomAlphanumeric(5))
                .withRotationGoalId(RandomNumberUtils.nextPositiveLong());
    }

    public static GdAddInternalFreeCampaign getGdAddInternalFreeCampaignRequest() {
        return getCommonAddInternalCampaignRequest(GdAddInternalFreeCampaign::new)
                .withName("Add free campaign " + RandomStringUtils.randomAlphanumeric(5))
                .withRestrictionValue(RandomNumberUtils.nextPositiveLong())
                .withRestrictionType(GdInternalFreeCampaignRestrictionType.CLICKS);
    }

    /**
     * Метод создает запрос на добавление кампании внутренней рекламы и заполняет общие поля из
     * GdAddAbstractInternalCampaign
     */
    public static <T extends GdAddAbstractInternalCampaign> T getCommonAddInternalCampaignRequest(Supplier<T> model) {
        //noinspection unchecked
        return (T) model.get()
                .withIsMobile(false)
                .withPlaceId((long) RandomNumberUtils.nextPositiveInteger())
                .withPageId(List.of(RandomNumberUtils.nextPositiveLong()))
                .withAttributionModel(GdCampaignAttributionModel.LAST_SIGNIFICANT_CLICK)
                .withStartDate(LocalDate.now())
                .withEndDate(LocalDate.now().plusMonths(1))
                .withMetrikaCounters(emptyList())
                .withMeaningfulGoals(List.of(new GdMeaningfulGoalRequest()
                        .withConversionValue(BigDecimal.ONE)
                        .withGoalId(11L)))
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(new GdCampaignEmailSettingsRequest()
                                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                                .withSendAccountNews(true)
                                .withStopByReachDailyBudget(true)
                                .withXlsReady(true)
                                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE))
                        .withSmsSettings(new GdCampaignSmsSettingsRequest()
                                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN))))
                .withTimeTarget(defaultGdTimeTarget());
    }
}
