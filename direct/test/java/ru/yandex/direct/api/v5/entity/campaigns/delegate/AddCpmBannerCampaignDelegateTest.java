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
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSetting;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignSettingsEnum;
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignStrategyAdd;
import com.yandex.direct.api.v5.campaigns.EmailSettings;
import com.yandex.direct.api.v5.campaigns.FrequencyCapSetting;
import com.yandex.direct.api.v5.campaigns.Notification;
import com.yandex.direct.api.v5.campaigns.SmsEventsEnum;
import com.yandex.direct.api.v5.campaigns.SmsSettings;
import com.yandex.direct.api.v5.campaigns.StrategyWbDecreasedPriceForRepeatedImpressionsAdd;
import com.yandex.direct.api.v5.campaigns.TimeTargetingAdd;
import com.yandex.direct.api.v5.campaigns.TimeTargetingOnPublicHolidays;
import com.yandex.direct.api.v5.general.ArrayOfString;
import com.yandex.direct.api.v5.general.VideoTargetEnum;
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
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType;
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
import ru.yandex.direct.feature.FeatureName;
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
public class AddCpmBannerCampaignDelegateTest {

    private static final String NAME = "CpmBanner кампания";

    private static final GeoTimezone AMSTERDAM_TIMEZONE = new GeoTimezone()
            .withTimezone(ZoneId.of("Europe/Amsterdam"))
            .withTimezoneId(174L);

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.DISABLE_BILLING_AGGREGATES, true);
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
    public void addCpmBannerCampaign_withDefaultSettings() {
        AddRequest request = getAddCpmBannerCampaignRequest(NAME);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        CpmBannerCampaign actual = (CpmBannerCampaign) campaigns.get(campaignId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getHasAddMetrikaTagToUrl()).as("hasAddMetrikaTagToUrl").isTrue();
            softly.assertThat(actual.getHasAddOpenstatTagToUrl()).as("hasAddOpenstatTagToUrl").isFalse();
            softly.assertThat(actual.getHasExtendedGeoTargeting()).as("hasExtendedGeoTargeting").isTrue();
            softly.assertThat(actual.getHasSiteMonitoring()).as("hasSiteMonitoring").isFalse();
            softly.assertThat(actual.getFavoriteForUids()).as("favoriteForUids").isNull();
        });
    }

    @Test
    public void addCpmBannerCampaign_withCustomSettings() {
        CpmBannerCampaignAddItem campaignAddItem = defaultCpmBannerCampaignAddItem()
                .withSettings(List.of(
                        cpmBannerCampaignSetting(CpmBannerCampaignSettingsEnum.ADD_METRICA_TAG, YesNoEnum.NO),
                        cpmBannerCampaignSetting(CpmBannerCampaignSettingsEnum.ADD_OPENSTAT_TAG, YesNoEnum.YES),
                        cpmBannerCampaignSetting(CpmBannerCampaignSettingsEnum.ENABLE_AREA_OF_INTEREST_TARGETING,
                                YesNoEnum.NO),
                        cpmBannerCampaignSetting(CpmBannerCampaignSettingsEnum.ENABLE_SITE_MONITORING, YesNoEnum.YES),
                        cpmBannerCampaignSetting(CpmBannerCampaignSettingsEnum.ADD_TO_FAVORITES, YesNoEnum.YES)
                ));
        AddRequest request = getAddRequest(NAME, campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        CpmBannerCampaign actual = (CpmBannerCampaign) campaigns.get(campaignId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getHasAddMetrikaTagToUrl()).as("hasAddMetrikaTagToUrl").isFalse();
            softly.assertThat(actual.getHasAddOpenstatTagToUrl()).as("hasAddOpenstatTagToUrl").isTrue();
            softly.assertThat(actual.getHasExtendedGeoTargeting()).as("hasExtendedGeoTargeting").isFalse();
            softly.assertThat(actual.getHasSiteMonitoring()).as("hasSiteMonitoring").isTrue();
            softly.assertThat(actual.getFavoriteForUids()).as("favoriteForUids")
                    .isEqualTo(Set.of(clientInfo.getUid()));
        });
    }

    @Test
    public void addCpmBannerCampaign_allFields() {
        var strategy = defaultStrategy();

        TimeTargetingAdd timeTargeting = new TimeTargetingAdd()
                .withHolidaysSchedule(new TimeTargetingOnPublicHolidays()
                        .withBidPercent(100)
                        .withStartHour(9)
                        .withEndHour(23)
                        .withSuspendOnHolidays(YesNoEnum.NO))
                .withConsiderWorkingWeekends(YesNoEnum.NO);

        TimeTarget expectedTimeTarget = TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUV" +
                "WX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRST" +
                "UVWX7ABCDEFGHIJKLMNOPQRSTUVWX8JKLMNOPQRSTUVW;p:o");

        int impressions = 20;
        int periodDays = 10;
        CpmBannerCampaignAddItem cpmBannerCampaignAddItem = new CpmBannerCampaignAddItem()
                .withBiddingStrategy(strategy)
                .withFrequencyCap(new FrequencyCapSetting()
                        .withImpressions(impressions)
                        .withPeriodDays(periodDays))
                .withVideoTarget(VideoTargetEnum.CLICKS);
        String fio = "Ivanov Ivan";
        var startDate = LocalDate.now().plusDays(10);
        var endDate = LocalDate.now().plusDays(20);
        int warningBalance = 20;
        YesNoEnum sendWarn = YesNoEnum.YES;
        Boolean expectedSendWarn = Boolean.TRUE;
        YesNoEnum sendAccountNews = YesNoEnum.YES;
        Boolean expectedSendNews = Boolean.TRUE;
        int checkPositionIntervalEvent = 15;
        CampaignWarnPlaceInterval expectedCheckPositionIntervalEvent = CampaignWarnPlaceInterval._15;
        String email = "test@email.com";
        String smsTimeFrom = "12:00";
        String smsTimeTo = "15:15";
        TimeInterval expectedSmsInterval = new TimeInterval()
                .withStartHour(12)
                .withStartMinute(0)
                .withEndHour(15)
                .withEndMinute(15);
        var smsEvents = List.of(SmsEventsEnum.MONEY_IN);
        SmsFlag[] expectedSmsEvents = {SmsFlag.NOTIFY_ORDER_MONEY_IN_SMS};

        List<String> excludedSites = List.of(SSP_PLATFORM_1.toUpperCase(), SSP_PLATFORM_2, "google.ru 1", "google.com");
        List<String> expectedDisabledSsp = List.of(SSP_PLATFORM_1, SSP_PLATFORM_2);
        List<String> expectedDisabledDomains = List.of(SSP_PLATFORM_2, "google.ru", "google.com");

        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(startDate))
                .withEndDate(DATETIME_FORMATTER.format(endDate))
                .withCpmBannerCampaign(cpmBannerCampaignAddItem)
                .withTimeZone(AMSTERDAM_TIMEZONE.getTimezone().getId())
                .withTimeTargeting(timeTargeting)
                .withClientInfo(fio)
                .withNotification(new Notification()
                        .withEmailSettings(new EmailSettings()
                                .withEmail(email)
                                .withCheckPositionInterval(checkPositionIntervalEvent)
                                .withSendAccountNews(sendAccountNews)
                                .withSendWarnings(sendWarn)
                                .withWarningBalance(warningBalance)
                        )
                        .withSmsSettings(new SmsSettings()
                                .withEvents(smsEvents)
                                .withTimeFrom(smsTimeFrom)
                                .withTimeTo(smsTimeTo)))
                .withExcludedSites(new ArrayOfString()
                        .withItems(excludedSites));

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        CpmBannerCampaign actual = (CpmBannerCampaign) campaigns.get(campaignId);

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
            softly.assertThat(actual.getDisabledDomains()).as("disabledDomains")
                    .containsExactlyInAnyOrder(expectedDisabledDomains.toArray(new String[0]));

            // не можем протестировать blockedIps, т.к. в network-config.allow-all.json все ip объявляются внутренними,
            // а ядро не разрешает блокировать внутренние ip
            softly.assertThat(actual.getDisabledIps()).as("disabledIps").isNull();

            softly.assertThat(actual.getDisabledSsp()).as("disabledSsp")
                    .containsExactlyInAnyOrder(expectedDisabledSsp.toArray(new String[0]));
            softly.assertThat(actual.getImpressionRateCount()).as("impressionRateCount")
                    .isEqualTo(impressions);
            softly.assertThat(actual.getImpressionRateIntervalDays()).as("impressionRateIntervalDays")
                    .isEqualTo(periodDays);
            softly.assertThat(actual.getSource()).as("source")
                    .isEqualTo(CampaignSource.API);
            softly.assertThat(actual.getEshowsSettings().getVideoType()).as("videoType")
                    .isEqualTo(EshowsVideoType.LONG_CLICKS);
        });
    }

    @Test
    public void addCpmBannerCampaign_onlyRequiredFields() {
        CpmBannerCampaignAddItem cpmBannerCampaignAddItem = new CpmBannerCampaignAddItem()
                .withBiddingStrategy(defaultStrategy());

        var startDate = LocalDate.now();
        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(DATETIME_FORMATTER.format(LocalDateTime.now()))
                .withCpmBannerCampaign(cpmBannerCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        Long campaignId = response.getAddResults().get(0).getId();

        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        CpmBannerCampaign actual = (CpmBannerCampaign) campaigns.get(campaignId);

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
            softly.assertThat(actual.getDisabledIps()).as("disabledIps").isNull();
            softly.assertThat(actual.getDisabledSsp()).as("disabledSsp").isEmpty();
            softly.assertThat(actual.getDisabledDomains()).as("disabledDomains").isNull();
            softly.assertThat(actual.getImpressionRateCount()).as("impressionRateCount").isNull();
            softly.assertThat(actual.getImpressionRateIntervalDays()).as("impressionRateIntervalDays").isNull();
            softly.assertThat(actual.getSource()).as("source")
                    .isEqualTo(CampaignSource.API);
            softly.assertThat(actual.getEshowsSettings().getVideoType()).as("videoType")
                    .isEqualTo(EshowsVideoType.COMPLETES);
        });
    }

    private static AddRequest getAddCpmBannerCampaignRequest(String name) {
        CpmBannerCampaignAddItem cpmBannerCampaignAddItem = defaultCpmBannerCampaignAddItem();
        return getAddRequest(name, cpmBannerCampaignAddItem);
    }

    private static AddRequest getAddRequest(String name, CpmBannerCampaignAddItem cpmBannerCampaignAddItem) {
        var campaignAddItem = new CampaignAddItem()
                .withName(name)
                .withStartDate(DATETIME_FORMATTER.format(LocalDateTime.now()))
                .withCpmBannerCampaign(cpmBannerCampaignAddItem);

        return new AddRequest().withCampaigns(campaignAddItem);
    }

    private static CpmBannerCampaignAddItem defaultCpmBannerCampaignAddItem() {
        var strategy = defaultStrategy();

        return new CpmBannerCampaignAddItem()
                .withBiddingStrategy(strategy);
    }

    private static CpmBannerCampaignStrategyAdd defaultStrategy() {
        return new CpmBannerCampaignStrategyAdd()
                .withSearch(new CpmBannerCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(CpmBannerCampaignSearchStrategyTypeEnum.SERVING_OFF))
                .withNetwork(new CpmBannerCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(CpmBannerCampaignNetworkStrategyTypeEnum
                                .WB_DECREASED_PRICE_FOR_REPEATED_IMPRESSIONS)
                        .withWbDecreasedPriceForRepeatedImpressions(
                                new StrategyWbDecreasedPriceForRepeatedImpressionsAdd()
                                        .withAverageCpm(5000000L)
                                        .withSpendLimit(3000000000L)));
    }

    private static CpmBannerCampaignSetting cpmBannerCampaignSetting(
            CpmBannerCampaignSettingsEnum option, YesNoEnum value) {
        return new CpmBannerCampaignSetting().withOption(option).withValue(value);
    }

}
