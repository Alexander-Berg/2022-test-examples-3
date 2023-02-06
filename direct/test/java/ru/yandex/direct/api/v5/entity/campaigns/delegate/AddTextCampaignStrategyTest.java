package ru.yandex.direct.api.v5.entity.campaigns.delegate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.yandex.direct.api.v5.campaigns.AddRequest;
import com.yandex.direct.api.v5.campaigns.AddResponse;
import com.yandex.direct.api.v5.campaigns.CampaignAddItem;
import com.yandex.direct.api.v5.campaigns.StrategyAverageCpaAdd;
import com.yandex.direct.api.v5.campaigns.StrategyAverageCpcAdd;
import com.yandex.direct.api.v5.campaigns.StrategyAverageRoiAdd;
import com.yandex.direct.api.v5.campaigns.StrategyMaximumClicksAdd;
import com.yandex.direct.api.v5.campaigns.StrategyMaximumConversionRateAdd;
import com.yandex.direct.api.v5.campaigns.StrategyNetworkDefaultAdd;
import com.yandex.direct.api.v5.campaigns.StrategyPayForConversionAdd;
import com.yandex.direct.api.v5.campaigns.StrategyWeeklyClickPackageAdd;
import com.yandex.direct.api.v5.campaigns.TextCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.TextCampaignStrategyAdd;
import com.yandex.direct.api.v5.general.ArrayOfInteger;
import com.yandex.direct.api.v5.general.ExceptionNotification;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(SpringRunner.class)
public class AddTextCampaignStrategyTest {

    private static final String NAME = "Тестовая кампания";
    private static final Integer COUNTER_ID = 100;
    private static final Long GOAL_ID = 55L;

    @Autowired
    private Steps steps;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private CampaignOperationService campaignOperationService;
    @Autowired
    private CampaignsAddRequestConverter requestConverter;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private GenericApiService genericApiService;
    private AddCampaignsDelegate delegate;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();
        steps.featureSteps().setCurrentClient(clientId);
        ApiUser user = new ApiUser()
                .withUid(clientInfo.getUid())
                .withClientId(clientId);

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

        delegate = new AddCampaignsDelegate(
                auth,
                campaignOperationService,
                requestConverter,
                resultConverter,
                ppcPropertiesSupport,
                featureService);

