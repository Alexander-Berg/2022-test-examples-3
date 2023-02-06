package ru.yandex.direct.web.entity.inventori.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum;
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
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.request.AudienceGroup;
import ru.yandex.direct.inventori.model.request.AudioCreative;
import ru.yandex.direct.inventori.model.request.CampaignParametersCorrections;
import ru.yandex.direct.inventori.model.request.CryptaGroup;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.MainPageTrafficType;
import ru.yandex.direct.inventori.model.request.MobileOsType;
import ru.yandex.direct.inventori.model.request.PageBlock;
import ru.yandex.direct.inventori.model.request.PlatformCorrections;
import ru.yandex.direct.inventori.model.request.ProfileCorrection;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.request.TrafficTypeCorrections;
import ru.yandex.direct.inventori.model.request.VideoCreative;
import ru.yandex.direct.inventori.model.response.ForecastResponse;
import ru.yandex.direct.inventori.model.response.GeneralForecastResponse;
import ru.yandex.direct.inventori.model.response.IndoorPredictionResponse;
import ru.yandex.direct.inventori.model.response.OutdoorPredictionResponse;
import ru.yandex.direct.web.core.entity.inventori.model.AudioCreativeWeb;
import ru.yandex.direct.web.core.entity.inventori.model.BidModifierDemographicWeb;
import ru.yandex.direct.web.core.entity.inventori.model.BlockSize;
import ru.yandex.direct.web.core.entity.inventori.model.Condition;
import ru.yandex.direct.web.core.entity.inventori.model.MobileOsTypeWeb;
import ru.yandex.direct.web.core.entity.inventori.model.PageBlockWeb;
import ru.yandex.direct.web.core.entity.inventori.model.PlatformCorrectionsWeb;
import ru.yandex.direct.web.core.entity.inventori.model.ReachIndoorRequest;
import ru.yandex.direct.web.core.entity.inventori.model.ReachOutdoorRequest;
import ru.yandex.direct.web.core.entity.inventori.model.ReachRequest;
import ru.yandex.direct.web.core.entity.inventori.model.VideoCreativeWeb;
import ru.yandex.direct.web.core.entity.inventori.service.CryptaService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService;
import ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE_MOBILE;
import static ru.yandex.direct.core.entity.inventori.service.InventoriServiceCore.ALLOWED_BLOCK_SIZES;
import static ru.yandex.direct.core.entity.inventori.service.InventoriServiceCore.ALLOWED_BLOCK_SIZES_FOR_FRONTPAGE_MOBILE;
import static ru.yandex.direct.core.entity.inventori.service.type.AdGroupDataConverter.INCOME_B2;
import static ru.yandex.direct.core.entity.inventori.service.type.AdGroupDataConverter.VIDEO_PROPORTION_16_9;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.inventori.model.request.AudienceGroup.GroupingType.ANY;
import static ru.yandex.direct.inventori.model.request.GroupType.AUDIO;
import static ru.yandex.direct.inventori.model.request.GroupType.BANNER;
import static ru.yandex.direct.inventori.model.request.GroupType.INDOOR;
import static ru.yandex.direct.inventori.model.request.GroupType.MAIN_PAGE_AND_NTP;
import static ru.yandex.direct.inventori.model.request.GroupType.VIDEO;
import static ru.yandex.direct.inventori.model.request.ProfileCorrection.Age._25_34;
import static ru.yandex.direct.inventori.model.request.ProfileCorrection.Gender.MALE;
import static ru.yandex.direct.web.core.model.retargeting.RetargetingConditionRuleType.or;

public class InventoriWebServiceNewGroupsTest extends CampaignForecastControllerTestBase {

    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    @Autowired
    private CampaignWithBrandSafetyService campaignWithBrandSafetyService;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private CryptaSegmentRepository cryptaSegmentRepository;

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
    private Steps steps;

    @Mock
    PricePackageService pricePackageService;

    @Mock
    CampaignRepository campaignRepository;

    private Goal goalMetrika;
    private Goal goalSocialDemo;
    private Goal goalInterest1;
    private Goal goalInterest2;
    private Goal goalInterest3;
    private Goal goalGenre;
    private Goal goalAudience;
    private Goal goalIncomeB1;
    private Goal goalContentCategory;

