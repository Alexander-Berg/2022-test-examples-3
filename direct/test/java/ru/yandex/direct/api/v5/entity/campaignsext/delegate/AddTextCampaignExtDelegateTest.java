package ru.yandex.direct.api.v5.entity.campaignsext.delegate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.google.common.collect.ImmutableList;
import com.yandex.direct.api.v5.campaignsext.AddRequest;
import com.yandex.direct.api.v5.campaignsext.AddResponse;
import com.yandex.direct.api.v5.campaignsext.CampaignAddItem;
import com.yandex.direct.api.v5.campaignsext.DailyBudget;
import com.yandex.direct.api.v5.campaignsext.DailyBudgetModeEnum;
import com.yandex.direct.api.v5.campaignsext.EmailSettings;
import com.yandex.direct.api.v5.campaignsext.Notification;
import com.yandex.direct.api.v5.campaignsext.PriorityGoalsArray;
import com.yandex.direct.api.v5.campaignsext.PriorityGoalsItem;
import com.yandex.direct.api.v5.campaignsext.RelevantKeywordsSettingAdd;
import com.yandex.direct.api.v5.campaignsext.SmsEventsEnum;
import com.yandex.direct.api.v5.campaignsext.SmsSettings;
import com.yandex.direct.api.v5.campaignsext.StrategyNetworkDefaultAdd;
import com.yandex.direct.api.v5.campaignsext.TextCampaignAddItem;
import com.yandex.direct.api.v5.campaignsext.TextCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaignsext.TextCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaignsext.TextCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaignsext.TextCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaignsext.TextCampaignSetting;
import com.yandex.direct.api.v5.campaignsext.TextCampaignSettingsEnum;
import com.yandex.direct.api.v5.campaignsext.TextCampaignStrategyAdd;
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
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.RequestSource;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.entity.strategy.service.StrategyOperationFactory;
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
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultGoal;
import static ru.yandex.direct.feature.FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA;
import static ru.yandex.direct.feature.FeatureName.CRR_STRATEGY_ALLOWED;
import static ru.yandex.direct.feature.FeatureName.FIX_CRR_STRATEGY_ALLOWED;
import static ru.yandex.direct.feature.FeatureName.UNIVERSAL_CAMPAIGNS_BETA_DISABLED;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.defaultTimeTarget;

@Api5Test
@RunWith(SpringRunner.class)
public class AddTextCampaignExtDelegateTest {

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

    private static final Integer COUNTER_ID = 1999;

    private static final Long VALID_GOAL_ID = 199L;

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
    public StrategyOperationFactory strategyOperationFactory;

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
        metrikaClientStub.addGoals(clientInfo.getUid(), Set.of(
                ((Goal) ((Goal) defaultGoal(VALID_GOAL_ID))
                        .withCounterId(COUNTER_ID))
        ));

