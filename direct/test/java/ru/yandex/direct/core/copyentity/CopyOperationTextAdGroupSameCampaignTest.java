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
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.retargeting.service.AdGroupWithRetargetingsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestKeywords;
import ru.yandex.direct.core.testing.data.TestNewCpcVideoBanners;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ImageHashBannerInfo;
import ru.yandex.direct.core.testing.info.NewCpcVideoBannerInfo;
import ru.yandex.direct.core.testing.info.NewImageBannerInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupStatusArchived;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.core.testing.data.TestBanners.regularImageFormat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.clientTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("ALL")
public class CopyOperationTextAdGroupSameCampaignTest extends AdGroupsAddOperationTestBase {
    private final int maxAdGroupsInCampaign = 1000;

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

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();

        clientId = clientInfo.getClientId();
        steps.featureSteps().addClientFeature(clientId, FeatureName.TEXT_BANNER_INTERESTS_RET_COND_ENABLED, true);

        campaignInfoFrom = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru"),
                clientInfo);
        campaignIdFrom = campaignInfoFrom.getCampaignId();

        asserts.init(clientId, clientId, uid);
    }

    @Test
    public void adGroupWithLongName() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withName(randomAlphabetic(255)),
                campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var adGroup = actualAdGroup(copiedAdGroupIds.get(0));
        assertThat(adGroup.getName(), containsString(adGroupIdFrom.toString()));
    }

    @Test
    public void adGroupWithArchivedBanner() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        steps.bannerSteps().createDefaultArchivedBanner(adGroupInfo);

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);

        CopyResult<Long> copyResult = xerox.copy();
        assertThat(copyResult.getMassResult().getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0)), adGroupStatusArchived())));

        var copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        // архивные группы отфильтровываем
        assertThat(copiedAdGroupIds, empty());
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
                clientInfo, addedAdGroup.getId(), campaignIdFrom, uid);
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
                clientInfo, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        Set<Long> copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo.getBanner()), COPIED);
    }

    @Test
    public void adGroupWithTextImageAdWithImage_bannerWithImageIsCopied() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        var bannerInfo = steps.imageBannerSteps().createImageBanner(
                new NewImageBannerInfo()
                        .withAdGroupInfo(adGroupInfo));

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, uid);
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
                        .withBanner(testNewCpcVideoBanners.fullCpcVideoBanner(creativeInfo.getCreativeId()))
                        .withAdGroupInfo(adGroupInfo));

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, uid);
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

        var keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo, TestKeywords.defaultKeyword());

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedKeywordIds = adGroupWithKeywordsService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(Keyword.class, copiedKeywordIds, List.of(keywordInfo.getKeyword()), COPIED);
    }

    @Test
    public void adGroupWithSuspendedKeyword() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();
        var keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo,
                TestKeywords.defaultKeyword().withIsSuspended(true));
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);
        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedKeywordIds = adGroupWithKeywordsService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(Keyword.class, copiedKeywordIds, List.of(keywordInfo.getKeyword()), COPIED);
    }

    @Test
    public void adGroupWithRelevanceMatch() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();
        var relMatch = steps.relevanceMatchSteps().getDefaultRelevanceMatch(adGroupInfo);
        steps.relevanceMatchSteps()
                .addRelevanceMatchToAdGroup(List.of(relMatch), adGroupInfo);

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(clientInfo, adGroupIdFrom, campaignIdFrom, uid);
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
        final int adGroupsAlreadyInCampaign = 510;
        final int adGroupsToCopy = 500;

        var campaignInfo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru"),
                clientInfo);
        var campaignId = campaignInfo.getCampaignId();

        List<AdGroupInfo> adGroupInfos = IntStreamEx.range(0, adGroupsAlreadyInCampaign)
                .mapToObj(i -> new AdGroupInfo()
                        .withAdGroup(activeTextAdGroup().withName("adGroup " + i))
                        .withCampaignInfo(campaignInfo))
                .toList();

        steps.adGroupSteps().createAdGroups(adGroupInfos);
        List<Long> adGroupIds =
                StreamEx.of(adGroupInfos).map(AdGroupInfo::getAdGroupId).limit(adGroupsToCopy).toList();

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(clientInfo, adGroupIds, campaignId, uid);
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

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(clientInfo, adGroupIdCopied, campaignIdFrom, uid);
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
    public void bsRarelyLoadedGroup_bsRarelyLoadedStatusIsNotCopied() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withBsRarelyLoaded(true),
                campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();
        steps.adGroupSteps().setBsRarelyLoaded(shard, adGroupIdFrom, true);
        assertThat(actualAdGroup(adGroupIdFrom).getBsRarelyLoaded(), is(true));

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(clientInfo, adGroupIdFrom, campaignIdFrom, uid);
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
                        .withBanner(fullTextBanner(campaignIdFrom, adGroupIdFrom)
                                .withDomain("yandex.ru")
                                .withStatusModerate(BannerStatusModerate.READY))
                        .withAdGroupInfo(adGroupInfo));

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(clientInfo, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo.getBanner()), COPIED);
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
    public void bannerWithBannerImage_bannerImagePoolIsCopied() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        var bannerInfo = steps.bannerSteps().createBanner(
                activeTextBanner(campaignIdFrom, adGroupIdFrom)
                        .withDomain("yandex.ru").withReverseDomain("ur.xednay"),
                adGroupInfo);
        var bannerImageFormat = steps.bannerSteps().createBannerImageFormat(clientInfo,
                regularImageFormat("same-campaign"));
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
    public void bannerWithImage_bannerImagePoolIsCopied() {
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
