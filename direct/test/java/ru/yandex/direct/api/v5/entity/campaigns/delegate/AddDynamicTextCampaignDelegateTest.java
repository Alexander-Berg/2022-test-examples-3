package ru.yandex.direct.api.v5.entity.campaigns.delegate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.yandex.direct.api.v5.campaigns.AddRequest;
import com.yandex.direct.api.v5.campaigns.AddResponse;
import com.yandex.direct.api.v5.campaigns.CampaignAddItem;
import com.yandex.direct.api.v5.campaigns.DailyBudget;
import com.yandex.direct.api.v5.campaigns.DailyBudgetModeEnum;
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSetting;
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignSettingsEnum;
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignStrategyAdd;
import com.yandex.direct.api.v5.campaigns.EmailSettings;
import com.yandex.direct.api.v5.campaigns.Notification;
import com.yandex.direct.api.v5.campaigns.PlacementTypesEnum;
import com.yandex.direct.api.v5.campaigns.PriorityGoalsArray;
import com.yandex.direct.api.v5.campaigns.PriorityGoalsItem;
import com.yandex.direct.api.v5.campaigns.SmsEventsEnum;
import com.yandex.direct.api.v5.campaigns.SmsSettings;
import com.yandex.direct.api.v5.campaigns.TimeTargetingAdd;
import com.yandex.direct.api.v5.campaigns.TimeTargetingOnPublicHolidays;
import com.yandex.direct.api.v5.general.ArrayOfInteger;
import com.yandex.direct.api.v5.general.ArrayOfString;
import com.yandex.direct.api.v5.general.AttributionModelEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.campaigns.converter.CampaignsAddRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.PlacementType;
import ru.yandex.direct.core.entity.campaign.model.RequestSource;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.entity.time.model.TimeInterval;
import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone;
import ru.yandex.direct.core.entity.timetarget.repository.GeoTimezoneRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.libs.timetarget.TimeTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.campaigns.delegate.AddCampaignDelegateConstants.DEFAULT_CHECK_POSITION_INTERVAL_EVENT;
import static ru.yandex.direct.api.v5.entity.campaigns.delegate.AddCampaignDelegateConstants.DEFAULT_ENABLE_CHECK_POSITION_EVENT;
import static ru.yandex.direct.api.v5.entity.campaigns.delegate.AddCampaignDelegateConstants.DEFAULT_ENABLE_SEND_ACCOUNT_NEWS;
import static ru.yandex.direct.api.v5.entity.campaigns.delegate.AddCampaignDelegateConstants.DEFAULT_SMS_INTERVAL;
import static ru.yandex.direct.api.v5.entity.campaigns.delegate.AddCampaignDelegateConstants.DEFAULT_WARNING_BALANCE;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.defaultTimeTarget;

@Api5Test
@RunWith(SpringRunner.class)
public class AddDynamicTextCampaignDelegateTest {

    private static final String NAME = "Тестовая кампания";

    private static final GeoTimezone AMSTERDAM_TIMEZONE = new GeoTimezone()
            .withTimezone(ZoneId.of("Europe/Amsterdam"))
            .withTimezoneId(174L);

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Integer COUNTER_ID = 1;

    private static final Long VALID_GOAL_ID = 55L;

    private static final String SSP_PLATFORM_1 = "Rubicon";
    private static final String SSP_PLATFORM_2 = "sspplatform.ru";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private CampaignOperationService campaignOperationService;

    @Autowired
    private ResultConverter resultConverter;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private FeatureService featureService;

    @Mock
    private GeoTimezoneRepository geoTimezoneRepository;

    @Mock
    private ApiAuthenticationSource authenticationSource;

    @Autowired
    private UserService userService;

    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private SspPlatformsRepository sspPlatformsRepository;

    private GenericApiService genericApiService;
    private AddCampaignsDelegate delegate;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().setCurrentClient(clientInfo.getClientId());