        steps.sspPlatformsSteps().addSspPlatforms(List.of(SSP_PLATFORM_1, SSP_PLATFORM_2));
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), CRR_STRATEGY_ALLOWED, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FIX_CRR_STRATEGY_ALLOWED, true);

        // включаем UNIVERSAL_CAMPAIGNS_BETA_DISABLED, чтобы все цели метрики не были автоматически доступны
        // чтобы качественнее проверить валидацию целей
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), UNIVERSAL_CAMPAIGNS_BETA_DISABLED, true);
    }

    @Test
    public void addCampaign_campaignAddItemWithLimitPercentNotInRange_ValidationError() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT)
                        .withNetworkDefault(new StrategyNetworkDefaultAdd().withLimitPercent(9)));

        TextCampaignAddItem textCampaignAddItem = new TextCampaignAddItem()
                .withBiddingStrategy(strategy);

        var campaignAddItem = newCampaignAddItem()
                .withTextCampaign(textCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).isNotEmpty();
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(5005);
    }

    @Test
    public void addTextCampaign_allFields() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA, true);

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

        int budgetPercent = 99;
        JAXBElement<Long> optimizeGoalId = new JAXBElement(new QName(""), Long.class, null);
        AttributionModelEnum attributionModel = AttributionModelEnum.FC;
        var expectedAttributionModel = CampaignAttributionModel.FIRST_CLICK;
        var trackingParams = "umt_param=val";
        TextCampaignAddItem textCampaignAddItem = new TextCampaignAddItem()
                .withBiddingStrategy(defaultStrategy())
                .withRelevantKeywords(new RelevantKeywordsSettingAdd()
                        .withBudgetPercent(budgetPercent)
                        // значение для OptimizeGoalId по умолчанию 0, а null - отличное от умолчания, выставляем null
                        .withOptimizeGoalId(optimizeGoalId))
                .withAttributionModel(attributionModel)
                .withPriorityGoals(new PriorityGoalsArray()
                        .withItems(List.of(new PriorityGoalsItem()
                                .withValue(13000000)
                                .withGoalId(VALID_GOAL_ID))))
                .withCounterIds(new ArrayOfInteger().withItems(COUNTER_ID))
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
        List<String> excludedSites = List.of(SSP_PLATFORM_1.toUpperCase(), SSP_PLATFORM_2, "google.ru 1", "google.com");
        List<String> expectedDisabledSsp = List.of(SSP_PLATFORM_1, SSP_PLATFORM_2);
        List<String> expectedDisabledDomains = List.of(SSP_PLATFORM_2, "google.ru", "google.com");

        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(startDate))
                .withEndDate(DATETIME_FORMATTER.format(endDate))
                .withTextCampaign(textCampaignAddItem)
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
        TextCampaign actual = (TextCampaign) campaigns.get(campaignId);

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
            softly.assertThat(actual.getBroadMatch().getBroadMatchFlag()).isTrue();
            softly.assertThat(actual.getBroadMatch().getBroadMatchLimit()).as("broadMatchLimit")
                    .isEqualTo(budgetPercent);
            softly.assertThat(actual.getBroadMatch().getBroadMatchGoalId()).isNull();
            softly.assertThat(actual.getImpressionRateCount()).as("impressionRateCount").isNull();
            softly.assertThat(actual.getImpressionRateIntervalDays()).as("impressionRateIntervalDays").isNull();
            softly.assertThat(actual.getAttributionModel()).as("attributionModel")
                    .isEqualTo(expectedAttributionModel);
            softly.assertThat(actual.getMeaningfulGoals()).as("meaningfulGoals")
                    .isEqualTo(List.of(
                            new MeaningfulGoal()
                                    .withGoalId(VALID_GOAL_ID)
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
    public void addTextCampaign_onlyRequiredFields() {
        var strategy = defaultStrategy();

        TextCampaignAddItem textCampaignAddItem = new TextCampaignAddItem()
                .withBiddingStrategy(strategy);

        var startDate = LocalDate.now();
        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(startDate))
                .withTextCampaign(textCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        TextCampaign actual = (TextCampaign) campaigns.get(campaignId);

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
            softly.assertThat(actual.getDisabledDomains()).as("disabledDomains").isNull();
            softly.assertThat(actual.getDisabledIps()).as("disabledIps").isNull();
            softly.assertThat(actual.getDisabledSsp()).as("disabledSsp").isEmpty();
            softly.assertThat(actual.getBroadMatch().getBroadMatchFlag()).isFalse();
            softly.assertThat(actual.getBroadMatch().getBroadMatchLimit()).as("broadMatchLimit")
                    .isEqualTo(0);
            softly.assertThat(actual.getBroadMatch().getBroadMatchGoalId()).isEqualTo(0L);
            softly.assertThat(actual.getImpressionRateCount()).as("impressionRateCount").isNull();
            softly.assertThat(actual.getImpressionRateIntervalDays()).as("impressionRateIntervalDays")
                    .isNull();
            softly.assertThat(actual.getAttributionModel()).as("attributionModel")
                    .isEqualTo(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK);
            softly.assertThat(actual.getMeaningfulGoals()).as("meaningfulGoals").isNull();
            softly.assertThat(actual.getMetrikaCounters()).as("metrikaCounters").isNull();
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


    private static AddRequest getAddRequest(String name, TextCampaignAddItem textCampaignAddItem) {
        var campaignAddItem = new CampaignAddItem()
                .withName(name)
                .withStartDate(DATETIME_FORMATTER.format(LocalDateTime.now()))
                .withTextCampaign(textCampaignAddItem);

        return new AddRequest().withCampaigns(campaignAddItem);
    }

    private static TextCampaignAddItem defaultTextCampaignAddItem() {
        var strategy = defaultStrategy();

        return new TextCampaignAddItem()
                .withBiddingStrategy(strategy)
                .withCounterIds(new ArrayOfInteger().withItems(COUNTER_ID));
    }

    private static TextCampaignStrategyAdd defaultStrategy() {
        return new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.MAXIMUM_COVERAGE));
    }

    private static TextCampaignSetting textCampaignSetting(TextCampaignSettingsEnum option, YesNoEnum value) {
        return new TextCampaignSetting().withOption(option).withValue(value);
    }

}