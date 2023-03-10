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
import com.yandex.direct.api.v5.campaigns.EmailSettings;
import com.yandex.direct.api.v5.campaigns.Notification;
import com.yandex.direct.api.v5.campaigns.PriorityGoalsArray;
import com.yandex.direct.api.v5.campaigns.PriorityGoalsItem;
import com.yandex.direct.api.v5.campaigns.SmartCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.SmartCampaignSetting;
import com.yandex.direct.api.v5.campaigns.SmartCampaignSettingsEnum;
import com.yandex.direct.api.v5.campaigns.SmartCampaignStrategyAdd;
import com.yandex.direct.api.v5.campaigns.SmsEventsEnum;
import com.yandex.direct.api.v5.campaigns.SmsSettings;
import com.yandex.direct.api.v5.campaigns.StrategyAverageCrrAdd;
import com.yandex.direct.api.v5.campaigns.StrategyNetworkDefaultAdd;
import com.yandex.direct.api.v5.campaigns.TimeTargetingAdd;
import com.yandex.direct.api.v5.campaigns.TimeTargetingOnPublicHolidays;
import com.yandex.direct.api.v5.general.ArrayOfString;
import com.yandex.direct.api.v5.general.AttributionModelEnum;
import com.yandex.direct.api.v5.general.ExceptionNotification;
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
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.RequestSource;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.DirectAuthContextService;
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
import static ru.yandex.direct.feature.FeatureName.CRR_STRATEGY_ALLOWED;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.defaultTimeTarget;

@Api5Test
@RunWith(SpringRunner.class)
public class AddSmartCampaignDelegateTest {

    private static final String NAME = "???????????????? ????????????????";

    private static final GeoTimezone AMSTERDAM_TIMEZONE = new GeoTimezone()
            .withTimezone(ZoneId.of("Europe/Amsterdam"))
            .withTimezoneId(174L);

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final List<String> TIME_TARGETING_SCHEDULE = List.of(
            "1,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "2,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "3,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "4,100,100,100,100,100,100,100,100,100,10,20,30,40,100,50,60,70,160,170,180,190,200,150,100",
            "6,10,20,30,50,100,100,100,100,100,100,100,10,20,30,50,20,30,40,90,80,70,60,50,100",
            "7,10,20,30,50,100,100,100,100,100,100,100,10,20,30,50,20,30,40,90,80,70,60,50,100"
    );

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

    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private UserService userService;

    @Autowired
    private SspPlatformsRepository sspPlatformsRepository;

    @Autowired
    private DirectAuthContextService directAuthContextService;

    @Mock
    private GeoTimezoneRepository geoTimezoneRepository;

    @Mock
    private ApiAuthenticationSource authenticationSource;

