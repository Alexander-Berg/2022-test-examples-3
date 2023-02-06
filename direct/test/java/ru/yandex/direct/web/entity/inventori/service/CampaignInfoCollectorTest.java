package ru.yandex.direct.web.entity.inventori.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.inventori.service.CampaignInfoCollector;
import ru.yandex.direct.core.entity.inventori.service.type.FrontendData;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.inventori.model.request.AudienceGroup;
import ru.yandex.direct.inventori.model.request.BlockSize;
import ru.yandex.direct.inventori.model.request.CampaignParametersCorrections;
import ru.yandex.direct.inventori.model.request.CryptaGroup;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.MobileOsType;
import ru.yandex.direct.inventori.model.request.PageBlock;
import ru.yandex.direct.inventori.model.request.PlatformCorrections;
import ru.yandex.direct.inventori.model.request.ProfileCorrection;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.request.TrafficTypeCorrections;
import ru.yandex.direct.inventori.model.request.VideoCreative;
import ru.yandex.direct.web.configuration.DirectWebTest;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_AUDIENCE_UPPER_BOUND;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmIndoorBanner;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDemographics;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDesktop;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDesktopAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmIndoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.fullCreative;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultAudience;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestFullGoals.filterCryptaGoals;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmIndoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultTargetInterest;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@DirectWebTest
@RunWith(SpringRunner.class)
public class CampaignInfoCollectorTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    private static final TrafficTypeCorrections DEFAULT_TRAFFIC_TYPE_CORRECTIONS =
            new TrafficTypeCorrections(null, null, null, null, null, null);

    @Autowired
    private CampaignInfoCollector collector;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private CampaignRepository campaignRepository;

    @Before
    public void before() {
        testCryptaSegmentRepository.clean();
        steps.placementSteps().clearPlacements();
    }

    @Test
    @Ignore("Only for manual runs because it connects to real database")
    public void collectCampaignInfo() {

        List<Target> result = collectCampaignInfo(true, 31629089L);
        for (Target curTarget : result) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String curResult = mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(curTarget);
                System.out.println(curResult);
            } catch (JsonProcessingException e) {
                System.out.println("error while parsing json occured");
            }
        }
    }

    @Test
    public void collectCampaignInfo_WithNullCampaignId() {

        var frontendData = new FrontendData()
                .withGeo(Set.of(225))
                .withGroupType(GroupType.VIDEO);
        Map<Long, Pair<List<Target>, Boolean>> campaignsInfo = collector.collectCampaignsInfoWithClientIdAndUid(
                frontendData, true, 1, 1L, ClientId.fromLong(1L),
                singletonList(null), true, null);
        Target expectedTarget = new Target()
                .withRegions(Set.of(225))
                .withGroupType(GroupType.VIDEO)
                .withOrderTags(emptyList())
                .withTargetTags(emptyList())
                .withEnableNonSkippableVideo(false);
        assertThat(campaignsInfo.get(0L).getLeft().get(0))
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expectedTarget);
    }

    @Test
    public void collectCampaignInfo_CpmBannerAdGroup_CommonTarget() {
        CampaignInfo campaign = steps.campaignSteps().createCampaign(activeCpmBannerCampaign(null, null)
                .withDisabledDomains(singleton("ya.ru")));
        ClientInfo clientInfo = campaign.getClientInfo();
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(activeCpmBannerAdGroup(null)
                        .withGeo(singletonList(100L)),
                campaign);
        createRetargetingCondition(adGroup, RuleType.OR, asList(
                defaultAudience(METRIKA_AUDIENCE_UPPER_BOUND - 2),
                (Goal) defaultGoalByType(GoalType.SOCIAL_DEMO).withKeyword("123").withKeywordValue("456")));
        CreativeInfo creative = steps.creativeSteps().createCreative(fullCreative(null, null)
                        .withWidth(1920L)
                        .withHeight(1080L),
                clientInfo);
        steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(campaign.getCampaignId(), adGroup.getAdGroupId(), creative.getCreativeId()),
                adGroup);
        steps.bidModifierSteps().createAdGroupBidModifier(createDefaultBidModifierDesktop(campaign.getCampaignId())
                        .withDesktopAdjustment(createDefaultDesktopAdjustment().withPercent(120)),
                adGroup);
        steps.bidModifierSteps().createAdGroupBidModifier(createDefaultBidModifierMobile(campaign.getCampaignId())
                        .withMobileAdjustment(createDefaultMobileAdjustment()
                                .withPercent(130)
                                .withOsType(OsType.ANDROID)),
                adGroup);

        Target expectedTarget = new Target()
                .withAdGroupId(adGroup.getAdGroupId())
                .withExcludedDomains(singleton("ya.ru"))
                .withRegions(singleton(100))
                .withBlockSizes(singletonList(new BlockSize(1920, 1080)))
                .withCryptaGroups(singletonList(new CryptaGroup(singleton("123:456"))))
                .withAudienceGroups(singletonList(new AudienceGroup(AudienceGroup.GroupingType.ANY,
                        singleton(METRIKA_AUDIENCE_UPPER_BOUND - 2 + ""))))
                .withPlatformCorrections(PlatformCorrections.builder().withDesktop(120).withMobile(130)
                        .withMobileOsType(MobileOsType.ANDROID).build())
                .withGroupType(GroupType.BANNER)
                .withTargetTags(emptyList())
                .withOrderTags(emptyList())
                .withCorrections(new CampaignParametersCorrections(DEFAULT_TRAFFIC_TYPE_CORRECTIONS));

        Target actualTarget = collectCampaignInfo(true, campaign.getCampaignId()).get(0);

        assertThat(actualTarget).is(matchedBy(beanDiffer(expectedTarget)));
    }

    @Test
    public void collectCampaignInfo_IndoorAdGroup_IndoorTarget() {
        CampaignInfo campaign = steps.campaignSteps().createActiveCpmBannerCampaign();
        ClientInfo clientInfo = campaign.getClientInfo();
        ClientId clientId = campaign.getClientId();
        IndoorPlacement placement = steps.placementSteps().addDefaultIndoorPlacementWithOneBlock();
        var pageId = placement.getBlocks().get(0).getPageId();
        var blockId = placement.getBlocks().get(0).getBlockId();
        AdGroupInfo adGroup = steps.adGroupSteps()
                .createAdGroup(activeCpmIndoorAdGroup(null, placement), campaign);
        CreativeInfo creative = steps.creativeSteps()
                .createCreative(defaultCpmIndoorVideoAddition(clientId, null)
                                .withAdditionalData(new AdditionalData()
                                        .withDuration(BigDecimal.valueOf(1.5))
                                        .withFormats(singletonList(
                                                new VideoFormat()
                                                        .withWidth(1248)
                                                        .withHeight(416)
                                                        .withType("video/mp4")
                                                        .withUrl("http://abc.com/1")))),
                        clientInfo);
        steps.bannerSteps().createActiveCpmIndoorBanner(
                activeCpmIndoorBanner(campaign.getCampaignId(), adGroup.getAdGroupId(), creative.getCreativeId()),
                adGroup);
        steps.bidModifierSteps().createAdGroupBidModifier(createDefaultBidModifierDemographics(campaign.getCampaignId())
                        .withDemographicsAdjustments(singletonList(
                                createDefaultDemographicsAdjustment()
                                        .withGender(GenderType.MALE)
                                        .withAge(AgeType._25_34)
                                        .withPercent(110))),
                adGroup);

        Target expectedTarget = new Target()
                .withAdGroupId(adGroup.getAdGroupId())
                .withGroupType(GroupType.INDOOR)
                .withVideoCreatives(singletonList(new VideoCreative(1500,
                        null, singleton(new BlockSize(3, 1)))))
                .withPageBlocks(singletonList(new PageBlock(pageId, singletonList(blockId))))
                .withProfileCorrections(singletonList(ProfileCorrection.builder()
                        .withGender(ProfileCorrection.Gender.MALE)
                        .withAge(ProfileCorrection.Age._25_34)
                        .withCorrection(110)
                        .build()));
        Target actualTarget = collectCampaignInfo(true, campaign.getCampaignId()).get(0);

        assertThat(actualTarget).is(matchedBy(beanDiffer(expectedTarget)));
    }

    @Test
    public void collectCampaignInfo_CpmBannerAdGroup_CreativeStatusModerateNo() {
        CampaignInfo campaign = steps.campaignSteps().createCampaign(activeCpmBannerCampaign(null, null)
                .withDisabledDomains(singleton("ya.ru")));
        ClientInfo clientInfo = campaign.getClientInfo();
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(activeCpmBannerAdGroup(null)
                        .withGeo(singletonList(100L)),
                campaign);
        createRetargetingCondition(adGroup, RuleType.OR, asList(
                defaultAudience(METRIKA_AUDIENCE_UPPER_BOUND - 2),
                (Goal) defaultGoalByType(GoalType.SOCIAL_DEMO).withKeyword("123").withKeywordValue("456")));
        CreativeInfo creative = steps.creativeSteps().createCreative(fullCreative(null, null)
                        .withWidth(1920L)
                        .withHeight(1080L)
                        .withStatusModerate(StatusModerate.NO),
                clientInfo);
        steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(campaign.getCampaignId(), adGroup.getAdGroupId(), creative.getCreativeId()),
                adGroup);
        steps.bidModifierSteps().createAdGroupBidModifier(createDefaultBidModifierDesktop(campaign.getCampaignId())
                        .withDesktopAdjustment(createDefaultDesktopAdjustment().withPercent(120)),
                adGroup);
        steps.bidModifierSteps().createAdGroupBidModifier(createDefaultBidModifierMobile(campaign.getCampaignId())
                        .withMobileAdjustment(createDefaultMobileAdjustment()
                                .withPercent(130)
                                .withOsType(OsType.ANDROID)),
                adGroup);

        List<Target> actualTargets = collectCampaignInfo(true, campaign.getCampaignId());

        assertThat(actualTargets).isEmpty();
    }

