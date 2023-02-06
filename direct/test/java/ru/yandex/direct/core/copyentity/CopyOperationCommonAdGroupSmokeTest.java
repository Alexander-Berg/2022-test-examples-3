package ru.yandex.direct.core.copyentity;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import junitparams.converters.Nullable;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBidModifiersService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithKeywordsService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithRelevanceMatchesService;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.McBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.core.entity.retargeting.service.AdGroupWithRetargetingsService;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.entity.vcard.service.VcardService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewBannerInfo;
import ru.yandex.direct.core.testing.info.NewDynamicBannerInfo;
import ru.yandex.direct.core.testing.info.NewMcBannerInfo;
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.otherFilterConditions;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;

@SuppressWarnings("rawtypes")
@CoreTest
@RunWith(Parameterized.class)
public class CopyOperationCommonAdGroupSmokeTest {

    private final static Set<AdGroupType> ADGROUP_TYPE_WITH_RELEVANCE_MATCH =
            Set.of(AdGroupType.BASE, AdGroupType.MOBILE_CONTENT);
    private final static Set<AdGroupType> ADGROUP_TYPE_WITH_KEYWORDS =
            Set.of(AdGroupType.BASE, AdGroupType.MOBILE_CONTENT, AdGroupType.MCBANNER);
    private final static Set<CampaignType> CAMPAIGN_TYPE_WITH_VCARD =
            Set.of(CampaignType.TEXT);
    private final static Set<AdGroupType> ADGROUP_TYPE_WITH_RETARGETING =
            Set.of(AdGroupType.BASE, AdGroupType.MOBILE_CONTENT);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private AdGroupService adgroupService;
    @Autowired
    private BannerService bannerService;
    @Autowired
    private AdGroupWithBannersService adGroupWithBannersService;
    @Autowired
    private BidModifierService bidModifierService;
    @Autowired
    private AdGroupWithBidModifiersService adGroupWithBidModifiersService;
    @Autowired
    private Steps steps;
    @Autowired
    private CopyOperationFactory factory;
    @Autowired
    private BannerTypedRepository newBannerTypedRepository;
    @Autowired
    private VcardService vcardService;
    @Autowired
    private AdGroupWithKeywordsService adGroupWithKeywordsService;
    @Autowired
    private KeywordService keywordService;
    @Autowired
    private AdGroupWithRelevanceMatchesService adGroupWithRelevanceMatchesService;
    @Autowired
    private RelevanceMatchService relevanceMatchService;
    @Autowired
    private RetargetingService retargetingService;
    @Autowired
    private AdGroupWithRetargetingsService adGroupWithRetargetingsService;
    @Autowired
    private NetAcl netAcl;

    private Long uid;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private int shard;
    private ClientInfo clientInfoTo;
    private ClientId clientIdTo;

    private Long campaignId;
    private Long campaignIdDifferentClient;
    private Long adGroupId;

    @Parameterized.Parameter
    public CampaignType campaignType;
    @Parameterized.Parameter(1)
    public AdGroupType adGroupType;
    @Parameterized.Parameter(2)
    public Class classOfBanner;