        ApiUser user = new ApiUser()
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId());

        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(user);
        when(auth.getChiefSubclient()).thenReturn(user);

        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());

        genericApiService = new GenericApiService(
                apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));

        when(geoTimezoneRepository.getByTimeZones(eq(ImmutableList.of(AMSTERDAM_TIMEZONE.getTimezone().getId()))))
                .thenReturn(Map.of(AMSTERDAM_TIMEZONE.getTimezone().getId(), AMSTERDAM_TIMEZONE));

        when(authenticationSource.getRequestSource())
                .thenReturn(RequestSource.API_DEFAULT);

        var requestConverter = new CampaignsAddRequestConverter(userService, geoTimezoneRepository,
                authenticationSource, sspPlatformsRepository, featureService);

        delegate = new AddCampaignsDelegate(
                auth,
                campaignOperationService,
                requestConverter,
                resultConverter,
                ppcPropertiesSupport,
                featureService);

        metrikaClientStub.addUserCounter(clientInfo.getUid(), COUNTER_ID);
        metrikaClientStub.addCounterGoal(COUNTER_ID, VALID_GOAL_ID.intValue());

        steps.sspPlatformsSteps().addSspPlatforms(List.of(SSP_PLATFORM_1, SSP_PLATFORM_2));
    }

    @Test
    public void addDynamicTextCampaign_withDefaultSettings() {
        DynamicTextCampaignAddItem dynamicTextCampaignAddItem = defaultDynamicTextCampaignAddItem();
        AddRequest request = getAddRequest(NAME, dynamicTextCampaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        DynamicCampaign actual = (DynamicCampaign) campaigns.get(campaignId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getHasAddMetrikaTagToUrl()).as("hasAddMetrikaTagToUrl").isTrue();
            softly.assertThat(actual.getHasAddOpenstatTagToUrl()).as("hasAddOpenstatTagToUrl").isFalse();
            softly.assertThat(actual.getHasExtendedGeoTargeting()).as("hasExtendedGeoTargeting").isTrue();
            softly.assertThat(actual.getEnableCompanyInfo()).as("enableCompanyInfo").isTrue();
            softly.assertThat(actual.getHasTitleSubstitution()).as("hasTitleSubstitution").isTrue();
            softly.assertThat(actual.getHasSiteMonitoring()).as("hasSiteMonitoring").isFalse();
            softly.assertThat(actual.getFavoriteForUids()).as("favoriteForUids").isNull();
            softly.assertThat(actual.getIsOrderPhraseLengthPrecedenceEnabled())
                    .as("orderPhraseLengthPrecedenceEnabled").isFalse();
        });
    }

    @Test
    public void addDynamicTextCampaign_withCustomSettings() {
        DynamicTextCampaignAddItem dynamicTextCampaignAddItem = defaultDynamicTextCampaignAddItem()
                .withSettings(List.of(
                        DynamicTextCampaignSetting(DynamicTextCampaignSettingsEnum.ADD_METRICA_TAG, YesNoEnum.NO),
                        DynamicTextCampaignSetting(DynamicTextCampaignSettingsEnum.ADD_OPENSTAT_TAG, YesNoEnum.YES),
                        DynamicTextCampaignSetting(DynamicTextCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING,
                                YesNoEnum.NO),
                        DynamicTextCampaignSetting(DynamicTextCampaignSettingsEnum.ENABLE_COMPANY_INFO, YesNoEnum.NO),
                        DynamicTextCampaignSetting(DynamicTextCampaignSettingsEnum.ENABLE_EXTENDED_AD_TITLE,
                                YesNoEnum.NO),
                        DynamicTextCampaignSetting(DynamicTextCampaignSettingsEnum.ENABLE_SITE_MONITORING,
                                YesNoEnum.YES),
                        DynamicTextCampaignSetting(DynamicTextCampaignSettingsEnum.ADD_TO_FAVORITES, YesNoEnum.YES)
                ));
        AddRequest request = getAddRequest(NAME, dynamicTextCampaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        DynamicCampaign actual = (DynamicCampaign) campaigns.get(campaignId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getHasAddMetrikaTagToUrl()).as("hasAddMetrikaTagToUrl").isFalse();
            softly.assertThat(actual.getHasAddOpenstatTagToUrl()).as("hasAddOpenstatTagToUrl").isTrue();
            softly.assertThat(actual.getHasExtendedGeoTargeting()).as("hasExtendedGeoTargeting").isFalse();
            softly.assertThat(actual.getEnableCompanyInfo()).as("enableCompanyInfo").isFalse();
            softly.assertThat(actual.getHasTitleSubstitution()).as("hasTitleSubstitution").isFalse();
            softly.assertThat(actual.getHasSiteMonitoring()).as("hasSiteMonitoring").isTrue();
            softly.assertThat(actual.getFavoriteForUids()).as("favoriteForUids")
                    .isEqualTo(Set.of(clientInfo.getUid()));
        });
    }

    @Test
    public void addDynamicTextCampaign_allFields() {
        TimeTargetingAdd timeTargeting = new TimeTargetingAdd()
                .withHolidaysSchedule(new TimeTargetingOnPublicHolidays()
                        .withSuspendOnHolidays(YesNoEnum.YES))
                .withConsiderWorkingWeekends(YesNoEnum.NO);

        TimeTarget expectedTimeTarget = TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVW" +
                "X3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUV" +
                "WX7ABCDEFGHIJKLMNOPQRSTUVWX8;p:o");

        AttributionModelEnum attributionModel = AttributionModelEnum.LC;
        var expectedAttributionModel = CampaignAttributionModel.LAST_CLICK;
        var trackingParams = "utm_param=value";
        DynamicTextCampaignAddItem dynamicTextCampaignAddItem = new DynamicTextCampaignAddItem()
                .withBiddingStrategy(defaultStrategy())
                .withAttributionModel(attributionModel)
                .withPriorityGoals(new PriorityGoalsArray()
                        .withItems(List.of(new PriorityGoalsItem()
                                .withValue(13000000)
                                .withGoalId(VALID_GOAL_ID))))
                .withCounterIds(new ArrayOfInteger().withItems(COUNTER_ID))
                .withPlacementTypes(
                        new com.yandex.direct.api.v5.campaigns.PlacementType()
                                .withType(PlacementTypesEnum.SEARCH_RESULTS)
                                .withValue(YesNoEnum.NO))
                .withTrackingParams(trackingParams);

        String fio = "Ivanov Ivan";
        var startDate = LocalDate.now().plusDays(10);
        var endDate = LocalDate.now().plusDays(20);
        int warningBalance = 20;
        YesNoEnum sendWarn = YesNoEnum.YES;
        Boolean expectedSendWarn = Boolean.TRUE;
        YesNoEnum sendAccountNews = YesNoEnum.YES;
        Boolean expectedSendNews = Boolean.TRUE;
        int checkPositionIntervalEvent = 30;
        CampaignWarnPlaceInterval expectedCheckPositionIntervalEvent = CampaignWarnPlaceInterval._30;
        String email = "test@email.com";
        String smsTimeFrom = "9:15";
        String smsTimeTo = "23:45";
        TimeInterval expectedSmsInterval = new TimeInterval()
                .withStartHour(9)
                .withStartMinute(15)
                .withEndHour(23)
                .withEndMinute(45);
        var smsEvents = List.of(SmsEventsEnum.MODERATION);
        SmsFlag[] expectedSmsEvents = {SmsFlag.MODERATE_RESULT_SMS};
        long dailyBudgetAmount = 300888888;
        BigDecimal expectedDailyBudget = BigDecimal.valueOf(300.89);
        DailyBudgetModeEnum dailyBudgetMode = DailyBudgetModeEnum.DISTRIBUTED;
        DayBudgetShowMode expectedDailyBudgetMode = DayBudgetShowMode.STRETCHED;
        List<String> negativeKeywords = List.of("negative", "keywords");
        List<String> excludedSites = List.of(SSP_PLATFORM_1.toLowerCase(), SSP_PLATFORM_2, "google.ru 1", "google.com");
        List<String> expectedDisabledSsp = List.of(SSP_PLATFORM_1, SSP_PLATFORM_2);
        List<String> expectedDisabledDomains = List.of(SSP_PLATFORM_2, "google.ru", "google.com");

        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(startDate))
                .withEndDate(DATETIME_FORMATTER.format(endDate))
                .withDynamicTextCampaign(dynamicTextCampaignAddItem)
                .withTimeZone(AMSTERDAM_TIMEZONE.getTimezone().getId())
                .withTimeTargeting(timeTargeting)
                .withClientInfo(fio)
                .withNotification(new Notification()
                        .withEmailSettings(new EmailSettings()
                                .withEmail(email)
                                .withCheckPositionInterval(checkPositionIntervalEvent)
                                .withSendAccountNews(sendAccountNews)
                                .withSendWarnings(sendWarn)
                                .withWarningBalance(warningBalance))
                        .withSmsSettings(new SmsSettings()
                                .withEvents(smsEvents)
                                .withTimeFrom(smsTimeFrom)
                                .withTimeTo(smsTimeTo)))
                .withDailyBudget(new DailyBudget()
                        .withAmount(dailyBudgetAmount)
                        .withMode(dailyBudgetMode))
                .withNegativeKeywords(new ArrayOfString()
                        .withItems(negativeKeywords))
                .withExcludedSites(new ArrayOfString()
                        .withItems(excludedSites));

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        DynamicCampaign actual = (DynamicCampaign) campaigns.get(campaignId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getName()).as("name").isEqualTo(NAME);
            softly.assertThat(actual.getTimeZoneId()).as("timezonId").isEqualTo(
                    AMSTERDAM_TIMEZONE.getTimezoneId());
            softly.assertThat(actual.getTimeTarget()).as("timeTarget").isEqualTo(expectedTimeTarget);
            softly.assertThat(actual.getFio()).as("fio").isEqualTo(fio);
            softly.assertThat(actual.getStartDate()).as("startDate").isEqualTo(startDate);
            softly.assertThat(actual.getEndDate()).as("endDate").isEqualTo(endDate);
            softly.assertThat(actual.getWarningBalance()).as("warningBalance").isEqualTo(warningBalance);
            softly.assertThat(actual.getEmail()).as("email").isEqualTo(email);
            softly.assertThat(actual.getEnableCheckPositionEvent()).as("enableCheckPositionEvent")
                    .isEqualTo(expectedSendWarn);
            softly.assertThat(actual.getCheckPositionIntervalEvent()).as("checkPositionIntervalEvent")
                    .isEqualTo(expectedCheckPositionIntervalEvent);
            softly.assertThat(actual.getEnableSendAccountNews()).as("sendAccountNews")
                    .isEqualTo(expectedSendNews);
            softly.assertThat(actual.getSmsTime()).as("smsTime").isEqualTo(expectedSmsInterval);
            softly.assertThat(actual.getSmsFlags()).as("smsFlags")
                    .containsExactlyInAnyOrder(expectedSmsEvents);
            softly.assertThat(actual.getDayBudget()).as("dayBudget").isEqualTo(expectedDailyBudget);
            softly.assertThat(actual.getDayBudgetShowMode()).as("dayBudgetShowMode").
                    isEqualTo(expectedDailyBudgetMode);
            softly.assertThat(actual.getMinusKeywords()).as("minusKeywords")
                    .containsExactlyInAnyOrder(negativeKeywords.toArray(new String[0]));
            softly.assertThat(actual.getDisabledDomains()).as("disabledDomains")
                    .containsExactlyInAnyOrder(expectedDisabledDomains.toArray(new String[0]));

            // не можем протестировать blockedIps, т.к. в network-config.allow-all.json все ip объявляются внутренними,
            // а ядро не разрешает блокировать внутренние ip
            softly.assertThat(actual.getDisabledIps()).as("disabledIps").isNull();

            softly.assertThat(actual.getDisabledSsp()).as("disabledSsp")
                    .containsExactlyInAnyOrder(expectedDisabledSsp.toArray(new String[0]));
            softly.assertThat(actual.getAttributionModel()).as("attributionModel")
                    .isEqualTo(expectedAttributionModel);
            softly.assertThat(actual.getMeaningfulGoals()).as("meaningfulGoals")
                    .isEqualTo(List.of(
                            new MeaningfulGoal().withGoalId(VALID_GOAL_ID)
                                    .withConversionValue(new BigDecimal("13"))));
            softly.assertThat(actual.getMetrikaCounters()).as("metrikaCounters")
                    .containsExactlyInAnyOrder(Long.valueOf(COUNTER_ID));
            softly.assertThat(actual.getSource()).as("source")
                    .isEqualTo(CampaignSource.API);
            softly.assertThat(actual.getPlacementTypes()).as("placementTypes")
                    .isEqualTo(Set.of(PlacementType.ADV_GALLERY));
            softly.assertThat(actual.getBannerHrefParams()).as("hrefParams")
                    .isEqualTo(trackingParams);
        });
    }

    @Test
    public void addDynamicTextCampaign_onlyRequiredFields() {
        DynamicTextCampaignAddItem dynamicTextCampaignAddItem = new DynamicTextCampaignAddItem()
                .withBiddingStrategy(defaultStrategy());

        var startDate = LocalDate.now();
        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(startDate))
                .withDynamicTextCampaign(dynamicTextCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        DynamicCampaign actual = (DynamicCampaign) campaigns.get(campaignId);

        User user = userService.getUser(clientInfo.getUid());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getName()).as("name").isEqualTo(NAME);
            softly.assertThat(actual.getTimeZoneId()).as("timezonId").isEqualTo(0);
            softly.assertThat(actual.getTimeTarget()).as("timeTarget").isEqualTo(defaultTimeTarget());
            softly.assertThat(actual.getFio()).as("fio").isEqualTo(user.getFio());
            softly.assertThat(actual.getStartDate()).as("startDate").isEqualTo(startDate);
            softly.assertThat(actual.getEndDate()).as("endDate").isNull();
            softly.assertThat(actual.getWarningBalance()).as("warningBalance")
                    .isEqualTo(DEFAULT_WARNING_BALANCE);
            softly.assertThat(actual.getEmail()).as("email").isEqualTo(user.getEmail());
            softly.assertThat(actual.getEnableCheckPositionEvent()).as("enableCheckPositionEvent")
                    .isEqualTo(DEFAULT_ENABLE_CHECK_POSITION_EVENT);
            softly.assertThat(actual.getCheckPositionIntervalEvent()).as("checkPositionIntervalEvent")
                    .isEqualTo(DEFAULT_CHECK_POSITION_INTERVAL_EVENT);
            softly.assertThat(actual.getEnableSendAccountNews()).as("sendAccountNews")
                    .isEqualTo(DEFAULT_ENABLE_SEND_ACCOUNT_NEWS);
            softly.assertThat(actual.getSmsTime()).as("smsTime").isEqualTo(DEFAULT_SMS_INTERVAL);
            softly.assertThat(actual.getSmsFlags()).as("smsFlags").isEmpty();
            softly.assertThat(actual.getDayBudget()).as("dayBudget").isEqualTo(new BigDecimal("0.00"));
            softly.assertThat(actual.getDayBudgetShowMode()).as("dayBudgetShowMode").
                    isEqualTo(DayBudgetShowMode.DEFAULT_);
            softly.assertThat(actual.getMinusKeywords()).as("minusKeywords").isEmpty();
            softly.assertThat(actual.getDisabledIps()).as("disabledIps").isNull();
            softly.assertThat(actual.getDisabledSsp()).as("disabledSsp").isEmpty();
            softly.assertThat(actual.getAttributionModel()).as("attributionModel")
                    .isEqualTo(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK);
            softly.assertThat(actual.getMeaningfulGoals()).as("meaningfulGoals").isNull();
            softly.assertThat(actual.getMetrikaCounters()).as("metrikaCounters").isNull();
            softly.assertThat(actual.getSource()).as("source")
                    .isEqualTo(CampaignSource.API);
            softly.assertThat(actual.getPlacementTypes()).as("placementTypes")
                    .isEqualTo(Set.of());
        });
    }

    private static AddRequest getAddRequest(String name, DynamicTextCampaignAddItem dynamicTextCampaignAddItem) {
        var campaignAddItem = new CampaignAddItem()
                .withName(name)
                .withStartDate(DATETIME_FORMATTER.format(LocalDateTime.now()))
                .withDynamicTextCampaign(dynamicTextCampaignAddItem);

        return new AddRequest().withCampaigns(campaignAddItem);
    }

    private static DynamicTextCampaignAddItem defaultDynamicTextCampaignAddItem() {
        DynamicTextCampaignStrategyAdd strategy = defaultStrategy();

        return new DynamicTextCampaignAddItem()
                .withBiddingStrategy(strategy);
    }

    private static DynamicTextCampaignStrategyAdd defaultStrategy() {
        return new DynamicTextCampaignStrategyAdd()
                .withSearch(new DynamicTextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(DynamicTextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION))
                .withNetwork(new DynamicTextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(DynamicTextCampaignNetworkStrategyTypeEnum.SERVING_OFF));
    }

    private static DynamicTextCampaignSetting DynamicTextCampaignSetting(DynamicTextCampaignSettingsEnum option,
                                                                         YesNoEnum value) {
        return new DynamicTextCampaignSetting().withOption(option).withValue(value);
    }

}