        metrikaClientStub.addUserCounter(clientInfo.getUid(), COUNTER_ID);
        metrikaClientStub.addCounterGoal(COUNTER_ID, GOAL_ID.intValue());
    }

    @Test
    public void addTextCampaign_whenSearchTypeIs_HighestPosition() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.NO);
        expectedStrategy.withPlatform(CampaignsPlatform.SEARCH);
        expectedStrategy.withStrategy(null);
        expectedStrategy.withStrategyName(StrategyName.DEFAULT_);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("default"));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenSearchTypeIs_AverageCpc() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.AVERAGE_CPC)
                        .withAverageCpc(new StrategyAverageCpcAdd()
                                .withAverageCpc(1_234_567L)
                                .withWeeklySpendLimit(1_234_567_890L)))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.YES);
        expectedStrategy.withPlatform(CampaignsPlatform.SEARCH);
        expectedStrategy.withStrategy(null);
        expectedStrategy.withStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("autobudget_avg_click")
                .withAvgBid(new BigDecimal("1.23"))
                .withSum(new BigDecimal("1234.57")));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenSearchTypeIs_WbMaximumClicks() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CLICKS)
                        .withWbMaximumClicks(new StrategyMaximumClicksAdd()
                                .withWeeklySpendLimit(1_234_567_890L)
                                .withBidCeiling(1_234_567L)))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.YES);
        expectedStrategy.withPlatform(CampaignsPlatform.SEARCH);
        expectedStrategy.withStrategy(null);
        expectedStrategy.withStrategyName(StrategyName.AUTOBUDGET);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("autobudget")
                .withSum(new BigDecimal("1234.57"))
                .withBid(new BigDecimal("1.23")));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenSearchTypeIs_WeeklyClickPackage() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.WEEKLY_CLICK_PACKAGE)
                        .withWeeklyClickPackage(new StrategyWeeklyClickPackageAdd()
                                .withClicksPerWeek(123L)
                                .withAverageCpc(1_234_567L)))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        TextCampaignAddItem textCampaignAddItem = new TextCampaignAddItem()
                .withCounterIds(new ArrayOfInteger().withItems(List.of(COUNTER_ID)))
                .withBiddingStrategy(strategy);

        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(LocalDate.now().toString())
                .withTextCampaign(textCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        checkFirstElementHasError(response, 5006);
    }

    @Test
    public void addTextCampaign_whenSearchTypeIs_AverageCpa() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.AVERAGE_CPA)
                        .withAverageCpa(new StrategyAverageCpaAdd()
                                .withAverageCpa(5_000_000L)
                                .withGoalId(GOAL_ID)
                                .withWeeklySpendLimit(300_000_000L)
                                .withBidCeiling(10_000_000L)))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.YES);
        expectedStrategy.withPlatform(CampaignsPlatform.SEARCH);
        expectedStrategy.withStrategy(null);
        expectedStrategy.withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("autobudget_avg_cpa")
                .withAvgCpa(new BigDecimal("5"))
                .withGoalId(GOAL_ID)
                .withSum(new BigDecimal("300"))
                .withBid(new BigDecimal("10"))
                .withPayForConversion(false));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenSearchTypeIs_PayForConversion() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.PAY_FOR_CONVERSION)
                        .withPayForConversion(new StrategyPayForConversionAdd()
                                .withCpa(5_000_000L)
                                .withGoalId(GOAL_ID)
                                .withWeeklySpendLimit(400_000_000L)))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.YES);
        expectedStrategy.withPlatform(CampaignsPlatform.SEARCH);
        expectedStrategy.withStrategy(null);
        expectedStrategy.withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("autobudget_avg_cpa")
                .withAvgCpa(new BigDecimal("5"))
                .withGoalId(GOAL_ID)
                .withSum(new BigDecimal("400"))
                .withPayForConversion(true));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenSearchTypeIs_WbMaximumConversionRate() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.WB_MAXIMUM_CONVERSION_RATE)
                        .withWbMaximumConversionRate(new StrategyMaximumConversionRateAdd()
                                .withWeeklySpendLimit(500_000_000L)
                                .withBidCeiling(3_000_000L)
                                .withGoalId(GOAL_ID)))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.YES);
        expectedStrategy.withPlatform(CampaignsPlatform.SEARCH);
        expectedStrategy.withStrategy(null);
        expectedStrategy.withStrategyName(StrategyName.AUTOBUDGET);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("autobudget")
                .withSum(new BigDecimal("500"))
                .withBid(new BigDecimal("3"))
                .withGoalId(GOAL_ID));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenSearchTypeIs_AverageRoi() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.AVERAGE_ROI)
                        .withAverageRoi(new StrategyAverageRoiAdd()
                                .withReserveReturn(70)
                                .withRoiCoef(12_349_999L)
                                .withGoalId(GOAL_ID)
                                .withWeeklySpendLimit(450_000_000L)
                                .withBidCeiling(8_000_000L)
                                .withProfitability(34_560_000L)))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.YES);
        expectedStrategy.withPlatform(CampaignsPlatform.SEARCH);
        expectedStrategy.withStrategy(null);
        expectedStrategy.withStrategyName(StrategyName.AUTOBUDGET_ROI);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("autobudget_roi")
                .withReserveReturn(70L)
                .withRoiCoef(new BigDecimal("12.34"))
                .withGoalId(GOAL_ID)
                .withSum(new BigDecimal("450"))
                .withBid(new BigDecimal("8"))
                .withProfitability(new BigDecimal("34.56")));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenNetworkTypeIs_MaximumCoverage() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.SERVING_OFF))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.MAXIMUM_COVERAGE));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.NO);
        expectedStrategy.withPlatform(CampaignsPlatform.CONTEXT);
        expectedStrategy.withStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        expectedStrategy.withStrategyName(StrategyName.DEFAULT_);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("default"));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenNetworkTypeIs_WbMaximumClicks() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.SERVING_OFF))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.WB_MAXIMUM_CLICKS)
                        .withWbMaximumClicks(new StrategyMaximumClicksAdd()
                                .withWeeklySpendLimit(400_000_000L)));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.YES);
        expectedStrategy.withPlatform(CampaignsPlatform.CONTEXT);
        expectedStrategy.withStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        expectedStrategy.withStrategyName(StrategyName.AUTOBUDGET);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("autobudget")
                .withSum(new BigDecimal("400")));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenNetworkTypeIs_AverageCpc() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.SERVING_OFF))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.AVERAGE_CPC)
                        .withAverageCpc(new StrategyAverageCpcAdd()
                                .withAverageCpc(4_500_000L)));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.YES);
        expectedStrategy.withPlatform(CampaignsPlatform.CONTEXT);
        expectedStrategy.withStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        expectedStrategy.withStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("autobudget_avg_click")
                .withAvgBid(new BigDecimal("4.5")));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenNetworkTypeIs_WeeklyClickPackage() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.SERVING_OFF))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.WEEKLY_CLICK_PACKAGE)
                        .withWeeklyClickPackage(new StrategyWeeklyClickPackageAdd()
                                .withClicksPerWeek(480L)
                                .withBidCeiling(8_750_000L)));

        TextCampaignAddItem textCampaignAddItem = new TextCampaignAddItem()
                .withCounterIds(new ArrayOfInteger().withItems(List.of(COUNTER_ID)))
                .withBiddingStrategy(strategy);

        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(LocalDate.now().toString())
                .withTextCampaign(textCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);
        checkFirstElementHasError(response, 5006);
    }

    @Test
    public void addTextCampaign_whenNetworkTypeIs_WbMaximumConversionRate() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.SERVING_OFF))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.WB_MAXIMUM_CONVERSION_RATE)
                        .withWbMaximumConversionRate(new StrategyMaximumConversionRateAdd()
                                .withWeeklySpendLimit(456_789_000L)
                                .withGoalId(GOAL_ID)));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.YES);
        expectedStrategy.withPlatform(CampaignsPlatform.CONTEXT);
        expectedStrategy.withStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        expectedStrategy.withStrategyName(StrategyName.AUTOBUDGET);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("autobudget")
                .withSum(new BigDecimal("456.79"))
                .withGoalId(GOAL_ID));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenSearchTypeIs_HighestPosition_andNetworkMaximumCoverage() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.MAXIMUM_COVERAGE));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.NO);
        expectedStrategy.withPlatform(CampaignsPlatform.BOTH);
        expectedStrategy.withStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        expectedStrategy.withStrategyName(StrategyName.DEFAULT_);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("default"));

        checkStrategy(campaignId, expectedStrategy);
    }

    @Test
    public void addTextCampaign_whenSearchTypeIs_HighestPosition_andNetworkDefault() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.HIGHEST_POSITION))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT)
                        .withNetworkDefault(new StrategyNetworkDefaultAdd()
                                .withLimitPercent(80)));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.NO);
        expectedStrategy.withPlatform(CampaignsPlatform.BOTH);
        expectedStrategy.withStrategy(null);
        expectedStrategy.withStrategyName(StrategyName.DEFAULT_);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("default"));

        checkStrategy(campaignId, expectedStrategy);
        checkContextLimit(campaignId, 80);
    }

    @Test
    public void addTextCampaign_whenSearchTypeIs_AverageRoi_andNetworkDefault() {
        var strategy = new TextCampaignStrategyAdd()
                .withSearch(new TextCampaignSearchStrategyAdd()
                        .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.AVERAGE_ROI)
                        .withAverageRoi(new StrategyAverageRoiAdd()
                                .withReserveReturn(40)
                                .withRoiCoef(259_000L)
                                .withGoalId(GOAL_ID)))
                .withNetwork(new TextCampaignNetworkStrategyAdd()
                        .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.NETWORK_DEFAULT)
                        .withNetworkDefault(new StrategyNetworkDefaultAdd()));

        Long campaignId = addTextCampaignWithStrategy(strategy);

        var expectedStrategy = new DbStrategy();
        expectedStrategy.withAutobudget(CampaignsAutobudget.YES);
        expectedStrategy.withPlatform(CampaignsPlatform.BOTH);
        expectedStrategy.withStrategy(null);
        expectedStrategy.withStrategyName(StrategyName.AUTOBUDGET_ROI);
        expectedStrategy.withStrategyData(new StrategyData()
                .withVersion(1L)
                .withName("autobudget_roi")
                .withReserveReturn(40L)
                .withRoiCoef(new BigDecimal("0.25"))
                .withGoalId(GOAL_ID));

        checkStrategy(campaignId, expectedStrategy);
        checkContextLimit(campaignId, 0);
    }

    private Long addTextCampaignWithStrategy(TextCampaignStrategyAdd strategy) {
        TextCampaignAddItem textCampaignAddItem = new TextCampaignAddItem()
                .withCounterIds(new ArrayOfInteger().withItems(List.of(COUNTER_ID)))
                .withBiddingStrategy(strategy);

        var campaignAddItem = new CampaignAddItem()
                .withName(NAME)
                .withStartDate(LocalDate.now().toString())
                .withTextCampaign(textCampaignAddItem);

        AddRequest request = new AddRequest().withCampaigns(campaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
        return response.getAddResults().get(0).getId();
    }

    private void checkStrategy(Long campaignId, DbStrategy expected) {
        TextCampaign campaign = getTextCampaign(campaignId);
        DbStrategy actual = campaign.getStrategy();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getAutobudget()).as("autobudget").isEqualTo(expected.getAutobudget());
            softly.assertThat(actual.getPlatform()).as("platform").isEqualTo(expected.getPlatform());
            softly.assertThat(actual.getStrategy()).as("strategy").isEqualTo(expected.getStrategy());
            softly.assertThat(actual.getStrategyName()).as("strategyName").isEqualTo(expected.getStrategyName());
            softly.assertThat(actual.getStrategyData()).as("strategyData").isEqualTo(expected.getStrategyData());
        });
    }

    private void checkContextLimit(Long campaignId, Integer expected) {
        TextCampaign campaign = getTextCampaign(campaignId);
        assertThat(campaign.getContextLimit()).isEqualTo(expected);
    }

    private TextCampaign getTextCampaign(Long campaignId) {
        var campaigns = campaignTypedRepository.getTypedCampaignsMap(clientInfo.getShard(), List.of(campaignId));
        return (TextCampaign) campaigns.get(campaignId);
    }

    private void checkFirstElementHasError(AddResponse response, Integer expectedErrorCode) {
        List<ExceptionNotification> returnedErrors = response.getAddResults().get(0).getErrors();
        assertThat(returnedErrors).hasSize(1);
        assertThat(returnedErrors.get(0).getCode()).isEqualTo(expectedErrorCode);
    }
}
