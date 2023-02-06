package ru.yandex.direct.core.copyentity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithKeywordsService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithRelevanceMatchesService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupsAddOperationTestBase;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.service.AdGroupWithRetargetingsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestNewCpcVideoBanners;
import ru.yandex.direct.core.testing.data.TestRetargetings;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ImageHashBannerInfo;
import ru.yandex.direct.core.testing.info.NewCpcVideoBannerInfo;
import ru.yandex.direct.core.testing.info.NewImageBannerInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.core.testing.data.TestBanners.regularImageFormat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageBidStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.clientTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("unchecked")
public class CopyOperationTextAdGroupSameClientDifferentCampaignTest extends AdGroupsAddOperationTestBase {

    @Autowired
    private CopyOperationFactory factory;

    @Autowired
    private CopyOperationAssert asserts;

    @Autowired
    private AdGroupWithBannersService adGroupWithBannersService;

    @Autowired
    private AdGroupWithKeywordsService adGroupWithKeywordsService;

    @Autowired
    private AdGroupWithRetargetingsService adGroupWithRetargetingsService;

    @Autowired
    private AdGroupWithRelevanceMatchesService adGroupWithRelevanceMatchesService;

    @Autowired
    private TestNewCpcVideoBanners testNewCpcVideoBanners;

    private Long uid;
    private ClientId clientId;
    private ClientInfo clientInfo;

    private Long campaignIdFrom;
    private CampaignInfo campaignInfoFrom;
    private CampaignInfo campaignInfoTo;

    private Long campaignIdTo;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();

        clientId = clientInfo.getClientId();

