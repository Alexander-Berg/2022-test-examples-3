package ru.yandex.direct.web.entity.inventori.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignWithBrandSafetyService;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.inventori.service.CampaignInfoCollector;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxImpressionsCustomPeriodStrategy;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.request.CampaignPredictionRequest;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.entity.inventori.model.Condition;
import ru.yandex.direct.web.core.entity.inventori.model.CpmCampaignType;
import ru.yandex.direct.web.core.entity.inventori.model.GeneralCpmRecommendationRequest;
import ru.yandex.direct.web.core.entity.inventori.model.MobileOsTypeWeb;
import ru.yandex.direct.web.core.entity.inventori.model.PlatformCorrectionsWeb;
import ru.yandex.direct.web.core.entity.inventori.model.ReachInfo;
import ru.yandex.direct.web.core.entity.inventori.model.ReachRequest;
import ru.yandex.direct.web.core.entity.inventori.model.ReachResult;
import ru.yandex.direct.web.core.entity.inventori.service.CryptaService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxImpressionsCustomPeriodStrategy;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.inventori.InventoriClient.DEFAULT_REACH_LESS_THAN;
import static ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService.DEFAULT_GENERAL_CAMPAIGN_PREDICTION_SUCCES_RESULT;
import static ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService.DEFAULT_GENERAL_CPM_RECOMMENDATION_RESULT;
import static ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService.DEFAULT_MULTI_BUDGETS_PREDICTION_RESPONCE;
import static ru.yandex.direct.web.core.model.retargeting.RetargetingConditionRuleType.or;
import static ru.yandex.direct.web.testing.data.TestCpmForecastRequest.defaultStrategy;

