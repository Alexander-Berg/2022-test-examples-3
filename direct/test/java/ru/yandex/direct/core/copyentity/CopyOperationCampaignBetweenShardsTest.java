package ru.yandex.direct.core.copyentity;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBidModifiers;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.service.BaseCampaignService;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestKeywords;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ImageHashBannerInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.core.testing.data.TestBanners.regularImageFormat;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierGeo;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultGeoAdjustment;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("unchecked")
public class CopyOperationCampaignBetweenShardsTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CopyOperationFactory factory;

    @Autowired
    private BaseCampaignService baseCampaignService;

    @Autowired
    private CampaignModifyRepository campaignRepository;

    @Autowired
    private DslContextProvider contextProvider;

    @Autowired
    private CopyOperationAssert asserts;

    @Autowired
    private NetAcl netAcl;

    private Long uidSuper;
    private ClientId clientIdFrom;
    private ClientInfo clientInfoFrom;
    private ClientId clientIdTo;
    private ClientInfo clientInfoTo;

    private Long campaignId;
    private CampaignInfo campaignInfo;
    private CopyOperation xerox;

    @Before
    public void setUp() {
        doReturn(false).when(netAcl).isInternalIp(any(InetAddress.class));
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uidSuper = superClientInfo.getUid();
        steps.featureSteps().setCurrentClient(superClientInfo.getClientId());

        clientInfoFrom = steps.clientSteps().createDefaultClient();
        clientIdFrom = clientInfoFrom.getClientId();

        clientInfoTo = steps.clientSteps().createDefaultClientAnotherShard();
        clientIdTo = clientInfoTo.getClientId();

        campaignInfo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientIdFrom, clientInfoFrom.getUid()).withEmail("test@yandex-team.ru")
                        .withStartTime(LocalDate.now().plusDays(1L)),
                clientInfoFrom);
        campaignId = campaignInfo.getCampaignId();

        asserts.init(clientIdFrom, clientIdTo, this.uidSuper);

        xerox = factory.build(clientInfoFrom.getShard(), clientInfoFrom.getClient(),
                clientInfoTo.getShard(), clientInfoTo.getClient(),
                this.uidSuper,
                BaseCampaign.class, List.of(campaignId),
                new CopyCampaignFlags.Builder()
                        .withCopyNotificationSettings(true)
                        .build());
    }

    @After
    public void after() {
        reset(netAcl);
    }

    @Test
    public void campaignIsCopied() {
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        Set<Long> copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        asserts.assertCampaignIsCopied(copiedCampaignIds, campaignInfo.getCampaignId());
    }

    @Test
    public void campaignWithOneAdGroup() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());

        asserts.assertCampaignIsCopied(copiedCampaignIds, campaignInfo.getCampaignId());
        asserts.assertEntitiesAreCopied(AdGroup.class, copiedAdGroupIds, List.of(adGroupInfo.getAdGroup()), COPIED);
    }

    @Test
    public void campaignWithSeveralAdGroups() {
        var adGroupInfo1 = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withName("adGroup1"), campaignInfo);
        var adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withName("adGroup2"), campaignInfo);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        asserts.assertEntitiesAreCopied(AdGroup.class, copiedAdGroupIds,
                List.of(adGroupInfo1.getAdGroup(), adGroupInfo2.getAdGroup()),
                COPIED);
    }

    @Test
    public void campaignWithOneBanner() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var bannerInfo = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner().withHref("http://banners.com/1")
                                .withDomain("banners.com"))
                        .withAdGroupInfo(adGroupInfo));

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedBannerIds = Set.copyOf(copyResult.getEntityMapping(BannerWithAdGroupId.class).values());
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo.getBanner()), COPIED);
    }

    @Test
    public void campaignWithBranchingTreeOfEntities() {
        var adGroupInfo1 = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withName("adGroup1"), campaignInfo);
        var adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withName("adGroup2"), campaignInfo);
        var bannerInfo1 = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner()
                                .withHref("http://banners.com/1")
                                .withDomain("banners.com"))
                        .withAdGroupInfo(adGroupInfo1));
        var bannerInfo2 = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner().withHref("http://banners.com/2")
                                .withDomain("banners.com"))
                        .withAdGroupInfo(adGroupInfo1));
        var bannerInfo3 = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner().withHref("http://banners.com/3")
                                .withDomain("banners.com"))
                        .withAdGroupInfo(adGroupInfo2));
        var bannerInfo4 = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner().withHref("http://banners.com/4")
                                .withDomain("banners.com"))
                        .withAdGroupInfo(adGroupInfo2));

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedBannerIds = Set.copyOf(copyResult.getEntityMapping(BannerWithAdGroupId.class).values());
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo1.getBanner(), bannerInfo2.getBanner(),
                        bannerInfo3.getBanner(), bannerInfo4.getBanner()),
                COPIED);
    }

    @Test
    public void campaignWithOneVcard() {
        var vcardInfo = steps.vcardSteps().createVcard(fullVcard().withCompanyName("Vcard1 Ltd"), campaignInfo);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedVcardIds = Set.copyOf(copyResult.getEntityMapping(Vcard.class).values());
        asserts.assertEntitiesAreCopied(Vcard.class, copiedVcardIds, List.of(vcardInfo.getVcard()), COPIED);
    }

    @Test
    public void campaignWithOneSitelinkSet() {
        var sitelinkSetInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfoFrom);
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var bannerInfo = steps.bannerSteps().createBanner(
                activeTextBanner().withHref("http://banners.com/1")
                        .withDomain("banners.com").withReverseDomain("moc.srennab")
                        .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId()),
                adGroupInfo);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedSitelinkSetIds = StreamEx.of(copyResult.getEntityContext()
                .getEntities(BannerWithAdGroupId.class).values())
                .select(TextBanner.class)
                .map(TextBanner::getSitelinksSetId)
                .toSet();
        asserts.assertEntitiesAreCopied(SitelinkSet.class,
                copiedSitelinkSetIds, List.of(sitelinkSetInfo.getSitelinkSet()), COPIED);
    }

    @Test
    public void campaignWithOneBidModifier() {
        var bidModifierInfo = steps.bidModifierSteps().createCampaignBidModifier(
                createDefaultBidModifierGeo(campaignId).withRegionalAdjustments(
                        List.of(createDefaultGeoAdjustment().withPercent(142).withRegionId(Region.MOSCOW_REGION_ID))),
                campaignInfo);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        List<BaseCampaign> copiedCampaigns = baseCampaignService.get(clientIdTo, uidSuper, copiedCampaignIds);
        var copiedBidModifierIds = StreamEx.of(copiedCampaigns)
                .select(CampaignWithBidModifiers.class)
                .flatMap(cdb -> cdb.getBidModifiers().stream())
                .map(BidModifier::getId)
                .toSet();
        asserts.assertCampaignBidModifierInfosAreCopied(copiedBidModifierIds, List.of(bidModifierInfo));
    }

    @Test
    public void campaignWithOneRetargetingCondition_notCopiedDueToPrevalidation() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var retCondInfo = steps.retConditionSteps().createDefaultRetCondition(clientInfoFrom);
        var retInfo = steps.retargetingSteps().createRetargeting(
                defaultRetargeting().withPriceContext(BigDecimal.valueOf(42)), adGroupInfo, retCondInfo);

        var copyResult = xerox.copy();
        assertThat(copyResult.getMassResult().getValidationResult().flattenErrors()).isNotEmpty();
    }

    @Test
    public void campaignWithBannerImage() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        var bannerInfo = steps.bannerSteps().createBanner(
                activeTextBanner(campaignId, adGroupIdFrom)
                        .withDomain("yandex.ru").withReverseDomain("ur.xednay"),
                adGroupInfo);
        var bannerImageFormat = steps.bannerSteps().createBannerImageFormat(clientInfoFrom,
                regularImageFormat("inter-shard"));
        var bannerImageInfo = steps.bannerSteps().createBannerImage(bannerInfo, bannerImageFormat,
                defaultBannerImage(bannerInfo.getBannerId(), bannerImageFormat.getImageHash()));

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedBannerIds = Set.copyOf(copyResult.getEntityMapping(BannerWithAdGroupId.class).values());
        asserts.assertBannerImagePoolsAreCopied(copiedBannerIds, List.of(bannerImageFormat.getImageHash()),
                false);
    }

    @Test
    public void campaignWithImage() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        ImageHashBannerInfo bannerInfo = steps.bannerSteps().createActiveImageHashBanner(
                activeImageHashBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withDomain("yandex.ru").withReverseDomain("ur.xednay"),
                adGroupInfo);
        var image = steps.bannerSteps().createImage(bannerInfo);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedBannerIds = Set.copyOf(copyResult.getEntityMapping(BannerWithAdGroupId.class).values());
        asserts.assertBannerImagePoolsAreCopied(copiedBannerIds, List.of(image.getImage().getImageHash()),
                false);
    }

    @Test
    public void campaignWithBroadMatch() {
        var campaign = defaultTextCampaignWithSystemFields()
                .withClientId(clientInfoFrom.getClientId().asLong())
                .withUid(clientInfoFrom.getUid())
                .withDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY)
                .withStatusShow(true);
        campaign.getBroadMatch().setBroadMatchGoalId(1L);

        int shard = clientInfoFrom.getShard();
        RestrictedCampaignsAddOperationContainer paramContainer = RestrictedCampaignsAddOperationContainer.create(shard,
                uidSuper, clientIdFrom, clientInfoFrom.getUid(), clientInfoFrom.getUid());
        campaignRepository.addCampaigns(contextProvider.ppc(shard), paramContainer, List.of(campaign));

        var xerox = factory.build(clientInfoFrom.getShard(), clientInfoFrom.getClient(),
                clientInfoTo.getShard(), clientInfoTo.getClient(),
                uidSuper,
                BaseCampaign.class, List.of(campaign.getId()),
                new CopyCampaignFlags());

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        Long copiedCampaignId = copyResult.getEntityMappings(BaseCampaign.class).values().stream()
                .findFirst().get();

        TextCampaign copiedCampaign = (TextCampaign)
                baseCampaignService.get(clientIdTo, uidSuper, List.of(copiedCampaignId)).get(0);

        // broadMatchGoalId должен сбрасываться в CampaignWithBroadMatchCopyPreprocessor
        assertThat(copiedCampaign.getBroadMatch().getBroadMatchGoalId()).isEqualTo(0);
    }

    @Test
    public void campaignWithKeyword() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);

        var keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo, TestKeywords.defaultKeyword());

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedKeywordIds = Set.copyOf(copyResult.getEntityMapping(Keyword.class).values());
        asserts.assertEntitiesAreCopied(Keyword.class, copiedKeywordIds, List.of(keywordInfo.getKeyword()), COPIED);
    }

    @Test
    public void adGroupWithRelevanceMatch() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var relMatch = steps.relevanceMatchSteps().getDefaultRelevanceMatch(adGroupInfo);
        steps.relevanceMatchSteps()
                .addRelevanceMatchToAdGroup(List.of(relMatch), adGroupInfo);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedRelMatchIds = Set.copyOf(copyResult.getEntityMapping(RelevanceMatch.class).values());
        asserts.assertEntitiesAreCopied(RelevanceMatch.class, copiedRelMatchIds, List.of(relMatch), COPIED);
    }

}