    private GenericApiService genericApiService;
    private AddCampaignsDelegate delegate;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        directAuthContextService.usingClientId(clientInfo.getClientId());

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

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), CRR_STRATEGY_ALLOWED, true);

        metrikaClientStub.addUserCounter(clientInfo.getUid(), COUNTER_ID);
        metrikaClientStub.addCounterGoal(COUNTER_ID, VALID_GOAL_ID.intValue());

        steps.sspPlatformsSteps().addSspPlatforms(List.of(SSP_PLATFORM_1, SSP_PLATFORM_2));
    }

    @Test
    public void addSmartCampaign_withDefaultSettings() {
        SmartCampaignAddItem smartCampaignAddItem = defaultCampaignAddItem();
        AddRequest request = getAddRequest(NAME, smartCampaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        SmartCampaign actual = (SmartCampaign) campaigns.get(campaignId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getFavoriteForUids()).as("favoriteForUids").isNull();
        });
    }

    @Test
    public void addSmartCampaign_withCustomSettings() {
        SmartCampaignAddItem smartCampaignAddItem = defaultCampaignAddItem()
                .withSettings(List.of(
                        SmartCampaignSetting(SmartCampaignSettingsEnum.ADD_TO_FAVORITES, YesNoEnum.YES)
                ));
        AddRequest request = getAddRequest(NAME, smartCampaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        SmartCampaign actual = (SmartCampaign) campaigns.get(campaignId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getFavoriteForUids()).as("favoriteForUids")
                    .isEqualTo(Set.of(clientInfo.getUid()));
        });
    }

    @Test
    public void addCampaign_campaignAddItemWithLimitPercentNotDividingTen_ValidationError() {
        var smartCampaignAddItem = defaultCampaignAddItem();

        smartCampaignAddItem.getBiddingStrategy()
                .withNetwork(new SmartCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(SmartCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT)
                        .withNetworkDefault(new StrategyNetworkDefaultAdd().withLimitPercent(39)));

        var campaignAddItem = newCampaignAddItem()
                .withSmartCampaign(smartCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(5005);
    }

    @Test
    public void addCampaign_campaignAddItemWithLimitPercentNotInRange_ValidationError() {
        var smartCampaignAddItem = defaultCampaignAddItem();

        smartCampaignAddItem.getBiddingStrategy()
                .withNetwork(new SmartCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(SmartCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT)
                        .withNetworkDefault(new StrategyNetworkDefaultAdd().withLimitPercent(39)));

        var campaignAddItem = newCampaignAddItem()
                .withSmartCampaign(smartCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(5005);
    }

    @Test
    public void addSmartCampaign_allFields() {
        TimeTargetingAdd timeTargeting = new TimeTargetingAdd()
                .withSchedule(new ArrayOfString()
                        .withItems(TIME_TARGETING_SCHEDULE))
                .withHolidaysSchedule(new TimeTargetingOnPublicHolidays()
                        .withBidPercent(50)
                        .withStartHour(9)
                        .withEndHour(23)
                        .withSuspendOnHolidays(YesNoEnum.NO))
                .withConsiderWorkingWeekends(YesNoEnum.YES);

        TimeTarget expectedTimeTarget = TimeTarget.parseRawString("1ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX2ABCDEFGHIJ" +
                "bKcLdMeNOfPgQhRqSrTsUtVuWpX3ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX4ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtV" +
                "uWpX5ABCDEFGHIJKLMNOPQRSTUVWX6AbBcCdDfEFGHIJKLbMcNdOfPcQdReSjTiUhVgWfX7AbBcCdDfEFGHIJKLbMcNdOfPcQdR" +
                "eSjTiUhVgWfX8JfKfLfMfNfOfPfQfRfSfTfUfVfWf9");

        AttributionModelEnum attributionModel = AttributionModelEnum.LSC;
        var expectedAttributionModel = CampaignAttributionModel.LAST_SIGNIFICANT_CLICK;
        var trackingParams = "utm_param=value";
        SmartCampaignAddItem smartCampaignAddItem = defaultCampaignAddItem()
                .withAttributionModel(attributionModel)
                .withPriorityGoals(new PriorityGoalsArray()
                        .withItems(List.of(new PriorityGoalsItem()
                                .withValue(13000000)
                                .withGoalId(VALID_GOAL_ID))))
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
        String smsTimeFrom = "0:00";
        String smsTimeTo = "12:30";
        TimeInterval expectedSmsInterval = new TimeInterval()
                .withStartHour(0)
                .withStartMinute(0)
                .withEndHour(12)
                .withEndMinute(30);
        var smsEvents = List.of(SmsEventsEnum.MONEY_OUT);
        SmsFlag[] expectedSmsEvents = {SmsFlag.ACTIVE_ORDERS_MONEY_OUT_SMS,
                SmsFlag.ACTIVE_ORDERS_MONEY_WARNING_SMS};
        List<String> negativeKeywords = List.of("negative", "keywords");
        List<String> excludedSites = List.of(SSP_PLATFORM_1.toUpperCase(), SSP_PLATFORM_2, "google.ru 1", "google.com");
        List<String> expectedDisabledSsp = List.of(SSP_PLATFORM_1, SSP_PLATFORM_2);
        List<String> expectedDisabledDomains = List.of(SSP_PLATFORM_2, "google.ru", "google.com");

        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(startDate))
                .withEndDate(DATETIME_FORMATTER.format(endDate))
                .withSmartCampaign(smartCampaignAddItem)
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
                .withNegativeKeywords(new ArrayOfString()
                        .withItems(negativeKeywords))
                .withExcludedSites(new ArrayOfString()
                        .withItems(excludedSites));

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        SmartCampaign actual = (SmartCampaign) campaigns.get(campaignId);

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
            softly.assertThat(actual.getMinusKeywords()).as("minusKeywords")
                    .containsExactlyInAnyOrder(negativeKeywords.toArray(new String[0]));
            softly.assertThat(actual.getDisabledSsp()).as("disabledSsp")
                    .containsExactlyInAnyOrder(expectedDisabledSsp.toArray(new String[0]));
            softly.assertThat(actual.getDisabledDomains()).as("disabledDomains")
                    .containsExactlyInAnyOrder(expectedDisabledDomains.toArray(new String[0]));

            // ???? ?????????? ???????????????????????????? blockedIps, ??.??. ?? network-config.allow-all.json ?????? ip ?????????????????????? ??????????????????????,
            // ?? ???????? ???? ?????????????????? ?????????????????????? ???????????????????? ip
            softly.assertThat(actual.getDisabledIps()).as("disabledIps").isNull();

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
            softly.assertThat(actual.getContextLimit()).as("contextLimit")
                    .isEqualTo(0);
            softly.assertThat(actual.getBannerHrefParams()).as("hrefParams")
                    .isEqualTo(trackingParams);
        });
    }

    @Test
    public void addSmartCampaign_onlyRequiredFields() {
        SmartCampaignAddItem smartCampaignAddItem = defaultCampaignAddItem();

        var startDate = LocalDate.now();
        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(startDate))
                .withSmartCampaign(smartCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        SmartCampaign actual = (SmartCampaign) campaigns.get(campaignId);

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
            softly.assertThat(actual.getMinusKeywords()).as("minusKeywords").isEmpty();
            softly.assertThat(actual.getDisabledIps()).as("disabledIps").isNull();
            softly.assertThat(actual.getDisabledSsp()).as("disabledSsp").isEmpty();
            softly.assertThat(actual.getAttributionModel()).as("attributionModel")
                    .isEqualTo(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK);
            softly.assertThat(actual.getMeaningfulGoals()).as("meaningfulGoals").isNull();
            softly.assertThat(actual.getMetrikaCounters()).as("metrikaCounters")
                    .containsExactlyInAnyOrder(Long.valueOf(COUNTER_ID));
            softly.assertThat(actual.getSource()).as("source")
                    .isEqualTo(CampaignSource.API);
            softly.assertThat(actual.getContextLimit()).as("contextLimit")
                    .isEqualTo(0);
        });

    }

    private static CampaignAddItem newCampaignAddItem() {
        return new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(LocalDateTime.now()));
    }


    private static AddRequest getAddRequest(String name, SmartCampaignAddItem smartCampaignAddItem) {
        var campaignAddItem = new CampaignAddItem()
                .withName(name)
                .withStartDate(DATETIME_FORMATTER.format(LocalDateTime.now()))
                .withSmartCampaign(smartCampaignAddItem);

        return new AddRequest().withCampaigns(campaignAddItem);
    }

    private static SmartCampaignAddItem defaultCampaignAddItem() {
        var strategy = defaultStrategy();

        return new SmartCampaignAddItem()
                .withBiddingStrategy(strategy)
                .withCounterId(COUNTER_ID);
    }

    private static SmartCampaignStrategyAdd defaultStrategy() {
        return new SmartCampaignStrategyAdd()
                .withSearch(new SmartCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(SmartCampaignSearchStrategyTypeEnum.AVERAGE_CRR)
                        .withAverageCrr(new StrategyAverageCrrAdd()
                                .withCrr(100)
                                .withGoalId(VALID_GOAL_ID)
                                .withWeeklySpendLimit(300000000L)))
                .withNetwork(new SmartCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(SmartCampaignNetworkStrategyTypeEnum.SERVING_OFF));
    }

    private static SmartCampaignSetting SmartCampaignSetting(SmartCampaignSettingsEnum option, YesNoEnum value) {
        return new SmartCampaignSetting().withOption(option).withValue(value);
    }

}