@DirectWebTest
@RunWith(JUnitParamsRunner.class)
public class InventoriWebServiceInvalidGroupTypesTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private CryptaService cryptaService;

    @Autowired
    private UserService userService;

    @Autowired
    private CampaignInfoCollector campaignInfoCollector;

    @Mock
    private InventoriClient inventoriClient;

    @Mock
    private InventoriService inventoriService;

    @Autowired
    private InventoriWebService inventoriWebService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    protected DirectWebAuthenticationSource authenticationSource;

    @Autowired
    protected TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    protected Steps steps;

    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    @Autowired
    private CampaignWithBrandSafetyService campaignWithBrandSafetyService;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private CryptaSegmentRepository cryptaSegmentRepository;

    @Mock
    PricePackageService pricePackageService;

    @Mock
    CampaignRepository campaignRepository;

    private Goal goalSocialDemo;
    private Goal goalAudience;
    protected CampaignInfo campaignInfo;
    protected ClientInfo clientInfo;

    private static final ReachResult DEFAULT_REACH_RESULT_WITHOUT_DETAILED = new ReachResult(null,
            new ReachInfo(DEFAULT_REACH_LESS_THAN, null, null),
            null);

    private static final ReachResult DEFAULT_REACH_RESULT = new ReachResult(null,
            new ReachInfo(DEFAULT_REACH_LESS_THAN, null, null),
            new ReachInfo(DEFAULT_REACH_LESS_THAN, null, null));

    public static GroupType[] getParametersForVariableValueGetter() {
        return new GroupType[]{
                GroupType.OUTDOOR,
                GroupType.INDOOR,
                GroupType.GEO_PIN,
                GroupType.GEOPRODUCT,
                GroupType.BANNER_IN_GEO_APPS,
                GroupType.BANNER_IN_METRO
        };
    }

    @Before
    public void before() {

        MockitoAnnotations.initMocks(this);

        inventoriWebService = new InventoriWebService(shardHelper, inventoriClient, cryptaService, authenticationSource,
                userService, campaignInfoCollector, inventoriService, campaignRepository, pricePackageService,
                featureService, retargetingConditionRepository, campaignWithBrandSafetyService, adGroupRepository, cryptaSegmentRepository);

        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();

        campaignInfo = steps.campaignSteps().createCampaign(activeCpmBannerCampaign(null, null)
                .withStrategy(strategy)
                .withDisabledDomains(singleton("ya.ru"))
                .withDisabledSsp(singletonList("Smaato"))
                .withDisabledVideoPlacements(singletonList("video.ru"))
        );

        clientInfo = campaignInfo.getClientInfo();
        setAuthData();

        goalAudience = defaultGoalByType(GoalType.AUDIENCE);
        goalSocialDemo = defaultGoalByType(GoalType.SOCIAL_DEMO);
        testCryptaSegmentRepository.clean();
        testCryptaSegmentRepository.addAll(asList(goalSocialDemo));
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "getParametersForVariableValueGetter")
    public void getReachForecastTest(GroupType groupType) {
        ReachRequest request = getDefaultRequest(groupType);

        final ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        var result = inventoriWebService.getReachForecast(request);
        verify(inventoriClient, times(0)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        equalsReachResultsWithoutRequestID(result, DEFAULT_REACH_RESULT_WITHOUT_DETAILED);
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "getParametersForVariableValueGetter")
    public void getGeneralReachForecastTest(GroupType groupType) {
        ReachRequest request = getDefaultRequest(groupType);

        final ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        var result = inventoriWebService.getGeneralReachForecast(request);
        verify(inventoriClient, times(0)).getGeneralForecast(anyString(), argument.capture(), any(), any(), any(), any());

        equalsReachResultsWithoutRequestID(result, DEFAULT_REACH_RESULT);
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "getParametersForVariableValueGetter")
    public void getReachRecommendationTest(GroupType groupType) {
        ReachRequest request = getDefaultRequest(groupType);

        final ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        var result = inventoriWebService.getReachRecommendation(request);
        verify(inventoriClient, times(0)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());

        assertNull(result.getBannerFormatIncreasePercent());
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "getParametersForVariableValueGetter")
    public void forecastTest(GroupType groupType) throws JsonProcessingException {
        GeneralCpmRecommendationRequest request = defaultGeneralCpmRecommendationRequestByGroupType(groupType);

        final ArgumentCaptor<CampaignPredictionRequest> argument = ArgumentCaptor.forClass(CampaignPredictionRequest.class);
        var result = inventoriWebService.forecast(request, CurrencyCode.RUB);
        verify(inventoriClient, times(0)).getGeneralRecommendation(anyString(), anyString(), anyString(), argument.capture());

        assertEquals(DEFAULT_GENERAL_CPM_RECOMMENDATION_RESULT, result.getResult());
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "getParametersForVariableValueGetter")
    public void getGeneralCampaignPredictionTest(GroupType groupType) throws JsonProcessingException {
        GeneralCpmRecommendationRequest request = defaultGeneralCpmRecommendationRequestByGroupType(groupType);

        final ArgumentCaptor<CampaignPredictionRequest> argument = ArgumentCaptor.forClass(CampaignPredictionRequest.class);
        var result = inventoriWebService.getGeneralCampaignPrediction(request, CurrencyCode.RUB);
        verify(inventoriClient, times(0)).getGeneralCampaignPrediction(anyString(), anyString(), anyString(), argument.capture());

        assertEquals(DEFAULT_GENERAL_CAMPAIGN_PREDICTION_SUCCES_RESULT, result.getResult());
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "getParametersForVariableValueGetter")
    public void getReachMultiBudgetsForecastTest(GroupType groupType) throws JsonProcessingException {
        ReachRequest request = getDefaultRequest(groupType);

        final ArgumentCaptor<CampaignPredictionRequest> argument = ArgumentCaptor.forClass(CampaignPredictionRequest.class);
        var result = inventoriWebService.getReachMultiBudgetsForecast(request, emptyList());
        verify(inventoriClient, times(0)).getParametrisedCampaignPrediction(anyString(), anyString(), anyString(), argument.capture(), any());

        assertEquals(DEFAULT_MULTI_BUDGETS_PREDICTION_RESPONCE, result.getMultiBudgetsPredictionResponse());
    }

    private ReachRequest getDefaultRequest(GroupType groupType) {
        return new ReachRequest()
                .withGroupType(groupType)
                .withCampaignId(campaignInfo.getCampaignId())
                .withBlockSizes(singletonList(new ru.yandex.direct.web.core.entity.inventori.model.BlockSize(728, 90)))
                .withGeo(singleton(225))
                .withConditions(asList(
                        new Condition().withType(or).withGoals(singletonList(toWebGoal(goalAudience))),
                        new Condition().withType(or).withGoals(singletonList(toWebGoal(goalSocialDemo)))
                ))
                .withPlatformCorrectionsWeb(new PlatformCorrectionsWeb(150, 160, MobileOsTypeWeb.ANDROID));
    }


    private ru.yandex.direct.web.core.entity.inventori.model.Goal toWebGoal(Goal goal) {
        return new ru.yandex.direct.web.core.entity.inventori.model.Goal().withId(goal.getId());
    }

    private void equalsReachInfo(ReachInfo actual, ReachInfo expected) {
        if (expected.getReach() == null) {
            assertNull(actual.getReach());
        } else {
            assertNotNull(actual.getReach());
            assertEquals(expected.getReach(), actual.getReach());
        }

        if (expected.getReachLessThan() == null) {
            assertNull(actual.getReachLessThan());
        } else {
            assertNotNull(actual.getReachLessThan());
            assertEquals(expected.getReachLessThan(), actual.getReachLessThan());
        }
    }

    private void equalsReachResultsWithoutRequestID(ReachResult actual, ReachResult expected) {
        if (expected.getBasic() == null) {
            assertNull(actual.getBasic());
        } else {
            assertNotNull(actual.getBasic());
            equalsReachInfo(actual.getBasic(), expected.getBasic());
        }

        if (expected.getDetailed() == null) {
            assertNull(actual.getDetailed());
        } else {
            assertNotNull(actual.getDetailed());
            equalsReachInfo(actual.getDetailed(), expected.getDetailed());
        }
    }

    private GeneralCpmRecommendationRequest defaultGeneralCpmRecommendationRequestByGroupType(GroupType groupType) {
        return new GeneralCpmRecommendationRequest()
                .withGroupType(groupType)
                .withCampaignId(campaignInfo.getCampaignId())
                .withBlockSizes(singletonList(new ru.yandex.direct.web.core.entity.inventori.model.BlockSize(728, 90)))
                .withGeo(singleton(225))
                .withConditions(asList(
                        new Condition().withType(or).withGoals(singletonList(toWebGoal(goalAudience))),
                        new Condition().withType(or).withGoals(singletonList(toWebGoal(goalSocialDemo)))
                ))
                .withCpmCampaignType(CpmCampaignType.CPM_BANNER)
                .withStrategy(defaultStrategy());
    }

    protected void setAuthData() {
        DirectWebAuthenticationSourceMock authSource =
                (DirectWebAuthenticationSourceMock) authenticationSource;
        authSource.withOperator(new User()
                .withUid(clientInfo.getUid()));
        authSource.withSubjectUser(new User()
                .withClientId(clientInfo.getClientId())
                .withUid(clientInfo.getUid()));

        UserInfo userInfo = clientInfo.getChiefUserInfo();
        User user = userInfo.getUser();
        SecurityContextHolder.getContext()
                .setAuthentication(new DirectAuthentication(user, user));
    }
}