        campaignInfoFrom = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru")
                        .withStrategy(averageBidStrategy()),
                clientInfo);
        campaignIdFrom = campaignInfoFrom.getCampaignId();

        campaignInfoTo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru")
                        .withStrategy(manualStrategy()),
                clientInfo);

        campaignIdTo = campaignInfoTo.getCampaignId();

        asserts.init(clientId, clientId, uid);
    }

    @Test
    public void adGroupWithMinusKeywordsId() {
        var adGroup = clientTextAdGroup(campaignIdFrom).withMinusKeywords(List.of("полет", "навигатора"));

        MassResult<Long> result = createAddOperation(Applicability.FULL, List.of(adGroup), uid, clientId,
                geoTree, shard, true)
                .prepareAndApply();
        assertThat(result, isFullySuccessful());
        AdGroup addedAdGroup = actualAdGroup(result.get(0).getResult());

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, addedAdGroup.getId(), campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());

        asserts.assertEntitiesAreCopied(AdGroup.class, copiedAdGroupIds, List.of(addedAdGroup), COPIED);
    }

    @Test
    public void adGroupWithBannerWithDomain_DomainIsCopied() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        var bannerInfo = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner().withHref("http://google.ru/test")
                                .withDomain("yandex.ru"))
                        .withAdGroupInfo(adGroupInfo));

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        Set<Long> copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo.getBanner()), COPIED);
    }

    @Test
    public void adGroupWithTextImageAdWithImage() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        var bannerInfo = steps.imageBannerSteps().createImageBanner(
                new NewImageBannerInfo()
                        .withAdGroupInfo(adGroupInfo));

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo.getBanner()), COPIED);
    }

    @Test
    public void adGroupWithCpcVideoBannerWithVideo() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        Creative creative = defaultCpcVideoForCpcVideoBanner(clientInfo.getClientId(), null);
        var creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        var bannerInfo = steps.cpcVideoBannerSteps().createBanner(
                new NewCpcVideoBannerInfo()
                        .withBanner(testNewCpcVideoBanners.fullCpcVideoBanner(creativeInfo.getCreativeId())
                                .withDomain("yandex.ru"))
                        .withAdGroupInfo(adGroupInfo));

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo.getBanner()), COPIED);
    }

    @Test
    public void adGroupWithKeyword() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        var keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo, defaultKeyword());

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedKeywordIds = adGroupWithKeywordsService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(Keyword.class, copiedKeywordIds, List.of(keywordInfo.getKeyword()), COPIED);
    }

    @Test
    public void adGroupWithRetargeting() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();
        var retCondInfo = steps.retConditionSteps().createDefaultRetCondition(clientInfo);
        TargetInterest retargeting = TestRetargetings
                .defaultTargetInterest(
                        adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), retCondInfo.getRetConditionId());

        steps.retargetingSteps().addRetargeting(shard, retargeting);

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedRetargetingIds = adGroupWithRetargetingsService.getChildEntityIdsByParentIds(clientId, uid,
                copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(Retargeting.class,
                copiedRetargetingIds, List.of(retargeting),
                COPIED);
    }

    @Test
    public void adGroupWithRelevanceMatch() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();
        var relMatch = steps.relevanceMatchSteps().getDefaultRelevanceMatch(adGroupInfo);
        steps.relevanceMatchSteps()
                .addRelevanceMatchToAdGroup(List.of(relMatch), adGroupInfo);

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedRelMatchIds = adGroupWithRelevanceMatchesService.getChildEntityIdsByParentIds(clientId, uid,
                copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(RelevanceMatch.class, copiedRelMatchIds, List.of(relMatch), COPIED);
    }

    @Test
    public void campaignWithTooManyAdGroups() {
        final int adGroupsToCopy = 510;
        final int adGroupsAlreadyInCampaign = 540;
        final int maxAdGroupsInCampaign = 1000;

        List<AdGroupInfo> adGroupInfosFrom = IntStreamEx.range(0, adGroupsToCopy)
                .mapToObj(i -> new AdGroupInfo()
                        .withAdGroup(activeTextAdGroup().withName("adGroup " + i))
                        .withCampaignInfo(campaignInfoFrom))
                .toList();
        steps.adGroupSteps().createAdGroups(adGroupInfosFrom);

        List<AdGroupInfo> adGroupInfosTo = IntStreamEx.range(0, adGroupsAlreadyInCampaign)
                .mapToObj(i -> new AdGroupInfo()
                        .withAdGroup(activeTextAdGroup().withName("adGroup " + i))
                        .withCampaignInfo(campaignInfoTo))
                .toList();

        steps.adGroupSteps().createAdGroups(adGroupInfosTo);

        List<Long> adGroupIds = StreamEx.of(adGroupInfosFrom).map(AdGroupInfo::getAdGroupId).toList();
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIds, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();

        final int adGroupsThatDoesntFitInCampaign = adGroupsAlreadyInCampaign + adGroupsToCopy
                - maxAdGroupsInCampaign;
        assertThat(copyResult.getMassResult().getValidationResult().flattenErrors().size(),
                is(adGroupsThatDoesntFitInCampaign));
    }

    @Test
    public void adGroupConnectedToAnotherAdGroupThroughVcard_onlyCurrentAdGroupIsCopied() {
        var vcardInfo = steps.vcardSteps().createVcard(campaignInfoFrom);

        var adGroupInfoCopied = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withName("Should be copied"),
                campaignInfoFrom);
        var adGroupIdCopied = adGroupInfoCopied.getAdGroupId();

        var bannerInfoCopied = steps.bannerSteps().createBanner(
                activeTextBanner().withVcardId(vcardInfo.getVcardId()),
                adGroupInfoCopied);

        var adGroupInfoNotCopied = steps.adGroupSteps().createAdGroup(
                activeTextAdGroup().withName("Should not be copied"), campaignInfoFrom);
        var bannerInfoNotCopied = steps.bannerSteps().createBanner(
                activeTextBanner().withVcardId(vcardInfo.getVcardId()),
                adGroupInfoNotCopied);

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdCopied, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        assertThat(copiedAdGroupIds.size(), is(1));

        asserts.assertEntitiesAreCopied(AdGroup.class,
                Set.copyOf(copiedAdGroupIds), List.of(adGroupInfoCopied.getAdGroup()),
                COPIED);
    }

    @Test
    public void adGroupWithVcardCopiedToADifferentCampaignWithTheSameVcard_bannerIsCopied() {
        var vcardInfoFrom = steps.vcardSteps().createVcard(campaignInfoFrom);

        var adGroupInfoFrom = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withName("Should be copied"),
                campaignInfoFrom);
        var adGroupIdToCopy = adGroupInfoFrom.getAdGroupId();

        NewTextBannerInfo bannerInfo = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner()
                                .withHref("http://google.ru/test")
                                .withDomain("yandex.ru")
                                .withVcardId(vcardInfoFrom.getVcardId()))
                        .withAdGroupInfo(adGroupInfoFrom));

        var vcardInfoTo = steps.vcardSteps().createVcard(campaignInfoTo);

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdToCopy, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        Set<Long> copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(((TextBanner) bannerInfo.getBanner()).withVcardId(vcardInfoTo.getVcardId())),
                COPIED);
    }

    @Test
    public void bsRarelyLoadedGroup_bsRarelyLoadedStatusIsNotCopied() {
        var adGroupInfoFrom = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withBsRarelyLoaded(true),
                campaignInfoFrom);
        var adGroupIdFrom = adGroupInfoFrom.getAdGroupId();
        steps.adGroupSteps().setBsRarelyLoaded(shard, adGroupIdFrom, true);
        assertThat(actualAdGroup(adGroupIdFrom).getBsRarelyLoaded(), is(true));

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var adGroup = actualAdGroup(copiedAdGroupIds.get(0));
        assertThat(adGroup.getBsRarelyLoaded(), is(false));
    }

    @Test
    public void bannerWithStatusModerateReady() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        var bannerInfo = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner().withDomain("yandex.ru")
                                .withStatusModerate(BannerStatusModerate.READY))
                        .withAdGroupInfo(adGroupInfo));

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo.getBanner()), COPIED);
    }

    @Test
    public void copyGroupWithRelevanceMatchFromNonAutoBudgetCampaignToAutoBudgetCampaign() {
        campaignInfoFrom = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru")
                        .withStrategy(manualStrategy()),
                clientInfo);
        campaignIdFrom = campaignInfoFrom.getCampaignId();

        campaignInfoTo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru")
                        .withStrategy(averageBidStrategy()),
                clientInfo);

        Long campaignIdTo = campaignInfoTo.getCampaignId();

        asserts.init(clientId, clientId, uid);

        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();
        var relMatch = steps.relevanceMatchSteps().getDefaultRelevanceMatch(adGroupInfo)
                .withAutobudgetPriority(null);
        steps.relevanceMatchSteps()
                .addRelevanceMatchToAdGroup(List.of(relMatch), adGroupInfo);

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedRelMatchIds = adGroupWithRelevanceMatchesService.getChildEntityIdsByParentIds(clientId, uid,
                copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(RelevanceMatch.class,
                copiedRelMatchIds, List.of(relMatch.withAutobudgetPriority(3)),
                COPIED);
    }

    @Test
    public void copyGroupWithKeywordFromNonAutoBudgetCampaignToAutoBudgetCampaign() {
        campaignInfoFrom = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru")
                        .withStrategy(manualStrategy()),
                clientInfo);
        campaignIdFrom = campaignInfoFrom.getCampaignId();

        campaignInfoTo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru")
                        .withStrategy(averageBidStrategy()),
                clientInfo);

        Long campaignIdTo = campaignInfoTo.getCampaignId();

        asserts.init(clientId, clientId, uid);

        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();
        var keyword = steps.keywordSteps().createKeyword(adGroupInfo, defaultKeyword().withAutobudgetPriority(null));

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedRelMatchIds = adGroupWithKeywordsService.getChildEntityIdsByParentIds(clientId, uid,
                copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(Keyword.class,
                copiedRelMatchIds, List.of(keyword.getKeyword().withAutobudgetPriority(3)),
                COPIED);
    }

    @Test
    public void adGroupWithLastChange() {
        LocalDateTime previousDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup()
                        .withLastChange(previousDate),
                campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(clientInfo, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var adGroup = actualAdGroup(copiedAdGroupIds.get(0));
        assertThat(adGroup.getLastChange(), is(not(equalTo(previousDate))));
    }

    @Test
    public void bannerWithBannerImage() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        var bannerInfo = steps.bannerSteps().createBanner(
                activeTextBanner(campaignIdFrom, adGroupIdFrom)
                        .withDomain("yandex.ru").withReverseDomain("ur.xednay"),
                adGroupInfo);
        var bannerImageFormat = steps.bannerSteps().createBannerImageFormat(clientInfo,
                regularImageFormat("different-campaign"));
        var bannerImageInfo = steps.bannerSteps().createBannerImage(bannerInfo, bannerImageFormat,
                defaultBannerImage(bannerInfo.getBannerId(), bannerImageFormat.getImageHash()));

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(clientInfo, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertBannerImagePoolsAreCopied(copiedBannerIds, List.of(bannerImageFormat.getImageHash()),
                true);
    }

    @Test
    public void bannerWithImage() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        ImageHashBannerInfo bannerInfo = steps.bannerSteps().createActiveImageHashBanner(
                activeImageHashBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withDomain("yandex.ru").withReverseDomain("ur.xednay"),
                adGroupInfo);
        var image = steps.bannerSteps().createImage(bannerInfo);

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(clientInfo, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertBannerImagePoolsAreCopied(copiedBannerIds, List.of(image.getImage().getImageHash()),
                true);
    }

    private AdGroup actualAdGroup(Long adGroupId) {
        return adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(adGroupId)).get(0);
    }
}
