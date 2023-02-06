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
import ru.yandex.direct.core.entity.adgroup.repository.internal.AdGroupBsTagsRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignWithBrandSafetyService;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.inventori.service.CampaignInfoCollector;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.inventori.InventoriClient;
import ru.yandex.direct.inventori.model.request.AudienceGroup;
import ru.yandex.direct.inventori.model.request.AudioCreative;
import ru.yandex.direct.inventori.model.request.CampaignParametersCorrections;
import ru.yandex.direct.inventori.model.request.CryptaGroup;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.MainPageTrafficType;
import ru.yandex.direct.inventori.model.request.MobileOsType;
import ru.yandex.direct.inventori.model.request.PlatformCorrections;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.request.TrafficTypeCorrections;
import ru.yandex.direct.inventori.model.request.VideoCreative;
import ru.yandex.direct.inventori.model.response.ForecastResponse;
import ru.yandex.direct.inventori.model.response.GeneralForecastResponse;
import ru.yandex.direct.inventori.model.response.IndoorPredictionResponse;
import ru.yandex.direct.inventori.model.response.OutdoorPredictionResponse;
import ru.yandex.direct.web.core.entity.inventori.model.ReachIndoorRequest;
import ru.yandex.direct.web.core.entity.inventori.model.ReachOutdoorRequest;
import ru.yandex.direct.web.core.entity.inventori.model.ReachRequest;
import ru.yandex.direct.web.core.entity.inventori.service.CryptaService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriService;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebService;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.BROWSER_NEW_TAB;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType.FRONTPAGE_MOBILE;
import static ru.yandex.direct.core.entity.inventori.service.InventoriServiceCore.ALLOWED_BLOCK_SIZES;
import static ru.yandex.direct.core.entity.inventori.service.InventoriServiceCore.ALLOWED_BLOCK_SIZES_FOR_FRONTPAGE_MOBILE;
import static ru.yandex.direct.core.entity.inventori.service.type.AdGroupDataConverter.INCOME_B2;
import static ru.yandex.direct.core.entity.inventori.service.type.AdGroupDataConverter.VIDEO_PROPORTION_16_9;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.DEFAULT_PERCENT;
import static ru.yandex.direct.core.testing.data.TestCreatives.DEFAULT_AUDIO_FORMAT_HEIGHT;
import static ru.yandex.direct.core.testing.data.TestCreatives.DEFAULT_AUDIO_FORMAT_WIDTH;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultAdaptive;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultCpmRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRule;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRules;
import static ru.yandex.direct.inventori.model.request.AudienceGroup.GroupingType.ANY;
import static ru.yandex.direct.inventori.model.request.GroupType.AUDIO;
import static ru.yandex.direct.inventori.model.request.GroupType.BANNER;
import static ru.yandex.direct.inventori.model.request.GroupType.MAIN_PAGE_AND_NTP;
import static ru.yandex.direct.inventori.model.request.GroupType.VIDEO;

// todo добавить тесты на данные фронта и существующую группу в рамках DIRECT-104384
public class InventoriWebServiceExistingGroupsTest extends CampaignForecastControllerTestBase {

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

    @Autowired
    private FeatureService featureService;

    @Mock
    private InventoriClient inventoriClient;

    @Mock
    private InventoriService inventoriService;

    @Mock
    PricePackageService pricePackageService;

    @Mock
    CampaignRepository campaignRepository;

    @Autowired
    AdGroupBsTagsRepository adGroupBsTagsRepository;

    private InventoriWebService inventoriWebService;

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
        testCryptaSegmentRepository.clean();
        createAdjustments(campaignInfo);

        MockitoAnnotations.initMocks(this);