    @Parameterized.Parameters(name = "{0}, {1}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT, AdGroupType.BASE, TextBanner.class},
                {CampaignType.PERFORMANCE, AdGroupType.PERFORMANCE, PerformanceBanner.class},
                {CampaignType.MOBILE_CONTENT, AdGroupType.MOBILE_CONTENT, MobileAppBanner.class},
                {CampaignType.MCBANNER, AdGroupType.MCBANNER, McBanner.class},
                {CampaignType.DYNAMIC, AdGroupType.DYNAMIC, DynamicBanner.class}
        });
    }

    @Before
    public void setUp() {
        doReturn(false).when(netAcl).isInternalIp(any(InetAddress.class));
        var superInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superInfo.getUid();

        steps.trustedRedirectSteps().addValidCounters();

        prepareClientData();
        prepareDifferentClientData();
    }

    private void prepareDifferentClientData() {
        clientInfoTo = steps.clientSteps().createDefaultClient();
        clientIdTo = clientInfoTo.getClientId();

        var campaignInfoDifferentClient = steps.campaignSteps()
                .createActiveCampaignByCampaignType(campaignType, clientInfoTo);
        campaignIdDifferentClient = campaignInfoDifferentClient.getCampaignId();
    }

    @After
    public void after() {
        reset(netAcl);
        steps.trustedRedirectSteps().deleteTrusted();
    }

    @Test
    public void copyAdGroupSameCampaign() {
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupId, campaignId, campaignId, uid);
        var xerox = factory.build(copyConfig);
        var copyResult = xerox.copy();

        checkErrors(copyResult.getMassResult());

        @SuppressWarnings("unchecked")
        Set<Long> copiedAdGroupIds = StreamEx.of(copyResult.getEntityMapping(AdGroup.class).values())
                .select(Long.class)
                .toSet();
        var copiedAdGroups = adgroupService.get(clientId, uid, copiedAdGroupIds);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(copiedAdGroups)
                .as("группы")
                .hasSize(1);

        var copiedBannerIds = adGroupWithBannersService
                .getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        var copiedBanners = bannerService.get(clientId, uid, copiedBannerIds);
        soft.assertThat(copiedBanners)
                .as("баннеры")
                .hasSize(1);

        var copiedBidModifierIds = adGroupWithBidModifiersService
                .getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        var copiedBidModifiers = bidModifierService.get(clientId, uid, copiedBidModifierIds);
        soft.assertThat(copiedBidModifiers)
                .as("корректировки ставок")
                .hasSize(1);

        if (ADGROUP_TYPE_WITH_KEYWORDS.contains(adGroupType)) {
            var copiedKeywordIds = adGroupWithKeywordsService.getChildEntityIdsByParentIds(clientId, uid,
                    copiedAdGroupIds);
            var copiedKeywords = keywordService.get(clientId, uid, copiedKeywordIds);
            soft.assertThat(copiedKeywords)
                    .as("ключевые фразы")
                    .hasSize(1);
        }

        if (ADGROUP_TYPE_WITH_RELEVANCE_MATCH.contains(adGroupType)) {
            var copiedRelevanceMathIds =
                    adGroupWithRelevanceMatchesService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
            var copiedRelevanceMatches = relevanceMatchService.get(clientId, uid, copiedRelevanceMathIds);
            soft.assertThat(copiedRelevanceMatches)
                    .as("автотаргетинг")
                    .hasSize(1);
        }

        if (CAMPAIGN_TYPE_WITH_VCARD.contains(campaignType)) {
            var copiedVcardIds = StreamEx.of(copiedBanners)
                    .select(TextBanner.class).map(TextBanner::getVcardId)
                    .nonNull()
                    .toSet();
            var copiedVcards = vcardService.getVcardsById(clientId, copiedVcardIds);
            soft.assertThat(copiedVcards)
                    .as("визитка")
                    .hasSize(1);
        }

        if (ADGROUP_TYPE_WITH_RETARGETING.contains(adGroupType)) {
            var copiedRetIds = adGroupWithRetargetingsService
                    .getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
            var copiedRetargetings = retargetingService.get(clientId, uid, copiedRetIds);
            soft.assertThat(copiedRetargetings)
                    .as("ретаргетинг")
                    .hasSize(1);
        }

        soft.assertAll();
    }

    @Test
    public void copyAdGroupDifferentCampaignSameClient() {
        CampaignInfo campaignInfoTo = steps.campaignSteps()
                .createActiveCampaignByCampaignType(campaignType, clientInfo);
        Long campaignIdTo = campaignInfoTo.getCampaignId();

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupId, campaignId, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);
        var copyResult = xerox.copy();

        checkErrors(copyResult.getMassResult());

        @SuppressWarnings("unchecked")
        Set<Long> copiedAdGroupIds = StreamEx.of(copyResult.getEntityMapping(AdGroup.class).values())
                .select(Long.class)
                .toSet();
        var copiedAdGroups = adgroupService.get(clientId, uid, copiedAdGroupIds);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(copiedAdGroups)
                .as("группы")
                .hasSize(1);

        var copiedBannerIds = adGroupWithBannersService
                .getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        var copiedBanners = bannerService.get(clientId, uid, copiedBannerIds);
        soft.assertThat(copiedBanners)
                .as("баннеры")
                .hasSize(1);

        var copiedBanner = getBannerSafely(shard, copiedBannerIds);
        soft.assertThat(copiedBanner)
                .as("правильный тип баннера")
                .hasSize(1);

        var copiedBidModifierIds = adGroupWithBidModifiersService
                .getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        var copiedBidModifiers = bidModifierService.get(clientId, uid, copiedBidModifierIds);
        soft.assertThat(copiedBidModifiers)
                .as("корректировки ставок")
                .hasSize(1);

        if (ADGROUP_TYPE_WITH_KEYWORDS.contains(adGroupType)) {
            var copiedKeywordIds = adGroupWithKeywordsService.getChildEntityIdsByParentIds(clientId, uid,
                    copiedAdGroupIds);
            var copiedKeywords = keywordService.get(clientId, uid, copiedKeywordIds);
            soft.assertThat(copiedKeywords)
                    .as("ключевые фразы")
                    .hasSize(1);
        }

        if (ADGROUP_TYPE_WITH_RELEVANCE_MATCH.contains(adGroupType)) {
            var copiedRelevanceMathIds =
                    adGroupWithRelevanceMatchesService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
            var copiedRelevanceMatches = relevanceMatchService.get(clientId, uid, copiedRelevanceMathIds);
            soft.assertThat(copiedRelevanceMatches)
                    .as("автотаргетинг")
                    .hasSize(1);
        }

        if (CAMPAIGN_TYPE_WITH_VCARD.contains(campaignType)) {
            var copiedVcardIds = StreamEx.of(copiedBanners)
                    .select(TextBanner.class).map(TextBanner::getVcardId).nonNull()
                    .toSet();
            var copiedVcards = vcardService.getVcardsById(clientId, copiedVcardIds);
            soft.assertThat(copiedVcards)
                    .as("визитка")
                    .hasSize(1);
        }

        if (ADGROUP_TYPE_WITH_RETARGETING.contains(adGroupType)) {
            var copiedRetIds = adGroupWithRetargetingsService
                    .getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
            var copiedRetargetings = retargetingService.get(clientId, uid, copiedRetIds);
            soft.assertThat(copiedRetargetings)
                    .as("ретаргетинг")
                    .hasSize(1);
        }

        soft.assertAll();
    }

    @Test
    public void copyAdGroupDifferentClient() {
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupBetweenClientsCopyConfig(
                clientInfo, clientInfoTo, adGroupId, campaignId, campaignIdDifferentClient, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();

        // Копирование ретаргетингов и фидов между клиентами пока не реализовано
        if (ADGROUP_TYPE_WITH_RETARGETING.contains(adGroupType) || adGroupType == AdGroupType.PERFORMANCE) {
            assertThat(copyResult.getMassResult().getValidationResult().flattenErrors()).isNotEmpty();
        } else {
            checkErrors(copyResult.getMassResult());

            @SuppressWarnings("unchecked")
            Set<Long> copiedAdGroupIds = StreamEx.of(copyResult.getEntityMapping(AdGroup.class).values())
                    .select(Long.class)
                    .toSet();
            var copiedAdGroups = adgroupService.get(clientIdTo, uid, copiedAdGroupIds);
            SoftAssertions soft = new SoftAssertions();
            soft.assertThat(copiedAdGroups)
                    .as("группы")
                    .hasSize(1);

            var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientIdTo, uid,
                    copiedAdGroupIds);
            var copiedBanners = bannerService.get(clientIdTo, uid, copiedBannerIds);
            soft.assertThat(copiedBanners)
                    .as("баннеры")
                    .hasSize(1);

            var copiedBanner = getBannerSafely(clientInfoTo.getShard(), copiedBannerIds);
            soft.assertThat(copiedBanner)
                    .as("правильный тип баннера")
                    .hasSize(1);

            var copiedBidModifierIds = adGroupWithBidModifiersService
                    .getChildEntityIdsByParentIds(clientIdTo, uid, copiedAdGroupIds);
            var copiedBidModifiers = bidModifierService.get(clientIdTo, uid, copiedBidModifierIds);
            soft.assertThat(copiedBidModifiers)
                    .as("корректировки ставок")
                    .hasSize(1);

            if (ADGROUP_TYPE_WITH_KEYWORDS.contains(adGroupType)) {
                var copiedKeywordIds = adGroupWithKeywordsService
                        .getChildEntityIdsByParentIds(clientIdTo, uid, copiedAdGroupIds);
                var copiedKeywords = keywordService.get(clientIdTo, uid, copiedKeywordIds);
                soft.assertThat(copiedKeywords)
                        .as("ключевые фразы")
                        .hasSize(1);
            }

            if (ADGROUP_TYPE_WITH_RELEVANCE_MATCH.contains(adGroupType)) {
                var copiedRelevanceMathIds = adGroupWithRelevanceMatchesService
                        .getChildEntityIdsByParentIds(clientIdTo, uid, copiedAdGroupIds);
                var copiedRelevanceMatches = relevanceMatchService.get(clientIdTo, uid, copiedRelevanceMathIds);
                soft.assertThat(copiedRelevanceMatches)
                        .as("автотаргетинг")
                        .hasSize(1);
            }

            if (CAMPAIGN_TYPE_WITH_VCARD.contains(campaignType)) {
                var copiedVcardIds = StreamEx.of(copiedBanners)
                        .select(TextBanner.class).map(TextBanner::getVcardId).nonNull()
                        .toSet();
                var copiedVcards = vcardService.getVcardsById(clientIdTo, copiedVcardIds);
                soft.assertThat(copiedVcards)
                        .as("визитка")
                        .hasSize(1);
            }

            soft.assertAll();
        }
    }

    private void checkErrors(MassResult massResult) {
        assertThat(massResult.getValidationResult().flattenErrors()).isEmpty();
    }

    private void prepareClientData() {

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(campaignType, clientInfo);
        campaignId = campaignInfo.getCampaignId();

        var tags = steps.tagCampaignSteps().createDefaultTags(shard, clientId, campaignId, 5);

        AdGroupInfo adGroupInfo;
        VcardInfo vcard = null;
        switch (adGroupType) {
            case BASE:
                vcard = steps.vcardSteps().createVcard(fullVcard()
                        .withUid(clientInfo.getUid())
                        .withCampaignId(campaignId), campaignInfo);
            case DYNAMIC:
            case MOBILE_CONTENT:
            case MCBANNER: {
                var adGroup = TestGroups.activeAdGroupByType(adGroupType, campaignId)
                        .withTags(tags);
                adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);

                adGroupId = adGroupInfo.getAdGroupId();

                createBannerByAdGroupType(campaignInfo, adGroupInfo, vcard);
                break;
            }
            case PERFORMANCE: {
                var filterInfo = createPerformanceAdGroup(campaignInfo);
                adGroupInfo = filterInfo.getAdGroupInfo();
                adGroupId = filterInfo.getAdGroupId();

                steps.bannerCreativeSteps().createPerformanceBannerCreative((PerformanceAdGroupInfo) adGroupInfo);
                break;
            }
            default:
                throw new IllegalArgumentException("Неизвестный тип группы: " + adGroupType);
        }

        if (ADGROUP_TYPE_WITH_KEYWORDS.contains(adGroupType)) {
            steps.keywordSteps().createKeywordWithText("my own keyword", adGroupInfo);
        }

        if (ADGROUP_TYPE_WITH_RELEVANCE_MATCH.contains(adGroupType)) {
            steps.relevanceMatchSteps().addDefaultRelevanceMatch(adGroupInfo);
        }

        if (ADGROUP_TYPE_WITH_RETARGETING.contains(adGroupType)) {
            steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);
        }

        steps.bidModifierSteps().createAdGroupBidModifier(
                TestBidModifiers.createEmptyDemographicsModifier().withDemographicsAdjustments(List.of(
                        TestBidModifiers.createDefaultDemographicsAdjustment()
                                .withAge(AgeType._25_34)
                                .withGender(GenderType.MALE)
                                .withPercent(120))),
                adGroupInfo);
    }

    private void createBannerByAdGroupType(CampaignInfo campaignInfo, AdGroupInfo adGroupInfo,
                                           @Nullable VcardInfo vcardInfo) {

        Function<NewBannerInfo, NewBannerInfo> fillBannerInfo = bannerInfo -> bannerInfo
                .withCampaignInfo(campaignInfo)
                .withAdGroupInfo(adGroupInfo)
                .withClientInfo(clientInfo);

        switch (adGroupType) {
            case BASE:
                var textBanner = ((NewTextBannerInfo) fillBannerInfo.apply(new NewTextBannerInfo()))
                        .withVcardInfo(vcardInfo);
                steps.textBannerSteps().createBanner(textBanner);
                break;
            case DYNAMIC:
                steps.dynamicBannerSteps().createDynamicBanner((NewDynamicBannerInfo) fillBannerInfo
                        .apply(new NewDynamicBannerInfo()));
                break;
            case MOBILE_CONTENT:
                steps.mobileAppBannerSteps().createMobileAppBanner((NewMobileAppBannerInfo) fillBannerInfo
                        .apply(new NewMobileAppBannerInfo()));
                break;
            case MCBANNER:
                steps.mcBannerSteps().createMcBanner((NewMcBannerInfo) fillBannerInfo
                        .apply(new NewMcBannerInfo()));
                break;
            default:
                throw new IllegalArgumentException("Неизвестный тип группы: " + adGroupType);
        }
    }

    @SuppressWarnings("unchecked")
    private List<? extends BannerWithAdGroupId> getBannerSafely(int shard, Set<Long> bannerIds) {
        return newBannerTypedRepository.getSafely(shard, bannerIds, classOfBanner);
    }

    private PerformanceFilterInfo createPerformanceAdGroup(CampaignInfo campaignInfo) {
        var adGroupInfoFrom = steps.adGroupSteps().addPerformanceAdGroup(new PerformanceAdGroupInfo()
                .withClientInfo(campaignInfo.getClientInfo())
                .withCampaignInfo(campaignInfo));
        var filterFrom = defaultPerformanceFilter(adGroupInfoFrom.getAdGroupId(),
                adGroupInfoFrom.getFeedId())
                .withConditions(otherFilterConditions());
        var performanceFilterInfo = new PerformanceFilterInfo()
                .withAdGroupInfo(adGroupInfoFrom)
                .withFilter(filterFrom);
        return steps.performanceFilterSteps().addPerformanceFilter(performanceFilterInfo);
    }
}
