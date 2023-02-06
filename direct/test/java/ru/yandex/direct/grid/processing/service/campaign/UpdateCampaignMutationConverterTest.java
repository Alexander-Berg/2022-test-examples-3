package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegmentAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.BroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithAllowedPageIds;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithContentLanguage;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDialog;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.ContentLanguage;
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppDeviceTypeTargeting;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppNetworkTargeting;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.grid.model.GdTime;
import ru.yandex.direct.grid.model.campaign.GdCampAimType;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.model.campaign.GdContentLanguage;
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignDeviceTypeTargeting;
import ru.yandex.direct.grid.model.campaign.GdMobileContentCampaignNetworkTargeting;
import ru.yandex.direct.grid.model.campaign.notification.GdCampaignSmsEvent;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdAgeType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType;
import ru.yandex.direct.grid.processing.model.bidmodifier.GdGenderType;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierABSegment;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierABSegmentAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographics;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierDemographicsAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.campaign.facelift.GdAddUpdateCampaignAdditionalData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignVcard;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdBroadMatchRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBrandSafetyRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignEmailSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignNotificationRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignSmsSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdMeaningfulGoalRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateContentPromotionCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateDynamicCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateMcBannerCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateMobileContentCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateSmartCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateTextCampaign;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.mutation.GdAddAddress;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.mutation.GdAddPhone;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.mutation.GdAddWorkTime;
import ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.time.LocalDate.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_DAY_BUDGET;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_DAY_BUDGET_SHOW_MODE;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_COMPANY_INFO;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_CPC_HOLD;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_HAS_SITE_MONITORING;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_HAS_TITLE_SUBSTITUTION;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_HAS_TURBO_SMARTS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.DEFAULT_BANNER_HREF_PARAMS;
import static ru.yandex.direct.grid.model.utils.GridTimeUtils.toGdTimeInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toBrandSafetyCategories;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toCampaignDayBudgetShowMode;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toCampaignWarnPlaceInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toCoreVcard;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toSmsFlags;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toTimeInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.UpdateCampaignMutationConverter.gdUpdateCampaignsToCoreModelChanges;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@ParametersAreNonnullByDefault
public class UpdateCampaignMutationConverterTest {

    public static final String PHONE_NUMBER = "1231231";
    public static final String CITY_CODE = "812";
    public static final String COUNTRY_CODE = "+7";
    public static final String EXTENSION = "12";
    public static final String CAMP_HREF = "https://ololoolololololo.ru";
    private static final String COMPANY_NAME = "Рога и копыта";
    private static final String BUSINESS_CATEGORY = "Ферма";