        when(inventoriClient.getForecast(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new ForecastResponse().withReach(0L));
        when(inventoriClient.getOutdoorPrediction(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new OutdoorPredictionResponse().withReach(0L));
        when(inventoriClient.getIndoorPrediction(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new IndoorPredictionResponse().withReach(0L));
        when(inventoriClient.getGeneralForecast(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new GeneralForecastResponse());

        inventoriWebService = new InventoriWebService(shardHelper, inventoriClient, cryptaService, authenticationSource,
                userService, campaignInfoCollector, inventoriService, campaignRepository, pricePackageService,
                featureService, retargetingConditionRepository, campaignWithBrandSafetyService, adGroupRepository, cryptaSegmentRepository);

        steps.cryptaGoalsSteps().addAllSocialDemoGoals();
        goalSocialDemo = cryptaSegmentRepository.getById(2499000002L); // Женщины
        goalIncomeB1 = cryptaSegmentRepository.getById(2499000010L); // Средний доход

        goalMetrika = defaultGoalByType(GoalType.GOAL);
        goalAudience = defaultGoalByType(GoalType.AUDIENCE);
        goalInterest1 = defaultGoalByType(GoalType.INTERESTS);
        goalInterest2 = defaultGoalByType(GoalType.INTERESTS);
        goalInterest3 = defaultGoalByType(GoalType.INTERESTS);
        goalGenre = defaultGoalByType(GoalType.AUDIO_GENRES);
        goalContentCategory = (Goal) defaultGoalByType(GoalType.CONTENT_CATEGORY)
                .withKeyword("982")
                .withKeywordValue("4294968299");
        testCryptaSegmentRepository.addAll(asList(goalInterest1, goalInterest2, goalInterest3, goalGenre, goalContentCategory));
    }

    @Test
    @Description("Группа типа BANNER")
    public void getReachForecast_Banner() {
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreativeWithSize(campaignInfo.getClientInfo(), 90L, 728L);

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalAudience), singletonList(goalSocialDemo)));

        createFullCpmBannerAdGroup(creativeInfo.getCreativeId(), retargetingCondition);

        ReachRequest request = getDefaultRequest(BANNER);

        Target expected = getDefaultTarget(BANNER)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"));

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа BANNER c GenresAndCategories")
    public void getReachForecast_BannerWithGenresAndCategories() {
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreativeWithSize(campaignInfo.getClientInfo(), 90L, 728L);

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalAudience), singletonList(goalSocialDemo), singletonList(goalContentCategory)));

        createFullCpmBannerAdGroup(creativeInfo.getCreativeId(), retargetingCondition);

        ReachRequest request = getDefaultRequest(BANNER);

        Target expected = getDefaultTarget(BANNER)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"))
                .withGenresAndCategories(List.of("982:4294968299"));

        checkTargetGeneralReachForecast(request, expected);
    }

    @Test
    @Description("Группа типа BANNER без условий ретаргетинга")
    public void getReachForecast_BannerWithoutConditions() {
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreativeWithSize(campaignInfo.getClientInfo(), 90L, 728L);

        createFullCpmBannerAdGroup(creativeInfo.getCreativeId(), null);

        ReachRequest request = getDefaultRequest(BANNER);

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
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreativeWithSize(campaignInfo.getClientInfo(), 90L, 728L);

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalMetrika)));

        createFullCpmBannerAdGroup(creativeInfo.getCreativeId(), retargetingCondition);

        ReachRequest request = getDefaultRequest(BANNER);

        inventoriWebService.getReachForecast(request);
        verify(inventoriClient, times(0)).getForecast(any(), any(), any(), any(), any(), any());
    }

    @Test
    @Description("Группа типа BANNER с ключевыми словами не должны отправляться")
    public void getReachForecast_BannerKeywords() {
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreativeWithSize(campaignInfo.getClientInfo(), 90L, 728L);

        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroupWithKeywordsCriterionType(campaignInfo);
        steps.bannerSteps().createActiveCpmBanner(activeCpmBanner(campaignInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(), creativeInfo.getCreativeId()), adGroupInfo);

        ReachRequest request = getDefaultRequest(BANNER);
        checkTarget(request, null, 1);
    }

    @Test
    @Description("Группа типа BANNER с короткими и длинными интересами")
    public void getReachForecast_BannerInterestTypes() {
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreativeWithSize(campaignInfo.getClientInfo(), 90L, 728L);

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(asList(
                        defaultRule(asList(goalInterest1, goalInterest2), CryptaInterestType.short_term),
                        defaultRule(asList(goalInterest2, goalInterest3), CryptaInterestType.all)
                ));

        createFullCpmBannerAdGroup(creativeInfo.getCreativeId(), retargetingCondition);

        ReachRequest request = getDefaultRequest(BANNER);

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
    @Description("Группа типа BANNER со средним доходом")
    public void getReachForecast_BannerWithAverageIncome() {
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreativeWithSize(campaignInfo.getClientInfo(), 90L, 728L);

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalIncomeB1)));

        createFullCpmBannerAdGroup(creativeInfo.getCreativeId(), retargetingCondition);

        ReachRequest request = getDefaultRequest(BANNER);

        Target expected = getDefaultTarget(BANNER)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"))
                .withCryptaGroups(singletonList(new CryptaGroup(asSet(getSegment(goalIncomeB1), INCOME_B2))))
                .withAudienceGroups(null);

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа BANNER без баннеров")
    public void getReachForecast_BannerWithoutBanners() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);
        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalAudience), singletonList(goalSocialDemo)));
        createRetargetingCondition(retargetingCondition);

        steps.bidModifierSteps().createDefaultAdGroupIosBidModifierMobile(adGroupInfo);
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDesktop(adGroupInfo);

        ReachRequest request = getDefaultRequest(BANNER);

        Target expected = getDefaultTarget(BANNER)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"))
                .withBlockSizes(new ArrayList<>(ALLOWED_BLOCK_SIZES));

        checkTarget(request, expected, 1);
    }

    @Test
    @Description("Группа типа BANNER с адаптивным креативом")
    public void getReachForecast_BannerAdaptiveCreative() {
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(
                defaultAdaptive(clientInfo.getClientId(), null), clientInfo);

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalAudience), singletonList(goalSocialDemo)));

        createFullCpmBannerAdGroup(creativeInfo.getCreativeId(), retargetingCondition);

        ReachRequest request = getDefaultRequest(BANNER);

        Target expected = getDefaultTarget(BANNER)
                .withExcludedDomains(asSet("ya.ru"))
                .withExcludedSsp(asSet("Smaato"))
                .withBlockSizes(new ArrayList<>(ALLOWED_BLOCK_SIZES));

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа VIDEO")
    public void getReachForecast_Video() {
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultVideoAdditionCreative(campaignInfo.getClientInfo());

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalAudience), singletonList(goalSocialDemo)));

        createFullCpmVideoBannerAdGroup(creativeInfo.getCreativeId(), retargetingCondition);

        ReachRequest request = getDefaultRequest(VIDEO);

        Target expected = getDefaultTarget(VIDEO)
                .withExcludedDomains(singleton("video.ru"))
                .withExcludedSsp(singleton("Smaato"))
                .withBlockSizes(null)
                .withEnableNonSkippableVideo(false)
                .withVideoCreatives(singletonList(new VideoCreative(4_000, null, singleton(VIDEO_PROPORTION_16_9))));

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа AUDIO")
    public void getReachForecast_Audio() {
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultAudioAdditionCreative(campaignInfo.getClientInfo());

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalAudience), singletonList(goalSocialDemo),
                        singletonList(goalGenre)));

        createFullCpmAudioBannerAdGroup(creativeInfo.getCreativeId(), retargetingCondition);

        ReachRequest request = getDefaultRequest(AUDIO);

        Target expected = getDefaultTarget(AUDIO)
                .withBlockSizes(null)
                .withGenresAndCategories(singletonList(getSegment(goalGenre)))
                .withAudioCreatives(singletonList(new AudioCreative(3_000,
                        new ru.yandex.direct.inventori.model.request.BlockSize((int) DEFAULT_AUDIO_FORMAT_WIDTH,
                                (int) DEFAULT_AUDIO_FORMAT_HEIGHT))));

        checkTarget(request, expected);
    }

    @Test
    @Description("Группа типа MAIN_PAGE_AND_NTP")
    public void getReachForecast_FrontPage() {
        createCpmYndxFrontpageCampaign(asSet(FRONTPAGE, FRONTPAGE_MOBILE));

        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCanvasCreativeWithSize(campaignInfo.getClientInfo(), 90L, 728L);

        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalAudience), singletonList(goalSocialDemo)));

        createFullCpmYndxFrontpageAdGroup(creativeInfo.getCreativeId(), retargetingCondition);

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
        createCpmYndxFrontpageCampaign(asSet(FRONTPAGE_MOBILE, BROWSER_NEW_TAB));

        adGroupInfo = steps.adGroupSteps().createActiveCpmYndxFrontpageAdGroup(campaignInfo);
        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(defaultRules(singletonList(goalAudience), singletonList(goalSocialDemo)));
        createRetargetingCondition(retargetingCondition);

        steps.bidModifierSteps().createDefaultAdGroupIosBidModifierMobile(adGroupInfo);
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDesktop(adGroupInfo);

        ReachRequest request = getDefaultRequest(MAIN_PAGE_AND_NTP);

        Target expected = getDefaultTarget(MAIN_PAGE_AND_NTP)
                .withBlockSizes(new ArrayList<>(ALLOWED_BLOCK_SIZES_FOR_FRONTPAGE_MOBILE))
                .withMainPageTrafficType(MainPageTrafficType.MOBILE)
                .withCryptaGroups(null)
                .withAudienceGroups(null)
                .withCorrections(null)
                .withTargetTags(List.of(TargetTagEnum.PORTAL_HOME_MOBILE_TAG.getTypedValue(),
                        TargetTagEnum.PORTAL_HOME_NTP_TAG.getTypedValue(),
                        TargetTagEnum.PORTAL_HOME_NTP_CHROME_TAG.getTypedValue()));

        checkTarget(request, expected, 1);
    }

    private ReachRequest getDefaultRequest(GroupType groupType) {
        return new ReachRequest()
                .withGroupType(groupType)
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdgroupId(adGroupInfo.getAdGroupId());
    }

    private Target getDefaultTarget(GroupType groupType) {
        return new Target()
                .withGroupType(groupType)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBlockSizes(singletonList(new ru.yandex.direct.inventori.model.request.BlockSize(728, 90)))
                .withRegions(singleton(225))
                .withCryptaGroups(singletonList(new CryptaGroup(asSet(getSegment(goalSocialDemo)))))
                .withAudienceGroups(singletonList(new AudienceGroup(ANY, singleton("" + goalAudience.getId()))))
                .withPlatformCorrections(new PlatformCorrections(DEFAULT_PERCENT, DEFAULT_PERCENT, MobileOsType.IOS))
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

        if (expected == null) {
            assertNull(result);
        } else {
            assertThat(result, beanDiffer(expected).useCompareStrategy(
                    allFieldsExcept(newPath("targetTags"), newPath("orderTags"))));
            if (expected.getTargetTags() != null) {
                assertThat(result.getTargetTags(), containsInAnyOrder(expected.getTargetTags().toArray()));
            }
            if (expected.getOrderTags() != null) {
                assertThat(result.getOrderTags(), containsInAnyOrder(expected.getOrderTags().toArray()));
            }
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
}
