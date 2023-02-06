package ru.yandex.direct.api.v5.entity.campaignsext.delegate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.yandex.direct.api.v5.campaignsext.AddRequest;
import com.yandex.direct.api.v5.campaignsext.AddResponse;
import com.yandex.direct.api.v5.campaignsext.CampaignAddItem;
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignAddItem;
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSetting;
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignSettingsEnum;
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignStrategyAdd;
import com.yandex.direct.api.v5.campaignsext.DailyBudget;
import com.yandex.direct.api.v5.campaignsext.DailyBudgetModeEnum;
import com.yandex.direct.api.v5.campaignsext.EmailSettings;
import com.yandex.direct.api.v5.campaignsext.Notification;
import com.yandex.direct.api.v5.campaignsext.SmsEventsEnum;
import com.yandex.direct.api.v5.campaignsext.SmsSettings;
import com.yandex.direct.api.v5.campaignsext.TimeTargetingAdd;
import com.yandex.direct.api.v5.campaignsext.TimeTargetingOnPublicHolidays;
import com.yandex.direct.api.v5.general.ArrayOfInteger;
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
import ru.yandex.direct.api.v5.entity.campaignsext.converter.CampaignsExtAddRequestConverter;
import ru.yandex.direct.api.v5.entity.campaignsext.converter.OtherCampaignsAddItemConverter;
import ru.yandex.direct.api.v5.entity.campaignsext.validation.OtherCampaignsAddRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
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
import static ru.yandex.direct.api.v5.entity.campaigns.delegate.AddCampaignDelegateConstants.DEFAULT_ENABLE_SEND_ACCOUNT_NEWS;
import static ru.yandex.direct.api.v5.entity.campaigns.delegate.AddCampaignDelegateConstants.DEFAULT_SMS_INTERVAL;
import static ru.yandex.direct.api.v5.entity.campaigns.delegate.AddCampaignDelegateConstants.DEFAULT_WARNING_BALANCE;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.defaultTimeTarget;

@Api5Test
@RunWith(SpringRunner.class)
public class AddContentPromotionCampaignDelegateTest {
    private static final String NAME = "ContentPromotion кампания";

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

    @Mock
    private GeoTimezoneRepository geoTimezoneRepository;

    @Mock
    private ApiAuthenticationSource authenticationSource;

    @Autowired
    private ResultConverter resultConverter;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private UserService userService;

    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private SspPlatformsRepository sspPlatformsRepository;

    @Autowired
    private OtherCampaignsAddItemConverter otherCampaignsAddItemConverter;

    @Autowired
    private OtherCampaignsAddRequestValidator otherCampaignsAddRequestValidator;

    private GenericApiService genericApiService;
    private AddCampaignsExtDelegate delegate;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

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

        var requestConverter = new CampaignsExtAddRequestConverter(userService, geoTimezoneRepository,
                authenticationSource, sspPlatformsRepository, otherCampaignsAddItemConverter,
                otherCampaignsAddRequestValidator);

        delegate = new AddCampaignsExtDelegate(
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
    public void addContentPromotionCampaign_withDefaultSettings() {
        ContentPromotionCampaignAddItem contentPromotionCampaignAddItem = defaultContentPromotionCampaignAddItem();
        AddRequest request = getAddRequest(contentPromotionCampaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        ContentPromotionCampaign actual = (ContentPromotionCampaign) campaigns.get(campaignId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getHasExtendedGeoTargeting()).as("hasExtendedGeoTargeting").isTrue();
            softly.assertThat(actual.getFavoriteForUids()).as("favoriteForUids").isNull();
        });
    }