    @Test
    public void convertTextCampaign() {
        LocalDate now = now();
        long id = 1L;
        BigDecimal dayBudget = BigDecimal.valueOf(1000L);
        GdDayBudgetShowMode gdDayBudgetShowMode = GdDayBudgetShowMode.STRETCHED;
        String name = "name";
        List<String> disabledSsp = List.of("ssp1", "ssp2");
        List<String> disabledDomains = List.of("domain.com", "domain.ru");
        List<String> disabledIps = List.of("77.1.1.1", "77.1.1.3");
        List<Long> allowedPageIds = List.of(123456L, 456789L);
        boolean hasTitleSubstitute = false;
        boolean hasTurboApp = false;
        BroadMatch broadMatch = new BroadMatch()
                .withBroadMatchFlag(true)
                .withBroadMatchLimit(50)
                .withBroadMatchGoalId(123L);

        GdCampaignEmailSettingsRequest emailSettingsRequest = new GdCampaignEmailSettingsRequest()
                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                .withSendAccountNews(false)
                .withStopByReachDailyBudget(false)
                .withXlsReady(true)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE);
        GdCampaignSmsSettingsRequest smsSettingsRequest = new GdCampaignSmsSettingsRequest()
                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN));
        long goalId = RandomNumberUtils.nextPositiveLong();
        BigDecimal conversionValue = RandomNumberUtils.nextPositiveBigDecimal();
        GdMeaningfulGoalRequest gdMeaningfulGoalRequest = new GdMeaningfulGoalRequest()
                .withGoalId(goalId)
                .withConversionValue(conversionValue);
        int percent = RandomNumberUtils.nextPositiveInteger(100);
        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers().withBidModifierABSegment(
                new GdUpdateBidModifierABSegment()
                        .withType(GdBidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withAdjustments(List.of(new GdUpdateBidModifierABSegmentAdjustmentItem()
                                .withPercent(percent))));
        GdCampaignBiddingStrategy strategy = new GdCampaignBiddingStrategy()
                .withPlatform(GdCampaignPlatform.SEARCH)
                .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                .withStrategyData(new GdCampaignStrategyData()
                        .withSum(BigDecimal.valueOf(5000))
                );
        GdTimeTarget timeTarget = DefaultValuesUtils.defaultGdTimeTarget()
                .withUseWorkingWeekends(true).withEnabledHolidaysMode(false)
                .withIdTimeZone(130L);

        GdAddPhone phone = new GdAddPhone()
                .withPhoneNumber(PHONE_NUMBER)
                .withCityCode(CITY_CODE)
                .withCountryCode(COUNTRY_CODE)
                .withExtension(EXTENSION);

        GdAddWorkTime workTime = new GdAddWorkTime()
                .withDaysOfWeek(Set.of(1))
                .withStartTime(new GdTime().withHour(1).withMinute(2))
                .withEndTime(new GdTime().withHour(3).withMinute(4));

        GdAddCampaignVcard vcard = new GdAddCampaignVcard()
                .withPhone(phone)
                .withAddress(new GdAddAddress().withCountry("Russia"))
                .withWorkTimes(List.of(workTime));

        List<Long> categoryIds = List.of(1L, 2L, 3L);

        GdUpdateTextCampaign textCampaign = new GdUpdateTextCampaign()
                .withId(id)
                .withName(name)
                .withStartDate(now)
                .withEndDate(now)
                .withTimeTarget(timeTarget)
                .withDisabledPlaces(ListUtils.union(disabledDomains, disabledSsp))
                .withDisabledIps(disabledIps)
                .withBroadMatch(new GdBroadMatchRequest()
                        .withBroadMatchFlag(broadMatch.getBroadMatchFlag())
                        .withBroadMatchLimit(broadMatch.getBroadMatchLimit())
                        .withBroadMatchGoalId(broadMatch.getBroadMatchGoalId()))
                .withHasTitleSubstitute(hasTitleSubstitute)
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(emailSettingsRequest)
                        .withSmsSettings(smsSettingsRequest))
                .withContentLanguage(GdContentLanguage.KZ)
                .withHasExtendedGeoTargeting(true)
                .withClientDialogId(id)
                .withHasSiteMonitoring(true)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withAllowedPageIds(allowedPageIds)
                .withAllowedDomains(ListUtils.union(disabledDomains, disabledSsp))
                .withMeaningfulGoals(List.of(gdMeaningfulGoalRequest))
                .withBidModifiers(bidModifiers)
                .withAbSegmentRetargetingConditionId(id)
                .withAbSegmentStatisticRetargetingConditionId(id)
                .withSectionIds(List.of(id))
                .withAbSegmentGoalIds(List.of(id))
                .withDayBudget(dayBudget)
                .withDayBudgetShowMode(gdDayBudgetShowMode)
                .withVcard(vcard)
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withEnableCompanyInfo(DEFAULT_ENABLE_COMPANY_INFO)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withRequireFiltrationByDontShowDomains(DEFAULT_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS)
                .withBiddingStategy(strategy)
                .withTurboAppsEnabled(hasTurboApp)
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData()
                        .withHref(CAMP_HREF)
                        .withCompanyName(COMPANY_NAME)
                        .withBusinessCategory(BUSINESS_CATEGORY))
                .withBrandSafetyCategories(categoryIds)
                .withBrandSafety(new GdCampaignBrandSafetyRequest()
                        .withIsEnabled(true)
                        .withAdditionalCategories(Set.of(4294967303L)))
                .withIsAllowedOnAdultContent(true)
                .withIsRecommendationsManagementEnabled(true)
                .withIsPriceRecommendationsManagementEnabled(true)
                .withIsS2sTrackingEnabled(true)
                .withBannerHrefParams(DEFAULT_BANNER_HREF_PARAMS);

        GdUpdateCampaignUnion union = new GdUpdateCampaignUnion().withTextCampaign(textCampaign);

        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(Collections.singletonList(union));

        List<? extends ModelChanges<? extends BaseCampaign>> modelChanges = gdUpdateCampaignsToCoreModelChanges(input,
                new CampaignConverterContext()
                        .withShouldProcessProperties(List.of(CampaignWithContentLanguage.CONTENT_LANGUAGE,
                                CampaignWithAllowedPageIds.ALLOWED_PAGE_IDS))
                        .withSspPlatforms(disabledSsp));
        assertThat(modelChanges).hasSize(1);

        BidModifierABSegment bidModifierABSegment =
                new BidModifierABSegment().withType(BidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withAbSegmentAdjustments(List.of(new BidModifierABSegmentAdjustment().withPercent(percent)));
        DbStrategy dbStrategy = new DbStrategy();
        dbStrategy.setPlatform(CampaignsPlatform.SEARCH);
        dbStrategy.setStrategyName(StrategyName.DEFAULT_);
        dbStrategy.setStrategyData(new StrategyData().withSum(BigDecimal.valueOf(5000)));

        ModelChanges<TextCampaign> expectedCampaignModelChanges = new ModelChanges<>(id, TextCampaign.class)
                .process(name, TextCampaign.NAME)
                .process(now, TextCampaign.START_DATE)
                .process(now, TextCampaign.END_DATE)
                .process(CampaignDataConverter.toTimeTarget(timeTarget), TextCampaign.TIME_TARGET)
                .process(timeTarget.getIdTimeZone(), TextCampaign.TIME_ZONE_ID)
                .process(null, TextCampaign.METRIKA_COUNTERS)
                .process(CampaignType.TEXT, TextCampaign.TYPE)
                .process(disabledDomains, TextCampaign.DISABLED_DOMAINS)
                .process(disabledSsp, TextCampaign.DISABLED_SSP)
                .process(disabledIps, TextCampaign.DISABLED_IPS)
                .process(emptySet(), TextCampaign.PLACEMENT_TYPES)
                .process(broadMatch, TextCampaign.BROAD_MATCH)
                .process(null, TextCampaign.MINUS_KEYWORDS)
                .process(hasTitleSubstitute, TextCampaign.HAS_TITLE_SUBSTITUTION)
                .process(hasTurboApp, TextCampaign.HAS_TURBO_APP)
                .process(toSmsFlags(smsSettingsRequest.getEnableEvents()), TextCampaign.SMS_FLAGS)
                .process(toTimeInterval(smsSettingsRequest.getSmsTime()), TextCampaign.SMS_TIME)
                .process(emailSettingsRequest.getEmail(), TextCampaign.EMAIL)
                .process(emailSettingsRequest.getCheckPositionInterval() != null,
                        TextCampaign.ENABLE_CHECK_POSITION_EVENT)
                .process(toCampaignWarnPlaceInterval(emailSettingsRequest.getCheckPositionInterval()),
                        TextCampaign.CHECK_POSITION_INTERVAL_EVENT)
                .process(emailSettingsRequest.getWarningBalance(), TextCampaign.WARNING_BALANCE)
                .process(emailSettingsRequest.getXlsReady(), TextCampaign.ENABLE_OFFLINE_STAT_NOTICE)
                .process(emailSettingsRequest.getStopByReachDailyBudget(),
                        TextCampaign.ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .process(emailSettingsRequest.getSendAccountNews(), TextCampaign.ENABLE_SEND_ACCOUNT_NEWS)
                .process(ContentLanguage.KZ, TextCampaign.CONTENT_LANGUAGE)
                .process(CampaignAttributionModel.FIRST_CLICK, TextCampaign.ATTRIBUTION_MODEL)
                .process(textCampaign.getHasExtendedGeoTargeting(), TextCampaign.HAS_EXTENDED_GEO_TARGETING)
                .processNotNull(textCampaign.getUseCurrentRegion(), TextCampaign.USE_CURRENT_REGION)
                .processNotNull(textCampaign.getUseRegularRegion(), TextCampaign.USE_REGULAR_REGION)
                .process(textCampaign.getHasAddOpenstatTagToUrl(), TextCampaign.HAS_ADD_OPENSTAT_TAG_TO_URL)
                .process(List.of(bidModifierABSegment), TextCampaign.BID_MODIFIERS)
                .process(List.of(new MeaningfulGoal()
                        .withGoalId(goalId)
                        .withConversionValue(conversionValue)), TextCampaign.MEANINGFUL_GOALS)
                .process(id, TextCampaign.AB_SEGMENT_RETARGETING_CONDITION_ID)
                .process(id, TextCampaign.AB_SEGMENT_STATISTIC_RETARGETING_CONDITION_ID)
                .process(List.of(id), TextCampaign.SECTION_IDS)
                .process(List.of(id), TextCampaign.AB_SEGMENT_GOAL_IDS)
                .process(id, CampaignWithDialog.CLIENT_DIALOG_ID)
                .process(textCampaign.getDayBudget(), TextCampaign.DAY_BUDGET)
                .process(toCampaignDayBudgetShowMode(textCampaign.getDayBudgetShowMode()),
                        TextCampaign.DAY_BUDGET_SHOW_MODE)
                .process(allowedPageIds, TextCampaign.ALLOWED_PAGE_IDS)
                .process(disabledDomains, TextCampaign.ALLOWED_DOMAINS)
                .process(disabledSsp, TextCampaign.ALLOWED_SSP)
                .process(textCampaign.getHasAddMetrikaTagToUrl(), TextCampaign.HAS_ADD_METRIKA_TAG_TO_URL)
                .process(textCampaign.getHasSiteMonitoring(), TextCampaign.HAS_SITE_MONITORING)
                .process(textCampaign.getPromoExtensionId(), TextCampaign.PROMO_EXTENSION_ID)
                .process(textCampaign.getDefaultPermalinkId(), TextCampaign.DEFAULT_PERMALINK_ID)
                .process(textCampaign.getDefaultChainId(), TextCampaign.DEFAULT_CHAIN_ID)
                .process(textCampaign.getDefaultTrackingPhoneId(), TextCampaign.DEFAULT_TRACKING_PHONE_ID)
                .process(textCampaign.getContextLimit(), TextCampaign.CONTEXT_LIMIT)
                .process(toCoreVcard(textCampaign.getVcard()), TextCampaign.CONTACT_INFO)
                .process(textCampaign.getEnableCpcHold(), TextCampaign.ENABLE_CPC_HOLD)
                .process(dbStrategy, TextCampaign.STRATEGY)
                .process(textCampaign.getEnableCompanyInfo(), TextCampaign.ENABLE_COMPANY_INFO)
                .process(textCampaign.getIsAloneTrafaretAllowed(), TextCampaign.IS_ALONE_TRAFARET_ALLOWED)
                .process(textCampaign.getExcludePausedCompetingAds(), TextCampaign.EXCLUDE_PAUSED_COMPETING_ADS)
                .process(toBrandSafetyCategories(textCampaign.getBrandSafetyCategories(),
                        textCampaign.getBrandSafety()),
                        TextCampaign.BRAND_SAFETY_CATEGORIES)
                .process(CAMP_HREF, TextCampaign.HREF)
                .process(ifNotNull(textCampaign.getCampAimType(), GdCampAimType::getTypedValue),
                        TextCampaign.CAMP_AIM_TYPE)
                .process(textCampaign.getCalltrackingSettingsId(), TextCampaign.CALLTRACKING_SETTINGS_ID)
                //.process(textCampaign.getIsUniversalCamp(), TextCampaign.IS_UNIVERSAL)  //DIRECT-130150
                .process(textCampaign.getRequireFiltrationByDontShowDomains(),
                        TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS)
                .process(COMPANY_NAME, TextCampaign.COMPANY_NAME)
                .process(BUSINESS_CATEGORY, TextCampaign.BUSINESS_CATEGORY)
                .process(textCampaign.getIsAllowedOnAdultContent(), TextCampaign.IS_ALLOWED_ON_ADULT_CONTENT)
                .process(textCampaign.getIsRecommendationsManagementEnabled(),
                        TextCampaign.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                .process(textCampaign.getIsPriceRecommendationsManagementEnabled(),
                        TextCampaign.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                .process(textCampaign.getIsS2sTrackingEnabled(), TextCampaign.IS_S2S_TRACKING_ENABLED)
                .process(textCampaign.getBannerHrefParams(), TextCampaign.BANNER_HREF_PARAMS);

        assertThat(modelChanges.get(0)).is(matchedBy(beanDiffer(expectedCampaignModelChanges)));
    }

    @Test
    public void convertMcBannerCampaign() {
        LocalDate now = now();
        long id = 1L;
        BigDecimal dayBudget = BigDecimal.valueOf(1000L);
        GdDayBudgetShowMode gdDayBudgetShowMode = GdDayBudgetShowMode.STRETCHED;
        String name = "mcbanner campaign";
        List<String> disabledSsp = List.of("ssp1", "ssp2");
        List<String> disabledDomains = List.of("domain.com", "domain.ru");
        List<String> disabledIps = List.of("77.1.1.1", "77.1.1.3");

        GdCampaignEmailSettingsRequest emailSettingsRequest = new GdCampaignEmailSettingsRequest()
                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                .withStopByReachDailyBudget(false)
                .withXlsReady(true)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE);
        GdCampaignSmsSettingsRequest smsSettingsRequest = new GdCampaignSmsSettingsRequest()
                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN));
        long goalId = RandomNumberUtils.nextPositiveLong();
        BigDecimal conversionValue = RandomNumberUtils.nextPositiveBigDecimal();
        GdMeaningfulGoalRequest gdMeaningfulGoalRequest = new GdMeaningfulGoalRequest()
                .withGoalId(goalId)
                .withConversionValue(conversionValue);
        int percent = RandomNumberUtils.nextPositiveInteger(100);
        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers().withBidModifierABSegment(
                new GdUpdateBidModifierABSegment()
                        .withType(GdBidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withAdjustments(List.of(new GdUpdateBidModifierABSegmentAdjustmentItem()
                                .withPercent(percent))));
        GdCampaignBiddingStrategy strategy = new GdCampaignBiddingStrategy()
                .withPlatform(GdCampaignPlatform.SEARCH)
                .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                .withStrategyData(new GdCampaignStrategyData()
                        .withSum(BigDecimal.valueOf(5000)));
        GdTimeTarget timeTarget = DefaultValuesUtils.defaultGdTimeTarget()
                .withUseWorkingWeekends(true).withEnabledHolidaysMode(false)
                .withIdTimeZone(130L);

        List<Long> categoryIds = List.of(1L, 2L, 3L);

        GdUpdateMcBannerCampaign updateCampaign = new GdUpdateMcBannerCampaign()
                .withId(id)
                .withName(name)
                .withStartDate(now)
                .withEndDate(now)
                .withTimeTarget(timeTarget)
                .withDisabledPlaces(ListUtils.union(disabledDomains, disabledSsp))
                .withDisabledIps(disabledIps)
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(emailSettingsRequest)
                        .withSmsSettings(smsSettingsRequest))
                .withContentLanguage(GdContentLanguage.KZ)
                .withHasExtendedGeoTargeting(true)
                .withHasSiteMonitoring(true)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withMeaningfulGoals(List.of(gdMeaningfulGoalRequest))
                .withBidModifiers(bidModifiers)
                .withAbSegmentRetargetingConditionId(id)
                .withAbSegmentStatisticRetargetingConditionId(id)
                .withSectionIds(List.of(id))
                .withAbSegmentGoalIds(List.of(id))
                .withDayBudget(dayBudget)
                .withDayBudgetShowMode(gdDayBudgetShowMode)
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withBiddingStrategy(strategy)
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(CAMP_HREF))
                .withIsRecommendationsManagementEnabled(true)
                .withIsPriceRecommendationsManagementEnabled(true)
                .withBrandSafetyCategories(categoryIds);

        GdUpdateCampaignUnion union = new GdUpdateCampaignUnion().withMcBannerCampaign(updateCampaign);

        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(Collections.singletonList(union));

        List<? extends ModelChanges<? extends BaseCampaign>> modelChanges = gdUpdateCampaignsToCoreModelChanges(input,
                new CampaignConverterContext()
                        .withShouldProcessProperties(List.of(CampaignWithContentLanguage.CONTENT_LANGUAGE))
                        .withSspPlatforms(disabledSsp));
        assertThat(modelChanges).hasSize(1);

        BidModifierABSegment bidModifierABSegment =
                new BidModifierABSegment().withType(BidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withAbSegmentAdjustments(List.of(new BidModifierABSegmentAdjustment().withPercent(percent)));
        DbStrategy dbStrategy = new DbStrategy();
        dbStrategy.setPlatform(CampaignsPlatform.SEARCH);
        dbStrategy.setStrategyName(StrategyName.DEFAULT_);
        dbStrategy.setStrategyData(new StrategyData().withSum(BigDecimal.valueOf(5000)));

        ModelChanges<McBannerCampaign> expectedCampaignModelChanges = new ModelChanges<>(id, McBannerCampaign.class)
                .process(name, McBannerCampaign.NAME)
                .process(now, McBannerCampaign.START_DATE)
                .process(now, McBannerCampaign.END_DATE)
                .process(CampaignDataConverter.toTimeTarget(timeTarget), McBannerCampaign.TIME_TARGET)
                .process(timeTarget.getIdTimeZone(), McBannerCampaign.TIME_ZONE_ID)
                .process(null, McBannerCampaign.METRIKA_COUNTERS)
                .process(CampaignType.MCBANNER, McBannerCampaign.TYPE)
                .process(disabledDomains, McBannerCampaign.DISABLED_DOMAINS)
                .process(disabledSsp, McBannerCampaign.DISABLED_SSP)
                .process(disabledIps, McBannerCampaign.DISABLED_IPS)
                .process(null, McBannerCampaign.MINUS_KEYWORDS)
                .process(toSmsFlags(smsSettingsRequest.getEnableEvents()), McBannerCampaign.SMS_FLAGS)
                .process(toTimeInterval(smsSettingsRequest.getSmsTime()), McBannerCampaign.SMS_TIME)
                .process(emailSettingsRequest.getEmail(), McBannerCampaign.EMAIL)
                .process(emailSettingsRequest.getWarningBalance(), McBannerCampaign.WARNING_BALANCE)
                .process(emailSettingsRequest.getXlsReady(), McBannerCampaign.ENABLE_OFFLINE_STAT_NOTICE)
                .process(emailSettingsRequest.getStopByReachDailyBudget(),
                        McBannerCampaign.ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .process(emailSettingsRequest.getSendAccountNews(), McBannerCampaign.ENABLE_SEND_ACCOUNT_NEWS)
                .process(ContentLanguage.KZ, McBannerCampaign.CONTENT_LANGUAGE)
                .process(updateCampaign.getHasExtendedGeoTargeting(), McBannerCampaign.HAS_EXTENDED_GEO_TARGETING)
                .processNotNull(updateCampaign.getUseCurrentRegion(), McBannerCampaign.USE_CURRENT_REGION)
                .processNotNull(updateCampaign.getUseRegularRegion(), McBannerCampaign.USE_REGULAR_REGION)
                .process(updateCampaign.getHasAddOpenstatTagToUrl(), McBannerCampaign.HAS_ADD_OPENSTAT_TAG_TO_URL)
                .process(List.of(bidModifierABSegment), McBannerCampaign.BID_MODIFIERS)
                .process(List.of(new MeaningfulGoal()
                        .withGoalId(goalId)
                        .withConversionValue(conversionValue)), McBannerCampaign.MEANINGFUL_GOALS)
                .process(id, McBannerCampaign.AB_SEGMENT_RETARGETING_CONDITION_ID)
                .process(id, McBannerCampaign.AB_SEGMENT_STATISTIC_RETARGETING_CONDITION_ID)
                .process(List.of(id), McBannerCampaign.SECTION_IDS)
                .process(List.of(id), McBannerCampaign.AB_SEGMENT_GOAL_IDS)
                .process(updateCampaign.getDayBudget(), McBannerCampaign.DAY_BUDGET)
                .process(toCampaignDayBudgetShowMode(updateCampaign.getDayBudgetShowMode()),
                        McBannerCampaign.DAY_BUDGET_SHOW_MODE)
                .process(updateCampaign.getHasAddMetrikaTagToUrl(), McBannerCampaign.HAS_ADD_METRIKA_TAG_TO_URL)
                .process(updateCampaign.getHasSiteMonitoring(), McBannerCampaign.HAS_SITE_MONITORING)
                .process(updateCampaign.getContextLimit(), McBannerCampaign.CONTEXT_LIMIT)
                .process(updateCampaign.getEnableCpcHold(), McBannerCampaign.ENABLE_CPC_HOLD)
                .process(dbStrategy, McBannerCampaign.STRATEGY)
                .process(CAMP_HREF, McBannerCampaign.HREF)
                .process(updateCampaign.getIsRecommendationsManagementEnabled(),
                        TextCampaign.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                .process(updateCampaign.getIsPriceRecommendationsManagementEnabled(),
                        TextCampaign.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                .process(updateCampaign.getBrandSafetyCategories(), McBannerCampaign.BRAND_SAFETY_CATEGORIES);

        assertThat(modelChanges.get(0)).is(matchedBy(beanDiffer(expectedCampaignModelChanges)));
    }

    @Test
    public void convertDynamicCampaign() {
        LocalDate now = now();
        long id = 1L;
        BigDecimal dayBudget = BigDecimal.valueOf(1000L);
        GdDayBudgetShowMode gdDayBudgetShowMode = GdDayBudgetShowMode.STRETCHED;
        String name = "name";
        List<String> disabledSsp = List.of("ssp1", "ssp2");
        List<String> disabledDomains = List.of("domain.com", "domain.ru");
        List<String> disabledIps = List.of("77.1.1.1", "77.1.1.3");
        List<Long> allowedPageIds = List.of(123456L, 456789L);
        boolean hasTitleSubstitute = false;
        boolean hasTurboApp = false;

        GdCampaignEmailSettingsRequest emailSettingsRequest = new GdCampaignEmailSettingsRequest()
                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                .withSendAccountNews(false)
                .withStopByReachDailyBudget(false)
                .withXlsReady(true)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE);
        GdCampaignSmsSettingsRequest smsSettingsRequest = new GdCampaignSmsSettingsRequest()
                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN));
        long goalId = RandomNumberUtils.nextPositiveLong();
        BigDecimal conversionValue = RandomNumberUtils.nextPositiveBigDecimal();
        GdMeaningfulGoalRequest gdMeaningfulGoalRequest = new GdMeaningfulGoalRequest()
                .withGoalId(goalId)
                .withConversionValue(conversionValue);
        int percent = RandomNumberUtils.nextPositiveInteger(100);
        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers().withBidModifierABSegment(
                new GdUpdateBidModifierABSegment()
                        .withType(GdBidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withAdjustments(List.of(new GdUpdateBidModifierABSegmentAdjustmentItem()
                                .withPercent(percent))));
        GdCampaignBiddingStrategy strategy = new GdCampaignBiddingStrategy()
                .withPlatform(GdCampaignPlatform.SEARCH)
                .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                .withStrategyData(new GdCampaignStrategyData()
                        .withSum(BigDecimal.valueOf(5000))
                );
        GdTimeTarget timeTarget = DefaultValuesUtils.defaultGdTimeTarget()
                .withUseWorkingWeekends(true).withEnabledHolidaysMode(false)
                .withIdTimeZone(130L);

        GdAddPhone phone = new GdAddPhone()
                .withPhoneNumber(PHONE_NUMBER)
                .withCityCode(CITY_CODE)
                .withCountryCode(COUNTRY_CODE)
                .withExtension(EXTENSION);

        GdAddWorkTime workTime = new GdAddWorkTime()
                .withDaysOfWeek(Set.of(1))
                .withStartTime(new GdTime().withHour(1).withMinute(2))
                .withEndTime(new GdTime().withHour(3).withMinute(4));

        GdAddCampaignVcard vcard = new GdAddCampaignVcard()
                .withPhone(phone)
                .withAddress(new GdAddAddress().withCountry("Russia"))
                .withWorkTimes(List.of(workTime));

        List<Long> categoryIds = List.of(1L, 2L, 3L);

        GdUpdateDynamicCampaign dynamicCampaign = new GdUpdateDynamicCampaign()
                .withId(id)
                .withName(name)
                .withStartDate(now)
                .withEndDate(now)
                .withTimeTarget(timeTarget)
                .withDisabledPlaces(ListUtils.union(disabledDomains, disabledSsp))
                .withDisabledIps(disabledIps)
                .withHasTitleSubstitute(hasTitleSubstitute)
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(emailSettingsRequest)
                        .withSmsSettings(smsSettingsRequest))
                .withContentLanguage(GdContentLanguage.KZ)
                .withHasExtendedGeoTargeting(true)
                .withHasSiteMonitoring(true)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withAllowedPageIds(allowedPageIds)
                .withAllowedDomains(ListUtils.union(disabledDomains, disabledSsp))
                .withMeaningfulGoals(List.of(gdMeaningfulGoalRequest))
                .withBidModifiers(bidModifiers)
                .withAbSegmentRetargetingConditionId(id)
                .withAbSegmentStatisticRetargetingConditionId(id)
                .withSectionIds(List.of(id))
                .withAbSegmentGoalIds(List.of(id))
                .withDayBudget(dayBudget)
                .withDayBudgetShowMode(gdDayBudgetShowMode)
                .withVcard(vcard)
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withEnableCompanyInfo(DEFAULT_ENABLE_COMPANY_INFO)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withRequireFiltrationByDontShowDomains(DEFAULT_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS)
                .withBiddingStategy(strategy)
                .withTurboAppsEnabled(hasTurboApp)
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(CAMP_HREF))
                .withBrandSafetyCategories(categoryIds)
                .withIsRecommendationsManagementEnabled(true)
                .withIsPriceRecommendationsManagementEnabled(true)
                .withIsS2sTrackingEnabled(true)
                .withBannerHrefParams(DEFAULT_BANNER_HREF_PARAMS);

        GdUpdateCampaignUnion union = new GdUpdateCampaignUnion().withDynamicCampaign(dynamicCampaign);

        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(Collections.singletonList(union));

        List<? extends ModelChanges<? extends BaseCampaign>> modelChanges = gdUpdateCampaignsToCoreModelChanges(input,
                new CampaignConverterContext()
                        .withShouldProcessProperties(List.of(CampaignWithContentLanguage.CONTENT_LANGUAGE,
                                CampaignWithAllowedPageIds.ALLOWED_PAGE_IDS))
                        .withSspPlatforms(disabledSsp));
        assertThat(modelChanges).hasSize(1);

        BidModifierABSegment bidModifierABSegment =
                new BidModifierABSegment().withType(BidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withAbSegmentAdjustments(List.of(new BidModifierABSegmentAdjustment().withPercent(percent)));
        DbStrategy dbStrategy = new DbStrategy();
        dbStrategy.setPlatform(CampaignsPlatform.SEARCH);
        dbStrategy.setStrategyName(StrategyName.DEFAULT_);
        dbStrategy.setStrategyData(new StrategyData().withSum(BigDecimal.valueOf(5000)));

        ModelChanges<DynamicCampaign> expectedCampaignModelChanges = new ModelChanges<>(id, DynamicCampaign.class)
                .process(name, DynamicCampaign.NAME)
                .process(now, DynamicCampaign.START_DATE)
                .process(now, DynamicCampaign.END_DATE)
                .process(CampaignDataConverter.toTimeTarget(timeTarget), DynamicCampaign.TIME_TARGET)
                .process(timeTarget.getIdTimeZone(), DynamicCampaign.TIME_ZONE_ID)
                .process(null, DynamicCampaign.METRIKA_COUNTERS)
                .process(CampaignType.DYNAMIC, DynamicCampaign.TYPE)
                .process(disabledDomains, DynamicCampaign.DISABLED_DOMAINS)
                .process(disabledSsp, DynamicCampaign.DISABLED_SSP)
                .process(disabledIps, DynamicCampaign.DISABLED_IPS)
                .process(emptySet(), DynamicCampaign.PLACEMENT_TYPES)
                .process(null, DynamicCampaign.MINUS_KEYWORDS)
                .process(hasTitleSubstitute, DynamicCampaign.HAS_TITLE_SUBSTITUTION)
                .process(hasTurboApp, DynamicCampaign.HAS_TURBO_APP)
                .process(toSmsFlags(smsSettingsRequest.getEnableEvents()), DynamicCampaign.SMS_FLAGS)
                .process(toTimeInterval(smsSettingsRequest.getSmsTime()), DynamicCampaign.SMS_TIME)
                .process(emailSettingsRequest.getEmail(), DynamicCampaign.EMAIL)
                .process(emailSettingsRequest.getCheckPositionInterval() != null,
                        DynamicCampaign.ENABLE_CHECK_POSITION_EVENT)
                .process(toCampaignWarnPlaceInterval(emailSettingsRequest.getCheckPositionInterval()),
                        DynamicCampaign.CHECK_POSITION_INTERVAL_EVENT)
                .process(emailSettingsRequest.getWarningBalance(), DynamicCampaign.WARNING_BALANCE)
                .process(emailSettingsRequest.getXlsReady(), DynamicCampaign.ENABLE_OFFLINE_STAT_NOTICE)
                .process(emailSettingsRequest.getStopByReachDailyBudget(),
                        DynamicCampaign.ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .process(emailSettingsRequest.getSendAccountNews(), DynamicCampaign.ENABLE_SEND_ACCOUNT_NEWS)
                .process(ContentLanguage.KZ, DynamicCampaign.CONTENT_LANGUAGE)
                .process(CampaignAttributionModel.FIRST_CLICK, DynamicCampaign.ATTRIBUTION_MODEL)
                .process(dynamicCampaign.getHasExtendedGeoTargeting(), DynamicCampaign.HAS_EXTENDED_GEO_TARGETING)
                .processNotNull(dynamicCampaign.getUseCurrentRegion(), DynamicCampaign.USE_CURRENT_REGION)
                .processNotNull(dynamicCampaign.getUseRegularRegion(), DynamicCampaign.USE_REGULAR_REGION)
                .process(dynamicCampaign.getHasAddOpenstatTagToUrl(), DynamicCampaign.HAS_ADD_OPENSTAT_TAG_TO_URL)
                .process(List.of(bidModifierABSegment), DynamicCampaign.BID_MODIFIERS)
                .process(List.of(new MeaningfulGoal()
                        .withGoalId(goalId)
                        .withConversionValue(conversionValue)), DynamicCampaign.MEANINGFUL_GOALS)
                .process(id, DynamicCampaign.AB_SEGMENT_RETARGETING_CONDITION_ID)
                .process(id, DynamicCampaign.AB_SEGMENT_STATISTIC_RETARGETING_CONDITION_ID)
                .process(List.of(id), DynamicCampaign.SECTION_IDS)
                .process(List.of(id), DynamicCampaign.AB_SEGMENT_GOAL_IDS)
                .process(dynamicCampaign.getDayBudget(), DynamicCampaign.DAY_BUDGET)
                .process(toCampaignDayBudgetShowMode(dynamicCampaign.getDayBudgetShowMode()),
                        DynamicCampaign.DAY_BUDGET_SHOW_MODE)
                .process(allowedPageIds, DynamicCampaign.ALLOWED_PAGE_IDS)
                .process(disabledDomains, DynamicCampaign.ALLOWED_DOMAINS)
                .process(disabledSsp, DynamicCampaign.ALLOWED_SSP)
                .process(dynamicCampaign.getHasAddMetrikaTagToUrl(), DynamicCampaign.HAS_ADD_METRIKA_TAG_TO_URL)
                .process(dynamicCampaign.getHasSiteMonitoring(), DynamicCampaign.HAS_SITE_MONITORING)
                .process(dynamicCampaign.getDefaultPermalinkId(), DynamicCampaign.DEFAULT_PERMALINK_ID)
                .process(toCoreVcard(dynamicCampaign.getVcard()), DynamicCampaign.CONTACT_INFO)
                .process(dynamicCampaign.getEnableCpcHold(), DynamicCampaign.ENABLE_CPC_HOLD)
                .process(dbStrategy, DynamicCampaign.STRATEGY)
                .process(dynamicCampaign.getEnableCompanyInfo(), DynamicCampaign.ENABLE_COMPANY_INFO)
                .process(dynamicCampaign.getIsAloneTrafaretAllowed(), DynamicCampaign.IS_ALONE_TRAFARET_ALLOWED)
                .process(dynamicCampaign.getBrandSafetyCategories(), DynamicCampaign.BRAND_SAFETY_CATEGORIES)
                .process(dynamicCampaign.getPromoExtensionId(), DynamicCampaign.PROMO_EXTENSION_ID)
                .process(CAMP_HREF, DynamicCampaign.HREF)
                .process(dynamicCampaign.getRequireFiltrationByDontShowDomains(),
                        DynamicCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS)
                .process(dynamicCampaign.getIsS2sTrackingEnabled(), DynamicCampaign.IS_S2S_TRACKING_ENABLED)
                .process(dynamicCampaign.getIsRecommendationsManagementEnabled(),
                        DynamicCampaign.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                .process(dynamicCampaign.getIsPriceRecommendationsManagementEnabled(),
                        DynamicCampaign.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                .process(DEFAULT_HAS_TURBO_SMARTS, DynamicCampaign.HAS_TURBO_SMARTS)
                .process(dynamicCampaign.getBannerHrefParams(), DynamicCampaign.BANNER_HREF_PARAMS);

        assertThat(modelChanges.get(0)).is(matchedBy(beanDiffer(expectedCampaignModelChanges)));
    }

    @Test
    public void convertSmartCampaign() {
        LocalDate now = now();
        long id = 1L;
        String name = "name";
        List<String> disabledSsp = List.of("ssp1", "ssp2");
        List<String> disabledDomains = List.of("domain.com", "domain.ru");
        List<String> disabledIps = List.of("77.1.1.1", "77.1.1.3");
        boolean hasTurboApp = false;

        GdCampaignEmailSettingsRequest emailSettingsRequest = new GdCampaignEmailSettingsRequest()
                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                .withSendAccountNews(false)
                .withStopByReachDailyBudget(false)
                .withXlsReady(true)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE);
        GdCampaignSmsSettingsRequest smsSettingsRequest = new GdCampaignSmsSettingsRequest()
                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN));
        long goalId = RandomNumberUtils.nextPositiveLong();
        BigDecimal conversionValue = RandomNumberUtils.nextPositiveBigDecimal();
        GdMeaningfulGoalRequest gdMeaningfulGoalRequest = new GdMeaningfulGoalRequest()
                .withGoalId(goalId)
                .withConversionValue(conversionValue);
        int percent = RandomNumberUtils.nextPositiveInteger(100);
        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers().withBidModifierABSegment(
                new GdUpdateBidModifierABSegment()
                        .withType(GdBidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withAdjustments(List.of(new GdUpdateBidModifierABSegmentAdjustmentItem()
                                .withPercent(percent))));
        GdCampaignBiddingStrategy strategy = new GdCampaignBiddingStrategy()
                .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_ROI)
                .withPlatform(GdCampaignPlatform.BOTH)
                .withStrategy(GdCampaignStrategy.DIFFERENT_PLACES)
                .withStrategyData(new GdCampaignStrategyData()
                        .withRoiCoef(new BigDecimal("1"))
                        .withReserveReturn(20L)
                        .withProfitability(new BigDecimal("20"))
                        .withGoalId(0L));
        GdTimeTarget timeTarget = DefaultValuesUtils.defaultGdTimeTarget()
                .withUseWorkingWeekends(true).withEnabledHolidaysMode(false)
                .withIdTimeZone(130L);
        List<Long> categoryIds = List.of(1L, 2L, 3L);
        GdUpdateSmartCampaign smartCampaign = new GdUpdateSmartCampaign()
                .withId(id)
                .withName(name)
                .withStartDate(now)
                .withEndDate(now)
                .withTimeTarget(timeTarget)
                .withDisabledPlaces(ListUtils.union(disabledDomains, disabledSsp))
                .withDisabledIps(disabledIps)
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(emailSettingsRequest)
                        .withSmsSettings(smsSettingsRequest))
                .withContentLanguage(GdContentLanguage.KZ)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withMeaningfulGoals(List.of(gdMeaningfulGoalRequest))
                .withBidModifiers(bidModifiers)
                .withAbSegmentRetargetingConditionId(id)
                .withAbSegmentStatisticRetargetingConditionId(id)
                .withSectionIds(List.of(id))
                .withAbSegmentGoalIds(List.of(id))
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withHasTurboSmarts(DEFAULT_HAS_TURBO_SMARTS)
                .withBiddingStrategy(strategy)
                .withTurboAppsEnabled(hasTurboApp)
                .withRequireFiltrationByDontShowDomains(DEFAULT_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS)
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(CAMP_HREF))
                .withBrandSafetyCategories(categoryIds)
                .withIsRecommendationsManagementEnabled(true)
                .withIsPriceRecommendationsManagementEnabled(true)
                .withIsS2sTrackingEnabled(true)
                .withBannerHrefParams(DEFAULT_BANNER_HREF_PARAMS);

        GdUpdateCampaignUnion union = new GdUpdateCampaignUnion().withSmartCampaign(smartCampaign);
        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(Collections.singletonList(union));
        List<? extends ModelChanges<? extends BaseCampaign>> modelChanges = gdUpdateCampaignsToCoreModelChanges(input,
                new CampaignConverterContext()
                        .withShouldProcessProperties(List.of(CampaignWithContentLanguage.CONTENT_LANGUAGE))
                        .withSspPlatforms(disabledSsp));
        assertThat(modelChanges).hasSize(1);
        BidModifierABSegment bidModifierABSegment =
                new BidModifierABSegment().withType(BidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withAbSegmentAdjustments(List.of(new BidModifierABSegmentAdjustment()
                                .withPercent(percent)));
        DbStrategy dbStrategy = TestCampaigns.defaultAutobudgetRoiStrategy(0L);
        ModelChanges<SmartCampaign> expectedCampaignModelChanges = new ModelChanges<>(id, SmartCampaign.class)
                .process(name, SmartCampaign.NAME)
                .process(now, SmartCampaign.START_DATE)
                .process(now, SmartCampaign.END_DATE)
                .process(CampaignDataConverter.toTimeTarget(timeTarget), SmartCampaign.TIME_TARGET)
                .process(timeTarget.getIdTimeZone(), SmartCampaign.TIME_ZONE_ID)
                .process(null, SmartCampaign.METRIKA_COUNTERS)
                .process(CampaignType.PERFORMANCE, SmartCampaign.TYPE)
                .process(disabledDomains, SmartCampaign.DISABLED_DOMAINS)
                .process(disabledSsp, SmartCampaign.DISABLED_SSP)
                .process(disabledIps, SmartCampaign.DISABLED_IPS)
                .process(null, SmartCampaign.MINUS_KEYWORDS)
                .process(hasTurboApp, SmartCampaign.HAS_TURBO_APP)
                .process(toSmsFlags(smsSettingsRequest.getEnableEvents()), SmartCampaign.SMS_FLAGS)
                .process(toTimeInterval(smsSettingsRequest.getSmsTime()), SmartCampaign.SMS_TIME)
                .process(emailSettingsRequest.getEmail(), SmartCampaign.EMAIL)
                .process(emailSettingsRequest.getWarningBalance(), SmartCampaign.WARNING_BALANCE)
                .process(emailSettingsRequest.getXlsReady(), SmartCampaign.ENABLE_OFFLINE_STAT_NOTICE)
                .process(emailSettingsRequest.getCheckPositionInterval() != null,
                        TextCampaign.ENABLE_CHECK_POSITION_EVENT)
                .process(toCampaignWarnPlaceInterval(emailSettingsRequest.getCheckPositionInterval()),
                        TextCampaign.CHECK_POSITION_INTERVAL_EVENT)
                .process(emailSettingsRequest.getStopByReachDailyBudget(),
                        SmartCampaign.ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .process(emailSettingsRequest.getSendAccountNews(), SmartCampaign.ENABLE_SEND_ACCOUNT_NEWS)
                .process(ContentLanguage.KZ, SmartCampaign.CONTENT_LANGUAGE)
                .process(CampaignAttributionModel.FIRST_CLICK, SmartCampaign.ATTRIBUTION_MODEL)
                .process(List.of(bidModifierABSegment), SmartCampaign.BID_MODIFIERS)
                .process(List.of(new MeaningfulGoal()
                        .withGoalId(goalId)
                        .withConversionValue(conversionValue)), SmartCampaign.MEANINGFUL_GOALS)
                .process(id, SmartCampaign.AB_SEGMENT_RETARGETING_CONDITION_ID)
                .process(id, SmartCampaign.AB_SEGMENT_STATISTIC_RETARGETING_CONDITION_ID)
                .process(List.of(id), SmartCampaign.SECTION_IDS)
                .process(List.of(id), SmartCampaign.AB_SEGMENT_GOAL_IDS)
                .process(smartCampaign.getHasAddMetrikaTagToUrl(), SmartCampaign.HAS_ADD_METRIKA_TAG_TO_URL)
                .process(smartCampaign.getContextLimit(), SmartCampaign.CONTEXT_LIMIT)
                .process(dbStrategy, SmartCampaign.STRATEGY)
                .process(smartCampaign.getIsAloneTrafaretAllowed(), SmartCampaign.IS_ALONE_TRAFARET_ALLOWED)
                .process(smartCampaign.getHasTurboSmarts(), SmartCampaign.HAS_TURBO_SMARTS)
                .process(smartCampaign.getRequireFiltrationByDontShowDomains(),
                        SmartCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS)
                .process(CAMP_HREF, SmartCampaign.HREF)
                .process(smartCampaign.getBrandSafetyCategories(), SmartCampaign.BRAND_SAFETY_CATEGORIES)
                .process(true, SmartCampaign.HAS_EXTENDED_GEO_TARGETING)
                .processNotNull(smartCampaign.getUseCurrentRegion(), SmartCampaign.USE_CURRENT_REGION)
                .processNotNull(smartCampaign.getUseRegularRegion(), SmartCampaign.USE_REGULAR_REGION)
                .process(smartCampaign.getIsRecommendationsManagementEnabled(),
                        TextCampaign.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                .process(smartCampaign.getIsPriceRecommendationsManagementEnabled(),
                        TextCampaign.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                .process(smartCampaign.getIsS2sTrackingEnabled(), SmartCampaign.IS_S2S_TRACKING_ENABLED)
                .process(smartCampaign.getBannerHrefParams(), SmartCampaign.BANNER_HREF_PARAMS)

                // Грид не поддерживает эти проперти, но при конвертации в core они появляются в обновлениях.
                // При добавлении поддержки в грид тест нужно обновить
                .process(DEFAULT_ENABLE_COMPANY_INFO, SmartCampaign.ENABLE_COMPANY_INFO)
                .process(false, SmartCampaign.ENABLE_CPC_HOLD)
                .process(DEFAULT_HAS_SITE_MONITORING, SmartCampaign.HAS_SITE_MONITORING)
                .process(DEFAULT_HAS_TITLE_SUBSTITUTION, SmartCampaign.HAS_TITLE_SUBSTITUTION)
                .process(DEFAULT_DAY_BUDGET, SmartCampaign.DAY_BUDGET)
                .process(DEFAULT_DAY_BUDGET_SHOW_MODE, SmartCampaign.DAY_BUDGET_SHOW_MODE)
                .process(DEFAULT_ADD_OPENSTAT_TAG_TO_URL, SmartCampaign.HAS_ADD_OPENSTAT_TAG_TO_URL)
                .process(emptySet(), SmartCampaign.PLACEMENT_TYPES)
                .process(null, SmartCampaign.DEFAULT_PERMALINK_ID);
        assertThat(modelChanges.get(0)).is(matchedBy(beanDiffer(expectedCampaignModelChanges)));
    }

    @Test
    public void convertContentPromotionCampaign() {
        LocalDate now = now();
        long id = 1L;
        BigDecimal dayBudget = BigDecimal.valueOf(1000L);
        GdDayBudgetShowMode gdDayBudgetShowMode = GdDayBudgetShowMode.STRETCHED;
        String name = "name";
        List<String> disabledSsp = List.of("ssp1", "ssp2");
        List<String> disabledIps = List.of("77.1.1.1", "77.1.1.3");

        GdCampaignEmailSettingsRequest emailSettingsRequest = new GdCampaignEmailSettingsRequest()
                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                .withCheckPositionInterval(DefaultValuesUtils.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                .withSendAccountNews(true)
                .withStopByReachDailyBudget(true)
                .withXlsReady(true)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE);
        GdCampaignSmsSettingsRequest smsSettingsRequest = new GdCampaignSmsSettingsRequest()
                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN));
        int percent = RandomNumberUtils.nextPositiveInteger(100);
        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers().withBidModifierABSegment(
                new GdUpdateBidModifierABSegment()
                        .withType(GdBidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withAdjustments(List.of(new GdUpdateBidModifierABSegmentAdjustmentItem()
                                .withPercent(percent))));
        GdCampaignBiddingStrategy strategy = new GdCampaignBiddingStrategy()
                .withPlatform(GdCampaignPlatform.SEARCH)
                .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                .withStrategyData(new GdCampaignStrategyData()
                        .withSum(BigDecimal.valueOf(5000))
                );
        GdTimeTarget timeTarget = DefaultValuesUtils.defaultGdTimeTarget()
                .withUseWorkingWeekends(true).withEnabledHolidaysMode(false)
                .withIdTimeZone(130L);

        List<Long> categoryIds = List.of(1L, 2L, 3L);

        GdUpdateContentPromotionCampaign contentPromotionCampaign = new GdUpdateContentPromotionCampaign()
                .withId(id)
                .withName(name)
                .withStartDate(now)
                .withEndDate(now)
                .withTimeTarget(timeTarget)
                .withDisabledIps(disabledIps)
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(emailSettingsRequest)
                        .withSmsSettings(smsSettingsRequest))
                .withContentLanguage(GdContentLanguage.KZ)
                .withHasExtendedGeoTargeting(true)
                .withBidModifiers(bidModifiers)
                .withDayBudget(dayBudget)
                .withDayBudgetShowMode(gdDayBudgetShowMode)
                .withBiddingStategy(strategy)
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(CAMP_HREF))
                .withIsRecommendationsManagementEnabled(true)
                .withIsPriceRecommendationsManagementEnabled(true)
                .withBrandSafetyCategories(categoryIds);

        GdUpdateCampaignUnion union =
                new GdUpdateCampaignUnion().withContentPromotionCampaign(contentPromotionCampaign);

        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(Collections.singletonList(union));

        List<? extends ModelChanges<? extends BaseCampaign>> modelChanges = gdUpdateCampaignsToCoreModelChanges(input,
                new CampaignConverterContext()
                        .withShouldProcessProperties(List.of(CampaignWithContentLanguage.CONTENT_LANGUAGE,
                                CampaignWithAllowedPageIds.ALLOWED_PAGE_IDS))
                        .withSspPlatforms(disabledSsp));
        assertThat(modelChanges).hasSize(1);

        BidModifierABSegment bidModifierABSegment =
                new BidModifierABSegment().withType(BidModifierType.AB_SEGMENT_MULTIPLIER)
                        .withAbSegmentAdjustments(List.of(new BidModifierABSegmentAdjustment().withPercent(percent)));
        DbStrategy dbStrategy = new DbStrategy();
        dbStrategy.setPlatform(CampaignsPlatform.SEARCH);
        dbStrategy.setStrategyName(StrategyName.DEFAULT_);
        dbStrategy.setStrategyData(new StrategyData().withSum(BigDecimal.valueOf(5000)));

        ModelChanges<ContentPromotionCampaign> expectedCampaignModelChanges = new ModelChanges<>(id,
                ContentPromotionCampaign.class)
                .process(name, ContentPromotionCampaign.NAME)
                .process(now, ContentPromotionCampaign.START_DATE)
                .process(now, ContentPromotionCampaign.END_DATE)
                .process(CampaignDataConverter.toTimeTarget(timeTarget), ContentPromotionCampaign.TIME_TARGET)
                .process(timeTarget.getIdTimeZone(), ContentPromotionCampaign.TIME_ZONE_ID)
                .process(null, ContentPromotionCampaign.METRIKA_COUNTERS)
                .process(CampaignType.CONTENT_PROMOTION, ContentPromotionCampaign.TYPE)
                .process(disabledIps, ContentPromotionCampaign.DISABLED_IPS)
                .process(null, ContentPromotionCampaign.MINUS_KEYWORDS)
                .process(true, ContentPromotionCampaign.ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .process(toSmsFlags(smsSettingsRequest.getEnableEvents()), ContentPromotionCampaign.SMS_FLAGS)
                .process(toTimeInterval(smsSettingsRequest.getSmsTime()), ContentPromotionCampaign.SMS_TIME)
                .process(emailSettingsRequest.getEmail(), ContentPromotionCampaign.EMAIL)
                .process(emailSettingsRequest.getWarningBalance(), ContentPromotionCampaign.WARNING_BALANCE)
                .process(ContentLanguage.KZ, ContentPromotionCampaign.CONTENT_LANGUAGE)
                .process(CampaignAttributionModel.FIRST_CLICK, ContentPromotionCampaign.ATTRIBUTION_MODEL)
                .process(contentPromotionCampaign.getHasExtendedGeoTargeting(),
                        ContentPromotionCampaign.HAS_EXTENDED_GEO_TARGETING)
                .processNotNull(contentPromotionCampaign.getUseCurrentRegion(),
                        ContentPromotionCampaign.USE_CURRENT_REGION)
                .processNotNull(contentPromotionCampaign.getUseRegularRegion(),
                        ContentPromotionCampaign.USE_REGULAR_REGION)
                .process(List.of(bidModifierABSegment), ContentPromotionCampaign.BID_MODIFIERS)
                .process(contentPromotionCampaign.getDayBudget(), ContentPromotionCampaign.DAY_BUDGET)
                .process(toCampaignDayBudgetShowMode(contentPromotionCampaign.getDayBudgetShowMode()),
                        ContentPromotionCampaign.DAY_BUDGET_SHOW_MODE)
                .process(dbStrategy, ContentPromotionCampaign.STRATEGY)
                .process(emptyList(), ContentPromotionCampaign.DISABLED_SSP)
                .process(emptyList(), ContentPromotionCampaign.DISABLED_DOMAINS)
                .process(false, ContentPromotionCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS)
                .process(CAMP_HREF, ContentPromotionCampaign.HREF)
                .process(contentPromotionCampaign.getIsRecommendationsManagementEnabled(),
                        TextCampaign.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                .process(contentPromotionCampaign.getIsPriceRecommendationsManagementEnabled(),
                        TextCampaign.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                .process(contentPromotionCampaign.getBrandSafetyCategories(),
                        ContentPromotionCampaign.BRAND_SAFETY_CATEGORIES);

        assertThat(modelChanges.get(0)).is(matchedBy(beanDiffer(expectedCampaignModelChanges)));
    }

    @Test
    public void convertMobileContentCampaign() {
        LocalDate now = now();
        long id = 1L;
        BigDecimal dayBudget = BigDecimal.valueOf(1000L);
        GdDayBudgetShowMode gdDayBudgetShowMode = GdDayBudgetShowMode.STRETCHED;
        String name = "name";
        List<String> disabledSsp = List.of("ssp1", "ssp2");
        List<String> disabledDomains = List.of("domain.com", "domain.ru");
        List<String> disabledIps = List.of("77.1.1.1", "77.1.1.3");
        List<Long> allowedPageIds = List.of(123456L, 456789L);
        boolean hasTurboApp = false;
        long mobileAppId = 5L;

        GdCampaignEmailSettingsRequest emailSettingsRequest = new GdCampaignEmailSettingsRequest()
                .withEmail(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru")
                .withSendAccountNews(false)
                .withStopByReachDailyBudget(false)
                .withXlsReady(true);
        GdCampaignSmsSettingsRequest smsSettingsRequest = new GdCampaignSmsSettingsRequest()
                .withSmsTime(toGdTimeInterval(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL))
                .withEnableEvents(Set.of(GdCampaignSmsEvent.FINISHED, GdCampaignSmsEvent.MONEY_IN));
        int percent = RandomNumberUtils.nextPositiveInteger(100);
        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers().withBidModifierDemographics(
                new GdUpdateBidModifierDemographics()
                        .withType(GdBidModifierType.DEMOGRAPHY_MULTIPLIER)
                        .withAdjustments(List.of(new GdUpdateBidModifierDemographicsAdjustmentItem()
                                .withPercent(percent)
                                .withGender(GdGenderType.FEMALE)
                                .withAge(GdAgeType._0_17))));
        GdCampaignBiddingStrategy strategy = new GdCampaignBiddingStrategy()
                .withPlatform(GdCampaignPlatform.SEARCH)
                .withStrategyName(GdCampaignStrategyName.DEFAULT_)
                .withStrategyData(new GdCampaignStrategyData()
                        .withSum(BigDecimal.valueOf(5000))
                );
        GdTimeTarget timeTarget = DefaultValuesUtils.defaultGdTimeTarget()
                .withUseWorkingWeekends(true).withEnabledHolidaysMode(false)
                .withIdTimeZone(130L);

        List<Long> categoryIds = List.of(1L, 2L, 3L);

        GdUpdateMobileContentCampaign mobileContentCampaign = new GdUpdateMobileContentCampaign()
                .withId(id)
                .withName(name)
                .withStartDate(now)
                .withEndDate(now)
                .withTimeTarget(timeTarget)
                .withDisabledPlaces(ListUtils.union(disabledDomains, disabledSsp))
                .withDisabledIps(disabledIps)
                .withNotification(new GdCampaignNotificationRequest()
                        .withEmailSettings(emailSettingsRequest)
                        .withSmsSettings(smsSettingsRequest))
                .withContentLanguage(GdContentLanguage.KZ)
                .withHasExtendedGeoTargeting(true)
                .withAllowedPageIds(allowedPageIds)
                .withAllowedDomains(ListUtils.union(disabledDomains, disabledSsp))
                .withBidModifiers(bidModifiers)
                .withDayBudget(dayBudget)
                .withBiddingStrategy(strategy)
                .withDayBudgetShowMode(gdDayBudgetShowMode)
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withBrandSafetyCategories(categoryIds)
                .withNetworkTargeting(Set.of(GdMobileContentCampaignNetworkTargeting.WI_FI))
                .withDeviceTypeTargeting(Set.of(GdMobileContentCampaignDeviceTypeTargeting.PHONE))
                .withMobileAppId(mobileAppId)
                .withAdditionalData(new GdAddUpdateCampaignAdditionalData().withHref(CAMP_HREF))
                .withIsInstalledApp(true)
                .withIsRecommendationsManagementEnabled(true)
                .withIsPriceRecommendationsManagementEnabled(true)
                .withIsSkadNetworkEnabled(true);

        GdUpdateCampaignUnion union = new GdUpdateCampaignUnion().withMobileContentCampaign(mobileContentCampaign);

        GdUpdateCampaigns input = new GdUpdateCampaigns()
                .withCampaignUpdateItems(Collections.singletonList(union));

        List<? extends ModelChanges<? extends BaseCampaign>> modelChanges = gdUpdateCampaignsToCoreModelChanges(input,
                new CampaignConverterContext()
                        .withShouldProcessProperties(List.of(CampaignWithContentLanguage.CONTENT_LANGUAGE,
                                CampaignWithAllowedPageIds.ALLOWED_PAGE_IDS))
                        .withSspPlatforms(disabledSsp));
        assertThat(modelChanges).hasSize(1);

        BidModifierDemographics bidModifierMobile =
                new BidModifierDemographics().withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                        .withDemographicsAdjustments(List.of(new BidModifierDemographicsAdjustment()
                                .withPercent(percent)
                                .withAge(AgeType._0_17)
                                .withGender(GenderType.FEMALE)));

        DbStrategy dbStrategy = new DbStrategy();
        dbStrategy.setPlatform(CampaignsPlatform.SEARCH);
        dbStrategy.setStrategyName(StrategyName.DEFAULT_);
        dbStrategy.setStrategyData(new StrategyData().withSum(BigDecimal.valueOf(5000)));

        ModelChanges<MobileContentCampaign> expectedCampaignModelChanges =
                new ModelChanges<>(id, MobileContentCampaign.class)
                        .process(CampaignType.MOBILE_CONTENT, MobileContentCampaign.TYPE)
                        .process(name, MobileContentCampaign.NAME)
                        .process(now, MobileContentCampaign.START_DATE)
                        .process(now, MobileContentCampaign.END_DATE)
                        .process(disabledSsp, MobileContentCampaign.DISABLED_SSP)
                        .process(disabledIps, MobileContentCampaign.DISABLED_IPS)
                        .process(mobileContentCampaign.getIsAloneTrafaretAllowed(),
                                MobileContentCampaign.IS_ALONE_TRAFARET_ALLOWED)
                        .process(null, MobileContentCampaign.MINUS_KEYWORDS)
                        .process(toTimeInterval(smsSettingsRequest.getSmsTime()),
                                MobileContentCampaign.SMS_TIME)
                        .process(toSmsFlags(smsSettingsRequest.getEnableEvents()),
                                MobileContentCampaign.SMS_FLAGS)
                        .process(hasTurboApp, MobileContentCampaign.HAS_TURBO_APP)
                        .process(emailSettingsRequest.getEmail(), MobileContentCampaign.EMAIL)
                        .process(emailSettingsRequest.getWarningBalance(),
                                MobileContentCampaign.WARNING_BALANCE)
                        .process(emailSettingsRequest.getStopByReachDailyBudget(),
                                MobileContentCampaign.ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                        .process(emailSettingsRequest.getSendAccountNews(),
                                MobileContentCampaign.ENABLE_SEND_ACCOUNT_NEWS)
                        .process(emailSettingsRequest.getXlsReady(),
                                MobileContentCampaign.ENABLE_OFFLINE_STAT_NOTICE)
                        .process(emailSettingsRequest.getCheckPositionInterval() != null,
                                MobileContentCampaign.ENABLE_CHECK_POSITION_EVENT)
                        .process(toCampaignWarnPlaceInterval(emailSettingsRequest.getCheckPositionInterval()),
                                MobileContentCampaign.CHECK_POSITION_INTERVAL_EVENT)
                        .process(mobileContentCampaign.getHasExtendedGeoTargeting(),
                                MobileContentCampaign.HAS_EXTENDED_GEO_TARGETING)
                        .processNotNull(mobileContentCampaign.getUseCurrentRegion(),
                                MobileContentCampaign.USE_CURRENT_REGION)
                        .processNotNull(mobileContentCampaign.getUseRegularRegion(),
                                MobileContentCampaign.USE_REGULAR_REGION)
                        .process(mobileContentCampaign.getContextLimit(), MobileContentCampaign.CONTEXT_LIMIT)
                        .process(mobileContentCampaign.getEnableCpcHold(), MobileContentCampaign.ENABLE_CPC_HOLD)
                        .process(mobileContentCampaign.getDayBudget(), MobileContentCampaign.DAY_BUDGET)
                        .process(toCampaignDayBudgetShowMode(mobileContentCampaign.getDayBudgetShowMode()),
                                MobileContentCampaign.DAY_BUDGET_SHOW_MODE)
                        .process(dbStrategy, MobileContentCampaign.STRATEGY)
                        .process(CampaignDataConverter.toTimeTarget(timeTarget), MobileContentCampaign.TIME_TARGET)
                        .process(timeTarget.getIdTimeZone(), MobileContentCampaign.TIME_ZONE_ID)
                        .processNotNull(mobileContentCampaign.getBrandSafetyCategories(),
                                MobileContentCampaign.BRAND_SAFETY_CATEGORIES)
                        .process(List.of(bidModifierMobile), MobileContentCampaign.BID_MODIFIERS)
                        .process(EnumSet.of(MobileAppDeviceTypeTargeting.PHONE),
                                MobileContentCampaign.DEVICE_TYPE_TARGETING)
                        .process(EnumSet.of(MobileAppNetworkTargeting.WI_FI),
                                MobileContentCampaign.NETWORK_TARGETING)
                        .process(true, MobileContentCampaign.IS_INSTALLED_APP)
                        .processNotNull(mobileAppId, MobileContentCampaign.MOBILE_APP_ID)
                        .process(disabledDomains, MobileContentCampaign.DISABLED_DOMAINS)
                        .process(ContentLanguage.KZ, MobileContentCampaign.CONTENT_LANGUAGE)
                        .process(allowedPageIds, MobileContentCampaign.ALLOWED_PAGE_IDS)
                        .process(disabledDomains, MobileContentCampaign.ALLOWED_DOMAINS)
                        .process(disabledSsp, MobileContentCampaign.ALLOWED_SSP)
                        .process(CAMP_HREF, MobileContentCampaign.HREF)
                        .process(false, ContentPromotionCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS)
                        .process(mobileContentCampaign.getIsRecommendationsManagementEnabled(),
                                TextCampaign.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                        .process(mobileContentCampaign.getIsPriceRecommendationsManagementEnabled(),
                                TextCampaign.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED)
                        .process(true, MobileContentCampaign.IS_SKAD_NETWORK_ENABLED);

        assertThat(modelChanges.get(0)).is(matchedBy(beanDiffer(expectedCampaignModelChanges)));
    }
}
