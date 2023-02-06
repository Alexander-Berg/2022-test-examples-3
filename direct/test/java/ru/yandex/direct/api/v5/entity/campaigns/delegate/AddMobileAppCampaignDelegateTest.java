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
import com.yandex.direct.api.v5.campaigns.EmailSettings;
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSetting;
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignSettingsEnum;
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignStrategyAdd;
import com.yandex.direct.api.v5.campaigns.Notification;
import com.yandex.direct.api.v5.campaigns.SmsEventsEnum;
import com.yandex.direct.api.v5.campaigns.SmsSettings;
import com.yandex.direct.api.v5.campaigns.StrategyMaximumClicksAdd;
import com.yandex.direct.api.v5.campaigns.StrategyNetworkDefaultAdd;
import com.yandex.direct.api.v5.campaigns.TimeTargetingAdd;
import com.yandex.direct.api.v5.campaigns.TimeTargetingOnPublicHolidays;
import com.yandex.direct.api.v5.general.ArrayOfString;
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
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
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
public class AddMobileAppCampaignDelegateTest {

    private static final String NAME = "Тестовая кампания";

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
    private UserService userService;

    @Autowired
    private SspPlatformsRepository sspPlatformsRepository;

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

        steps.sspPlatformsSteps().addSspPlatforms(List.of(SSP_PLATFORM_1, SSP_PLATFORM_2));
    }

    @Test
    public void addMobileAppCampaign_withDefaultSettings() {
        MobileAppCampaignAddItem mobileAppCampaignAddItem = defaultAddItem();
        AddRequest request = getAddRequest(NAME, mobileAppCampaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        MobileContentCampaign actual = (MobileContentCampaign) campaigns.get(campaignId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getHasExtendedGeoTargeting()).as("hasExtendedGeoTargeting").isTrue();
            softly.assertThat(actual.getEnableCpcHold()).as("enableCpcHold").isTrue();
            softly.assertThat(actual.getFavoriteForUids()).as("favoriteForUids").isNull();
            softly.assertThat(actual.getIsOrderPhraseLengthPrecedenceEnabled())
                    .as("orderPhraseLengthPrecedenceEnabled").isFalse();
        });
    }

    @Test
    public void addMobileAppCampaign_withCustomSettings() {
        MobileAppCampaignAddItem mobileAppCampaignAddItem = defaultAddItem()
                .withSettings(List.of(
                        MobileAppCampaignSetting(MobileAppCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING,
                                YesNoEnum.NO),
                        MobileAppCampaignSetting(MobileAppCampaignSettingsEnum.MAINTAIN_NETWORK_CPC, YesNoEnum.NO),
                        MobileAppCampaignSetting(MobileAppCampaignSettingsEnum.ADD_TO_FAVORITES, YesNoEnum.YES)
                ));
        AddRequest request = getAddRequest(NAME, mobileAppCampaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        MobileContentCampaign actual = (MobileContentCampaign) campaigns.get(campaignId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getHasExtendedGeoTargeting()).as("hasExtendedGeoTargeting").isFalse();
            softly.assertThat(actual.getEnableCpcHold()).as("enableCpcHold").isFalse();
            softly.assertThat(actual.getFavoriteForUids()).as("favoriteForUids")
                    .isEqualTo(Set.of(clientInfo.getUid()));
            softly.assertThat(actual.getIsOrderPhraseLengthPrecedenceEnabled())
                    .as("orderPhraseLengthPrecedenceEnabled").isFalse();
        });
    }

    @Test
    public void addCampaign_campaignAddItemWithLimitPercentNotDividingTen_ValidationError() {
        var strategy = new MobileAppCampaignStrategyAdd()
                .withSearch(new MobileAppCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(MobileAppCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS)
                        .withWbMaximumClicks(new StrategyMaximumClicksAdd()
                                .withWeeklySpendLimit(400_000_000L)))
                .withNetwork(new MobileAppCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(MobileAppCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT)
                        .withNetworkDefault(new StrategyNetworkDefaultAdd().withLimitPercent(39)));

        MobileAppCampaignAddItem mobileAppCampaignAddItem = new MobileAppCampaignAddItem()
                .withBiddingStrategy(strategy);

        var campaignAddItem = newCampaignAddItem()
                .withMobileAppCampaign(mobileAppCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(5005);
    }

    @Test
    public void addCampaign_campaignAddItemWithLimitPercentNotInRange_ValidationError() {
        var strategy = new MobileAppCampaignStrategyAdd()
                .withSearch(new MobileAppCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(MobileAppCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS)
                        .withWbMaximumClicks(new StrategyMaximumClicksAdd()
                                .withWeeklySpendLimit(400_000_000L)))
                .withNetwork(new MobileAppCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(MobileAppCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT)
                        .withNetworkDefault(new StrategyNetworkDefaultAdd().withLimitPercent(9)));

        MobileAppCampaignAddItem mobileAppCampaignAddItem = new MobileAppCampaignAddItem()
                .withBiddingStrategy(strategy);

        var campaignAddItem = newCampaignAddItem()
                .withMobileAppCampaign(mobileAppCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(5005);
    }

    @Test
    public void addCampaign_allFields() {
        var strategy = defaultStrategy();

        TimeTargetingAdd timeTargeting = new TimeTargetingAdd()
                .withSchedule(new ArrayOfString()
                        .withItems(TIME_TARGETING_SCHEDULE))
                .withHolidaysSchedule(new TimeTargetingOnPublicHolidays()
                        .withBidPercent(50)
                        .withStartHour(9)
                        .withEndHour(23)
                        .withSuspendOnHolidays(YesNoEnum.NO))
                .withConsiderWorkingWeekends(YesNoEnum.NO);

        TimeTarget expectedTimeTarget = TimeTarget.parseRawString("1ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX2ABCDEFGHIJ" +
                "bKcLdMeNOfPgQhRqSrTsUtVuWpX3ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtVuWpX4ABCDEFGHIJbKcLdMeNOfPgQhRqSrTsUtV" +
                "uWpX5ABCDEFGHIJKLMNOPQRSTUVWX6AbBcCdDfEFGHIJKLbMcNdOfPcQdReSjTiUhVgWfX7AbBcCdDfEFGHIJKLbMcNdOfPcQdR" +
                "eSjTiUhVgWfX8JfKfLfMfNfOfPfQfRfSfTfUfVfWf;p:o");

        MobileAppCampaignAddItem mobileAppCampaignAddItem = new MobileAppCampaignAddItem()
                .withBiddingStrategy(strategy);

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
        String smsTimeFrom = "0:15";
        String smsTimeTo = "0:30";
        TimeInterval expectedSmsInterval = new TimeInterval()
                .withStartHour(0)
                .withStartMinute(15)
                .withEndHour(0)
                .withEndMinute(30);
        var smsEvents = List.of(SmsEventsEnum.MONEY_IN);
        SmsFlag[] expectedSmsEvents = {SmsFlag.NOTIFY_ORDER_MONEY_IN_SMS};
        long dailyBudgetAmount = 300881111;
        BigDecimal expectedDailyBudget = BigDecimal.valueOf(300.88);
        DailyBudgetModeEnum dailyBudgetMode = DailyBudgetModeEnum.DISTRIBUTED;
        DayBudgetShowMode expectedDailyBudgetMode = DayBudgetShowMode.STRETCHED;
        List<String> negativeKeywords = List.of("negative");
        List<String> excludedSites = List.of(SSP_PLATFORM_1.toUpperCase(), SSP_PLATFORM_2, "google.ru 1", "google.com");
        List<String> expectedDisabledSsp = List.of(SSP_PLATFORM_1, SSP_PLATFORM_2);
        List<String> expectedDisabledDomains = List.of(SSP_PLATFORM_2, "google.ru", "google.com");

        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(startDate))
                .withEndDate(DATETIME_FORMATTER.format(endDate))
                .withMobileAppCampaign(mobileAppCampaignAddItem)
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

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(),
                List.of(campaignId));
        MobileContentCampaign actual = (MobileContentCampaign) campaigns.get(campaignId);

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
            softly.assertThat(actual.getSource()).as("source")
                    .isEqualTo(CampaignSource.API);
            softly.assertThat(actual.getContextLimit()).as("contextLimit")
                    .isEqualTo(0);
        });
    }

    @Test
    public void addCampaign_onlyRequiredFields() {
        var strategy = defaultStrategy();

        MobileAppCampaignAddItem mobileAppCampaignAddItem = new MobileAppCampaignAddItem()
                .withBiddingStrategy(strategy);

        var startDate = LocalDate.now();
        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(startDate))
                .withMobileAppCampaign(mobileAppCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(),
                List.of(campaignId));
        MobileContentCampaign actual = (MobileContentCampaign) campaigns.get(campaignId);

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

    private static AddRequest getAddRequest(String name, MobileAppCampaignAddItem mobileAppCampaignAddItem) {
        var campaignAddItem = new CampaignAddItem()
                .withName(name)
                .withStartDate(DATETIME_FORMATTER.format(LocalDateTime.now()))
                .withMobileAppCampaign(mobileAppCampaignAddItem);

        return new AddRequest().withCampaigns(campaignAddItem);
    }

    private static MobileAppCampaignAddItem defaultAddItem() {
        var strategy = defaultStrategy();

        return new MobileAppCampaignAddItem()
                .withBiddingStrategy(strategy);
    }

    private static MobileAppCampaignStrategyAdd defaultStrategy() {
        return new MobileAppCampaignStrategyAdd()
                .withSearch(new MobileAppCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(MobileAppCampaignSearchStrategyTypeEnum.HIGHEST_POSITION))
                .withNetwork(new MobileAppCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(MobileAppCampaignNetworkStrategyTypeEnum.MAXIMUM_COVERAGE));
    }

    private static MobileAppCampaignSetting MobileAppCampaignSetting(MobileAppCampaignSettingsEnum option,
                                                                     YesNoEnum value) {
        return new MobileAppCampaignSetting().withOption(option).withValue(value);
    }

}