    @Test
    public void addContentPromotionCampaign_withCustomSettings() {
        ContentPromotionCampaignAddItem contentPromotionCampaignAddItem = defaultContentPromotionCampaignAddItem()
                .withSettings(List.of(
                        contentPromotionCampaignSetting(ContentPromotionCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING, YesNoEnum.NO),
                        contentPromotionCampaignSetting(ContentPromotionCampaignSettingsEnum.ADD_TO_FAVORITES,
                                YesNoEnum.YES)
                ));
        AddRequest request = getAddRequest(contentPromotionCampaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        ContentPromotionCampaign actual = (ContentPromotionCampaign) campaigns.get(campaignId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getHasExtendedGeoTargeting()).as("hasExtendedGeoTargeting").isFalse();
            softly.assertThat(actual.getFavoriteForUids()).as("favoriteForUids")
                    .isEqualTo(Set.of(clientInfo.getUid()));
        });
    }

    @Test
    public void addContentPromotionCampaign_allFields() {
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

        AttributionModelEnum attributionModel = AttributionModelEnum.FC;
        var expectedAttributionModel = CampaignAttributionModel.FIRST_CLICK;
        ContentPromotionCampaignAddItem contentPromotionCampaignAddItem = new ContentPromotionCampaignAddItem()
                .withBiddingStrategy(defaultStrategy())
                .withAttributionModel(attributionModel)
                .withCounterIds(new ArrayOfInteger().withItems(COUNTER_ID));

        String fio = "Ivanov Ivan";
        var startDate = LocalDate.now().plusDays(10);
        var endDate = LocalDate.now().plusDays(20);
        int warningBalance = 20;
        YesNoEnum sendWarn = YesNoEnum.YES;
        YesNoEnum sendAccountNews = YesNoEnum.YES;
        Boolean expectedSendNews = Boolean.TRUE;
        int checkPositionIntervalEvent = 30;
        String email = "test@email.com";
        String smsTimeFrom = "18:00";
        String smsTimeTo = "20:00";
        TimeInterval expectedSmsInterval = new TimeInterval()
                .withStartHour(18)
                .withStartMinute(0)
                .withEndHour(20)
                .withEndMinute(0);
        var smsEvents = List.of(SmsEventsEnum.MONITORING, SmsEventsEnum.FINISHED);
        SmsFlag[] expectedSmsEvents = {SmsFlag.NOTIFY_METRICA_CONTROL_SMS, SmsFlag.CAMP_FINISHED_SMS};
        long dailyBudgetAmount = 400000000;
        BigDecimal expectedDailyBudget = new BigDecimal("400.00");
        DailyBudgetModeEnum dailyBudgetMode = DailyBudgetModeEnum.STANDARD;
        DayBudgetShowMode expectedDailyBudgetMode = DayBudgetShowMode.DEFAULT_;
        List<String> negativeKeywords = List.of("negative", "keywords");

        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(startDate))
                .withEndDate(DATETIME_FORMATTER.format(endDate))
                .withContentPromotionCampaign(contentPromotionCampaignAddItem)
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
                        .withItems(negativeKeywords));

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        ContentPromotionCampaign actual = (ContentPromotionCampaign) campaigns.get(campaignId);

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
            softly.assertThat(actual.getEnableSendAccountNews()).as("sendAccountNews")
                    .isEqualTo(expectedSendNews);
            softly.assertThat(actual.getEnableCheckPositionEvent()).as("sendWarnings")
                    .isTrue();
            softly.assertThat(actual.getCheckPositionIntervalEvent()).as("checkPositionInterval")
                    .isEqualTo(CampaignWarnPlaceInterval._30);
            softly.assertThat(actual.getSmsTime()).as("smsTime").isEqualTo(expectedSmsInterval);
            softly.assertThat(actual.getSmsFlags()).as("smsFlags")
                    .containsExactlyInAnyOrder(expectedSmsEvents);
            softly.assertThat(actual.getDayBudget()).as("dayBudget").isEqualTo(expectedDailyBudget);
            softly.assertThat(actual.getDayBudgetShowMode()).as("dayBudgetShowMode").
                    isEqualTo(expectedDailyBudgetMode);
            softly.assertThat(actual.getMinusKeywords()).as("minusKeywords")
                    .containsExactlyInAnyOrder(negativeKeywords.toArray(new String[0]));

            // не можем протестировать blockedIps, т.к. в network-config.allow-all.json все ip объявляются внутренними,
            // а ядро не разрешает блокировать внутренние ip
            softly.assertThat(actual.getDisabledIps()).as("disabledIps").isNull();

            softly.assertThat(actual.getAttributionModel()).as("attributionModel")
                    .isEqualTo(expectedAttributionModel);
            softly.assertThat(actual.getMetrikaCounters()).as("metrikaCounters")
                    .containsExactlyInAnyOrder(Long.valueOf(COUNTER_ID));
            softly.assertThat(actual.getSource()).as("source")
                    .isEqualTo(CampaignSource.API);
        });
    }

    @Test
    public void addContentPromotionCampaign_onlyRequiredFields() {
        var strategy = defaultStrategy();

        ContentPromotionCampaignAddItem contentPromotionCampaignAddItem = new ContentPromotionCampaignAddItem()
                .withBiddingStrategy(strategy);

        var startDate = LocalDate.now();
        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(startDate))
                .withContentPromotionCampaign(contentPromotionCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        ContentPromotionCampaign actual = (ContentPromotionCampaign) campaigns.get(campaignId);

        User user = Objects.requireNonNull(userService.getUser(clientInfo.getUid()));
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
            softly.assertThat(actual.getEnableSendAccountNews()).as("sendAccountNews")
                    .isEqualTo(DEFAULT_ENABLE_SEND_ACCOUNT_NEWS);
            softly.assertThat(actual.getEnableCheckPositionEvent()).as("sendWarnings").isFalse();
            softly.assertThat(actual.getCheckPositionIntervalEvent()).as("checkPositionInterval")
                    .isEqualTo(CampaignWarnPlaceInterval._60);
            softly.assertThat(actual.getSmsTime()).as("smsTime").isEqualTo(DEFAULT_SMS_INTERVAL);
            softly.assertThat(actual.getSmsFlags()).as("smsFlags").isEmpty();
            softly.assertThat(actual.getDayBudget()).as("dayBudget").isEqualTo(new BigDecimal("0.00"));
            softly.assertThat(actual.getDayBudgetShowMode()).as("dayBudgetShowMode").
                    isEqualTo(DayBudgetShowMode.DEFAULT_);
            softly.assertThat(actual.getMinusKeywords()).as("minusKeywords").isEmpty();
            softly.assertThat(actual.getAttributionModel()).as("attributionModel")
                    .isEqualTo(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK);
            softly.assertThat(actual.getMetrikaCounters()).as("metrikaCounters").isNull();
            softly.assertThat(actual.getSource()).as("source")
                    .isEqualTo(CampaignSource.API);
        });
    }

    @Test
    public void addContentPromotionCampaign_withExcludedSites_ValidationError() {
        var strategy = new ContentPromotionCampaignStrategyAdd()
                .withSearch(new ContentPromotionCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(ContentPromotionCampaignSearchStrategyTypeEnum.HIGHEST_POSITION))
                .withNetwork(new ContentPromotionCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(ContentPromotionCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        var contentPromotionCampaignAddItem = new ContentPromotionCampaignAddItem()
                .withCounterIds(new ArrayOfInteger().withItems(List.of(COUNTER_ID)))
                .withBiddingStrategy(strategy);

        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(LocalDate.now().toString())
                .withContentPromotionCampaign(contentPromotionCampaignAddItem)
                .withExcludedSites(new ArrayOfString()
                        .withItems("google.com"));

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).hasSize(1);
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(4000);
    }

    private static AddRequest getAddRequest(ContentPromotionCampaignAddItem contentPromotionCampaignAddItem) {
        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(LocalDateTime.now()))
                .withContentPromotionCampaign(contentPromotionCampaignAddItem);

        return new AddRequest().withCampaigns(campaignAddItem);
    }

    private static ContentPromotionCampaignAddItem defaultContentPromotionCampaignAddItem() {
        var strategy = defaultStrategy();

        return new ContentPromotionCampaignAddItem()
                .withBiddingStrategy(strategy)
                .withCounterIds(new ArrayOfInteger().withItems(COUNTER_ID));
    }

    private static ContentPromotionCampaignStrategyAdd defaultStrategy() {
        return new ContentPromotionCampaignStrategyAdd()
                .withSearch(new ContentPromotionCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(ContentPromotionCampaignSearchStrategyTypeEnum.HIGHEST_POSITION))
                .withNetwork(new ContentPromotionCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(ContentPromotionCampaignNetworkStrategyTypeEnum.SERVING_OFF));
    }

    private static ContentPromotionCampaignSetting contentPromotionCampaignSetting(ContentPromotionCampaignSettingsEnum option, YesNoEnum value) {
        return new ContentPromotionCampaignSetting().withOption(option).withValue(value);
    }

}