@Test
public void collectCampaignInfo_CpmBannerAdGroup_BannerCreativeStatusModerateNo() {
    CampaignInfo campaign = steps.campaignSteps().createCampaign(activeCpmBannerCampaign(null, null)
            .withDisabledDomains(singleton("ya.ru")));
    ClientInfo clientInfo = campaign.getClientInfo();
    AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(activeCpmBannerAdGroup(null)
                    .withGeo(singletonList(100L)),
            campaign);
    createRetargetingCondition(adGroup, RuleType.OR, asList(
            defaultAudience(METRIKA_AUDIENCE_UPPER_BOUND - 2),
            (Goal) defaultGoalByType(GoalType.SOCIAL_DEMO).withKeyword("123").withKeywordValue("456")));
    CreativeInfo creative = steps.creativeSteps().createCreative(fullCreative(null, null)
                    .withWidth(1920L)
                    .withHeight(1080L),
            clientInfo);
    steps.bannerSteps().createActiveCpmBanner(
            activeCpmBanner(campaign.getCampaignId(), adGroup.getAdGroupId(), creative.getCreativeId())
                    .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NO),
            adGroup);
    steps.bidModifierSteps().createAdGroupBidModifier(createDefaultBidModifierDesktop(campaign.getCampaignId())
                    .withDesktopAdjustment(createDefaultDesktopAdjustment().withPercent(120)),
            adGroup);
    steps.bidModifierSteps().createAdGroupBidModifier(createDefaultBidModifierMobile(campaign.getCampaignId())
                    .withMobileAdjustment(createDefaultMobileAdjustment()
                            .withPercent(130)
                            .withOsType(OsType.ANDROID)),
            adGroup);

    List<Target> actualTargets = collectCampaignInfo(true, campaign.getCampaignId());

    assertThat(actualTargets).isEmpty();
}

    private RetargetingCondition createRetargetingCondition(AdGroupInfo adGroup, RuleType ruleType, List<Goal> goals) {
        RetConditionInfo retConditionInfo = steps.retConditionSteps().createDefaultRetCondition(goals,
                adGroup.getClientInfo(), ConditionType.interests, ruleType);
        steps.retargetingSteps().createRetargeting(defaultTargetInterest(), adGroup, retConditionInfo);
        testCryptaSegmentRepository.addAll(filterCryptaGoals(retConditionInfo.getRetCondition().collectGoals()));

        return retConditionInfo.getRetCondition();
    }


    /**
     * Функция, возвращающая искомые данные по id кампании. Находит clientId по campaignId и вызывает
     * collectCampaignInfo(clientId, campaignId)
     *
     * @return Список таргетов в разбивке по группам
     */
    private List<Target> collectCampaignInfo(boolean fromIntapi, Long campaignId) {
        int shard = shardHelper.getShardByCampaignId(campaignId);
        Campaign campaign = campaignRepository.getCampaigns(shard, singletonList(campaignId)).get(0);
        ClientId clientId = ClientId.fromLong(campaign.getClientId());
        Long uid = campaign.getUserId();

        return StreamEx.of(collector.collectCampaignsInfoWithClientIdAndUid(null, fromIntapi, shard, uid, clientId,
                                singletonList(campaignId), true, null)
                        .get(campaignId)
                        .getLeft())
                .filter(Target::hasCreatives)
                .toList();
    }
}