    @Before
    public void before() {
        super.before();
        createAdjustments(campaignInfo);

        MockitoAnnotations.initMocks(this);

        when(inventoriClient.getForecast(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new ForecastResponse().withReach(0L));
        when(inventoriClient.getGeneralForecast(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new GeneralForecastResponse());
        when(inventoriClient.getOutdoorPrediction(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new OutdoorPredictionResponse().withReach(0L));
        when(inventoriClient.getIndoorPrediction(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new IndoorPredictionResponse().withReach(0L));

        inventoriWebService = new InventoriWebService(shardHelper, inventoriClient, cryptaService, authenticationSource,
                userService, campaignInfoCollector, inventoriService, campaignRepository, pricePackageService,
                featureService, retargetingConditionRepository, campaignWithBrandSafetyService, adGroupRepository, cryptaSegmentRepository);

        goalMetrika = defaultGoalByType(GoalType.GOAL);
        goalContentCategory = (Goal) defaultGoalByType(GoalType.CONTENT_CATEGORY)
                .withKeyword("982")
                .withKeywordValue("4294968299");
        goalAudience = defaultGoalByType(GoalType.AUDIENCE);
        goalSocialDemo = defaultGoalByType(GoalType.SOCIAL_DEMO);
        goalIncomeB1 = (Goal) defaultGoalByType(GoalType.SOCIAL_DEMO).withKeyword("618").withKeywordValue("1");
        goalInterest1 = defaultGoalByType(GoalType.INTERESTS);
        goalInterest2 = defaultGoalByType(GoalType.INTERESTS);
        goalInterest3 = defaultGoalByType(GoalType.INTERESTS);
        goalGenre = defaultGoalByType(GoalType.AUDIO_GENRES);
        testCryptaSegmentRepository.clean();
        testCryptaSegmentRepository.addAll(asList(goalSocialDemo, goalIncomeB1, goalInterest1, goalInterest2,
                goalInterest3, goalGenre, goalContentCategory));
    }

    @Test
    @Description("Группа типа BANNER")
    public void getReachForecast_Banner() {
        ReachRequest request = getDefaultRequest(BANNER);
        Target expected = getDefaultTarget(BANNER)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"));
        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа BANNER c GenresAndCategories")
    public void getReachForecast_BannerWithGenresAndCategories() {
        var goal = new ru.yandex.direct.web.core.entity.inventori.model.Goal(4294968299L, 0);
        steps.cryptaGoalsSteps().addGoals((Goal)new Goal().withId(goal.getId()));
        ReachRequest request = getDefaultRequest(BANNER)
                .withConditions(singletonList(
                        new Condition().withType(or)
                                .withGoals(List.of(toWebGoal(goalContentCategory)))));
        Target expected = getDefaultTarget(BANNER)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"))
                .withGenresAndCategories(List.of("982:4294968299"));
        checkTargetGeneralReachForecast(request, expected);
    }

    @Test
    @Description("Группа типа BANNER без условий ретаргетинга")
    public void getReachForecast_BannerWithoutConditions() {
        ReachRequest request = getDefaultRequest(BANNER)
                .withConditions(null);
        Target expected = getDefaultTarget(BANNER)
                .withCryptaGroups(null)
                .withAudienceGroups(null)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"));
        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа BANNER с целями метрики не должны отправляться")
    @Ignore //todo пока решили отправлять метрику в инвентори, окончательно разобраться в рамках DIRECT-104384
    public void getReachForecast_BannerWithMetrikaConditions() {
        ReachRequest request = getDefaultRequest(BANNER);
        List<Condition> conditions = new ArrayList<>(request.getConditions());
        conditions.add(new Condition().withType(or).withGoals(singletonList(toWebGoal(goalMetrika))));
        request
                .withAudioCreatives(singletonList(new AudioCreativeWeb(30, new BlockSize(100, 200))))
                .withConditions(conditions);

        inventoriWebService.getReachForecast(request);
        verify(inventoriClient, times(0)).getForecast(any(), any(), any(), any(), any(), any());
    }

    @Test
    @Description("Группа типа BANNER с короткими и длинными интересами")
    public void getReachForecast_BannerInterestTypes() {
        ReachRequest request = getDefaultRequest(BANNER)
                .withConditions(asList(
                        new Condition().withType(or).withInterestType(CryptaInterestTypeWeb.short_term)
                                .withGoals(asList(toWebGoal(goalInterest1), toWebGoal(goalInterest2))),
                        new Condition().withType(or).withInterestType(CryptaInterestTypeWeb.all)
                                .withGoals(asList(toWebGoal(goalInterest2), toWebGoal(goalInterest3)))));

        Target expected = getDefaultTarget(BANNER)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"))
                .withCryptaGroups(asList(
                        new CryptaGroup(asSet(getSegmentShort(goalInterest1), getSegmentShort(goalInterest2))),
                        new CryptaGroup(asSet(getSegment(goalInterest2), getSegment(goalInterest3),
                                getSegmentShort(goalInterest2), getSegmentShort(goalInterest3)))))
                .withAudienceGroups(null);

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа BANNER - пустые кейворды в интересах должны игнорироваться")
    public void getReachForecast_BannerInterestTypesEmptyKeywods() {
        //null, "" и "0" - пустые кейворды, должен остаться только краткосрочный интерес второй цели
        goalInterest1 = (Goal) defaultGoalByType(GoalType.INTERESTS).withKeyword(null).withKeywordShort("");
        goalInterest2 = (Goal) defaultGoalByType(GoalType.INTERESTS).withKeyword("0");
        testCryptaSegmentRepository.addAll(asList(goalInterest1, goalInterest2));

        ReachRequest request = getDefaultRequest(BANNER)
                .withConditions(singletonList(
                        new Condition().withType(or).withInterestType(CryptaInterestTypeWeb.all)
                                .withGoals(asList(toWebGoal(goalInterest1), toWebGoal(goalInterest2)))));

        Target expected = getDefaultTarget(BANNER)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"))
                .withCryptaGroups(singletonList(new CryptaGroup(singleton(getSegmentShort(goalInterest2)))))
                .withAudienceGroups(null);

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа BANNER со средним доходом")
    public void getReachForecast_BannerWithAverageIncome() {
        ReachRequest request = getDefaultRequest(BANNER)
                .withConditions(singletonList(
                        new Condition().withType(or).withGoals(singletonList(toWebGoal(goalIncomeB1)))));

        Target expected = getDefaultTarget(BANNER)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"))
                .withCryptaGroups(singletonList(new CryptaGroup(asSet(getSegment(goalIncomeB1), INCOME_B2))))
                .withAudienceGroups(null);

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа BANNER с адаптивным креативом")
    public void getReachForecast_BannerHasAdaptive() {
        ReachRequest request = getDefaultRequest(BANNER)
                .withHasAdaptiveCreative(true);

        Target expected = getDefaultTarget(BANNER)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"))
                .withBlockSizes(new ArrayList<>(ALLOWED_BLOCK_SIZES));

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа VIDEO")
    public void getReachForecast_Video() {
        ReachRequest request = getDefaultRequest(VIDEO)
                .withVideoCreatives(singletonList(new VideoCreativeWeb(30)));

        Target expected = getDefaultTarget(VIDEO)
                .withExcludedDomains(singleton("video.ru"))
                .withExcludedSsp(singleton("Smaato"))
                .withBlockSizes(null)
                .withEnableNonSkippableVideo(false)
                .withVideoCreatives(singletonList(new VideoCreative(30_000, null, singleton(VIDEO_PROPORTION_16_9))));

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа AUDIO")
    public void getReachForecast_Audio() {
        ReachRequest request = getDefaultRequest(AUDIO);
        List<Condition> conditions = new ArrayList<>(request.getConditions());
        conditions.add(new Condition().withType(or).withGoals(singletonList(toWebGoal(goalGenre))));
        request
                .withAudioCreatives(singletonList(new AudioCreativeWeb(30, new BlockSize(100, 200))))
                .withConditions(conditions);

        Target expected = getDefaultTarget(AUDIO)
                .withBlockSizes(null)
                .withGenresAndCategories(singletonList(getSegment(goalGenre)))
                .withAudioCreatives(singletonList(new AudioCreative(30_000,
                        new ru.yandex.direct.inventori.model.request.BlockSize(100, 200))));

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа MAIN_PAGE_AND_NTP")
    public void getReachForecast_FrontPage() {
        createCpmYndxFrontpageCampaign(asSet(FRONTPAGE, FRONTPAGE_MOBILE));

        ReachRequest request = getDefaultRequest(MAIN_PAGE_AND_NTP);

        Target expected = getDefaultTarget(MAIN_PAGE_AND_NTP)
                .withMainPageTrafficType(MainPageTrafficType.ALL)
                .withCryptaGroups(null)
                .withAudienceGroups(null)
                .withCorrections(null)
                .withTargetTags(List.of(TargetTagEnum.PORTAL_HOME_DESKTOP_TAG.getTypedValue(),
                        TargetTagEnum.PORTAL_HOME_MOBILE_TAG.getTypedValue()));

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа MAIN_PAGE_AND_NTP без баннеров")
    public void getReachForecast_FrontPageWithEmptyBlockSizes() {
        createCpmYndxFrontpageCampaign(asSet(FRONTPAGE_MOBILE));

        ReachRequest request = getDefaultRequest(MAIN_PAGE_AND_NTP)
                .withBlockSizes(null);

        Target expected = getDefaultTarget(MAIN_PAGE_AND_NTP)
                .withBlockSizes(new ArrayList<>(ALLOWED_BLOCK_SIZES_FOR_FRONTPAGE_MOBILE))
                .withMainPageTrafficType(MainPageTrafficType.MOBILE)
                .withCryptaGroups(null)
                .withAudienceGroups(null)
                .withCorrections(null)
                .withTargetTags(List.of(TargetTagEnum.PORTAL_HOME_MOBILE_TAG.getTypedValue()));

        checkTarget(request, expected, 1);
    }

    @Test
    @Description("Группа типа INDOOR")
    public void getReachForecast_Indoor() {
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCpmIndoorVideoCreative(clientInfo);
        Long pageId = steps.placementSteps().addDefaultIndoorPlacementWithOneBlock().getBlocks().get(0).getPageId();

        ReachIndoorRequest request = new ReachIndoorRequest()
                .withCampaignId(campaignInfo.getCampaignId())
                .withVideoCreativeIds(singletonList(creativeInfo.getCreativeId()))
                .withPageBlocks(singletonList(new PageBlockWeb(pageId, singletonList(77L))))
                .withBidModifierDemographics(singletonList(new BidModifierDemographicWeb("male", "25-34", 110)));

        Target expected = new Target()
                .withGroupType(INDOOR)
                .withPageBlocks(singletonList(new PageBlock(pageId, singletonList(77L))))
                .withVideoCreatives(singletonList(new VideoCreative(1500, null, singleton(VIDEO_PROPORTION_16_9))))
                .withProfileCorrections(singletonList(new ProfileCorrection(MALE, _25_34, 110)));

        checkTarget(request, expected);
    }

    private ReachRequest getDefaultRequest(GroupType groupType) {
        return new ReachRequest()
                .withGroupType(groupType)
                .withCampaignId(campaignInfo.getCampaignId())
                .withBlockSizes(singletonList(new BlockSize(728, 90)))
                .withGeo(singleton(225))
                .withConditions(asList(
                        new Condition().withType(or).withGoals(singletonList(toWebGoal(goalAudience))),
                        new Condition().withType(or).withGoals(singletonList(toWebGoal(goalSocialDemo)))
                ))
                .withPlatformCorrectionsWeb(new PlatformCorrectionsWeb(150, 160, MobileOsTypeWeb.ANDROID));
    }

    private Target getDefaultTarget(GroupType groupType) {
        return new Target()
                .withGroupType(groupType)
                .withBlockSizes(singletonList(new ru.yandex.direct.inventori.model.request.BlockSize(728, 90)))
                .withRegions(singleton(225))
                .withCryptaGroups(singletonList(new CryptaGroup(asSet(getSegment(goalSocialDemo)))))
                .withAudienceGroups(singletonList(new AudienceGroup(ANY, singleton("" + goalAudience.getId()))))
                .withPlatformCorrections(new PlatformCorrections(150, 160, MobileOsType.ANDROID))
                .withCorrections(new CampaignParametersCorrections(
                        new TrafficTypeCorrections(110, 120, 230, 340, 450, 560)));
    }

    private void checkTarget(Object request, Target expected) {
        checkTarget(request, expected, 2);
    }
    private void checkTarget(Object request, Target expected, int times) {
        final ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        if (request instanceof ReachRequest) {
            inventoriWebService.getReachForecast((ReachRequest) request);
            verify(inventoriClient, times(times)).getForecast(anyString(), argument.capture(), any(), any(), any(), any());
        } else {
            if (request instanceof ReachOutdoorRequest) {
                inventoriWebService.getReachOutdoorForecast((ReachOutdoorRequest) request);
                verify(inventoriClient)
                        .getOutdoorPrediction(anyString(), argument.capture(), any(), any(), any(), any());
            } else {
                inventoriWebService.getReachIndoorForecast((ReachIndoorRequest) request);
                verify(inventoriClient)
                        .getIndoorPrediction(anyString(), argument.capture(), any(), any(), any(), any());
            }
        }

        Target result = argument.getValue();
        assertThat(result, beanDiffer(expected).useCompareStrategy(
                allFieldsExcept(newPath("targetTags"), newPath("orderTags"))));
        if (expected.getTargetTags() != null) {
            assertThat(result.getTargetTags(), containsInAnyOrder(expected.getTargetTags().toArray()));
        }
        if (expected.getOrderTags() != null) {
            assertThat(result.getOrderTags(), containsInAnyOrder(expected.getOrderTags().toArray()));
        }
    }

    private void checkTargetGeneralReachForecast(ReachRequest request, Target expected) {
        final ArgumentCaptor<Target> argument = ArgumentCaptor.forClass(Target.class);
        inventoriWebService.getGeneralReachForecast(request);
        verify(inventoriClient, times(1)).getGeneralForecast(anyString(), argument.capture(), any(), any(), any(), any());
        Target result = argument.getValue();
        if (expected.getGenresAndCategories() != null){
            assertThat(result.getGenresAndCategories(), containsInAnyOrder(expected.getGenresAndCategories().toArray()));
        }
    }

    private ru.yandex.direct.web.core.entity.inventori.model.Goal toWebGoal(Goal goal) {
        return new ru.yandex.direct.web.core.entity.inventori.model.Goal().withId(goal.getId());
    }
}
