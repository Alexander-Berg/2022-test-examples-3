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
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags;
import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.BannerWithCallouts;
import ru.yandex.direct.core.entity.banner.model.BannerWithPhone;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBidModifiers;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPromoExtension;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.service.BaseCampaignService;
import ru.yandex.direct.core.entity.campaign.service.CampaignWithAdGroupsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignWithVcardsService;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.promoextension.model.PromoExtension;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ImageHashBannerInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefectIds.Gen.ORGANIZATION_NOT_FOUND;
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
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultOrganization;
import static ru.yandex.direct.core.testing.data.TestPromoExtensionsKt.defaultPromoExtension;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

@CoreTest
@RunWith(SpringRunner.class)
@SuppressWarnings("unchecked")
public class CopyOperationCampaignBetweenClientsTest {
    public static final String USER_EMAIL = "test@yandex-team.ru";
    @Autowired
    private Steps steps;

    @Autowired
    private CopyOperationFactory factory;

    @Autowired
    private CampaignWithAdGroupsService campaignToAdGroupService;

    @Autowired
    private AdGroupWithBannersService adGroupWithBannersService;

    @Autowired
    private CampaignWithVcardsService campaignWithVcardsService;

    @Autowired
    private BaseCampaignService baseCampaignService;

    @Autowired
    private BannerTypedRepository bannerRepository;

    @Autowired
    private CampaignModifyRepository campaignRepository;

    @Autowired
    private ClientPhoneRepository clientPhoneRepository;

    @Autowired
    OrganizationRepository organizationRepository;

    @Autowired
    private DslContextProvider contextProvider;

    @Autowired
    private CopyOperationAssert asserts;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    private NetAcl netAcl;

    private Long uid;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private ClientId clientIdTo;
    private ClientInfo clientInfoTo;

    private Long campaignId;
    private CampaignInfo campaignInfo;
    private CopyOperation xerox;

    @Before
    public void setUp() {
        doReturn(false).when(netAcl).isInternalIp(any(InetAddress.class));
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        steps.featureSteps().setCurrentClient(clientId);

        clientInfoTo = steps.clientSteps().createDefaultClient();
        clientIdTo = clientInfoTo.getClientId();

        campaignInfo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail(USER_EMAIL)
                        .withStartTime(LocalDate.now().plusDays(1L)),
                clientInfo);
        campaignId = campaignInfo.getCampaignId();

        asserts.init(clientId, clientIdTo, uid);

        xerox = buildCopyOperation(List.of(campaignId), true);
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
        var copiedAdGroupIds = campaignToAdGroupService.getChildEntityIdsByParentIds(clientId, uid, copiedCampaignIds);

        asserts.assertCampaignIsCopied(copiedCampaignIds, campaignInfo.getCampaignId());
        asserts.assertEntitiesAreCopied(AdGroup.class, copiedAdGroupIds, List.of(adGroupInfo.getAdGroup()), COPIED);
    }

    @Test
    public void campaignWithSeveralAdGroups() {
        var adGroupInfo1 = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withName("adGroup1"), campaignInfo);
        var adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withName("adGroup2"), campaignInfo);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        var copiedAdGroupIds = campaignToAdGroupService.getChildEntityIdsByParentIds(clientId, uid, copiedCampaignIds);
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

        var copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        var copiedAdGroupIds = campaignToAdGroupService.getChildEntityIdsByParentIds(clientId, uid, copiedCampaignIds);
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo.getBanner()), COPIED);
    }

    @Test
    public void campaignWithBranchingTreeOfEntities() {
        var adGroupInfo1 = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withName("adGroup1"), campaignInfo);
        var adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup().withName("adGroup2"), campaignInfo);
        var bannerInfo1 = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner().withHref("http://banners.com/1")
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

        var copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        var copiedAdGroupIds = campaignToAdGroupService.getChildEntityIdsByParentIds(clientId, uid, copiedCampaignIds);
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
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

        var copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        var copiedVcardIds = campaignWithVcardsService.getChildEntityIdsByParentIds(clientId, uid, copiedCampaignIds);
        asserts.assertEntitiesAreCopied(Vcard.class, copiedVcardIds, List.of(vcardInfo.getVcard()), COPIED);
    }

    @Test
    public void campaignWithOneSitelinkSet() {
        var sitelinkSetInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);
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
        List<BaseCampaign> copiedCampaigns = baseCampaignService.get(clientIdTo, uid, copiedCampaignIds);
        var copiedBidModifierIds = StreamEx.of(copiedCampaigns)
                .select(CampaignWithBidModifiers.class)
                .flatMap(cdb -> cdb.getBidModifiers().stream())
                .map(BidModifier::getId)
                .toSet();
        asserts.assertCampaignBidModifierInfosAreCopied(copiedBidModifierIds, List.of(bidModifierInfo));
    }

    @Test
    public void campaignWithBannerWithPhoneAndOrganization() {
        Organization org = defaultOrganization(clientId);
        steps.organizationSteps().createClientOrganization(clientId, org.getPermalinkId());
        organizationsClient.addUidsByPermalinkId(
                org.getPermalinkId(),
                List.of(clientInfo.getUid(), clientInfoTo.getUid())
        );

        ClientPhone clientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        Long phoneId = clientPhone.getId();

        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var bannerInfo = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner()
                                .withPermalinkId(org.getPermalinkId())
                                .withPhoneId(phoneId)
                                .withPreferVCardOverPermalink(false)
                        )
                        .withAdGroupInfo(adGroupInfo));

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);
        var targetClientPhones = clientPhoneRepository.getByClientId(clientIdTo);
        assertThat(targetClientPhones).isEmpty();

        var newBannerId = getNewBannerId(bannerInfo.getBanner().getId(), copyResult);

        var newBanner = getBanner(newBannerId, BannerWithPhone.class);
        assertThat(newBanner.getPhoneId()).isNull();
    }

    @Test
    public void campaignWithPhoneAndOrganization() {
        ClientPhone clientPhone = steps.clientPhoneSteps().addClientManualPhone(clientId, "+79161234567");
        Organization org = defaultOrganization(clientId);
        steps.organizationSteps().createClientOrganization(clientId, org.getPermalinkId());
        organizationsClient.addUidsByPermalinkId(
                org.getPermalinkId(),
                List.of(clientInfo.getUid(), clientInfoTo.getUid())
        );

        CampaignInfo campaignWithPhoneInfo = steps.textCampaignSteps().createCampaign(
                defaultTextCampaignWithSystemFields(clientInfo).withEmail(USER_EMAIL)
                        .withDefaultTrackingPhoneId(clientPhone.getId())
                        .withDefaultPermalinkId(org.getPermalinkId()));

        CopyOperation copyOperation = buildCopyOperation(List.of(campaignWithPhoneInfo.getCampaignId()), true);
        CopyResult<Long> copyResult = copyOperation.copy();
        asserts.checkErrors(copyResult);
        var targetClientPhones = clientPhoneRepository.getByClientId(clientIdTo);
        assertThat(targetClientPhones).isEmpty();

        long newCampaignId = (Long) copyResult.getEntityMapping(BaseCampaign.class)
                .get(campaignWithPhoneInfo.getCampaignId());

        var newCampaign = (TextCampaign) baseCampaignService.get(clientIdTo, uid, List.of(newCampaignId)).get(0);
        assertThat(newCampaign.getDefaultTrackingPhoneId()).isNull();
    }

    private <M extends Banner> M getBanner(Long newBannerId, Class<M> clazz) {
        return bannerRepository.getSafely(clientInfoTo.getShard(), singletonList(newBannerId), clazz).get(0);
    }


    private Long getNewBannerId(Long bannerId, CopyResult copyResult) {
        return (Long) copyResult.getEntityMapping(BannerWithAdGroupId.class).get(bannerId);
    }


    @Test
    public void campaignWithExistingOrganization() {
        Organization org = defaultOrganization(clientId);
        var permalinkId = org.getPermalinkId();

        organizationsClient.addUidsByPermalinkId(
                permalinkId,
                List.of(clientInfo.getUid(), clientInfoTo.getUid())
        );
        steps.organizationSteps().createClientOrganization(clientId, org.getPermalinkId());


        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var bannerInfo = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner()
                                .withPermalinkId(org.getPermalinkId())
                                .withPreferVCardOverPermalink(false)
                        )
                        .withAdGroupInfo(adGroupInfo));

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var newBannerId = getNewBannerId(bannerInfo.getBanner().getId(), copyResult);
        var targetClientOrganizations = organizationRepository.getOrganizationsByBannerIds(
                clientInfoTo.getShard(), singletonList(newBannerId));

        assertThat(targetClientOrganizations).isNotEmpty();
        assertThat(targetClientOrganizations.get(newBannerId).getPermalinkId()).isEqualTo(permalinkId);
    }

    @Test
    public void campaignWithNotExistingOrganization() {
        Organization org = defaultOrganization(clientId);
        var permalinkId = org.getPermalinkId();

        organizationsClient.removePermalinks(permalinkId);
        steps.organizationSteps().createClientOrganization(clientId, org.getPermalinkId());


        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var bannerInfo = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withBanner(fullTextBanner()
                                .withPermalinkId(org.getPermalinkId())
                                .withPreferVCardOverPermalink(false)
                        )
                        .withAdGroupInfo(adGroupInfo));

        var copyResult = xerox.copy();
        var newBannerId = getNewBannerId(bannerInfo.getBanner().getId(), copyResult);
        var targetClientOrganizations = organizationRepository.getOrganizationsByBannerIds(
                clientInfoTo.getShard(), singletonList(newBannerId));
        var vr = copyResult.getMassResult().getValidationResult();

        assertThat(targetClientOrganizations).isEmpty();
        assertThat(vr.flattenErrors()).isNotEmpty();
        assertThat(vr).is(matchedBy(hasDefectWithDefinition(
                validationError(ORGANIZATION_NOT_FOUND))));
    }


    @Test
    public void campaignWithOneRetargetingCondition_notCopiedDueToPrevalidation() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var retCondInfo = steps.retConditionSteps().createDefaultRetCondition(clientInfo);
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
        var bannerImageFormat = steps.bannerSteps().createBannerImageFormat(clientInfo,
                regularImageFormat("different-client"));
        var bannerImageInfo = steps.bannerSteps().createBannerImage(bannerInfo, bannerImageFormat,
                defaultBannerImage(bannerInfo.getBannerId(), bannerImageFormat.getImageHash()));

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertBannerImagePoolsAreCopied(copiedBannerIds, List.of(bannerImageFormat.getImageHash()),
                false);
    }

    @Test
    public void bannerWithImage() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var adGroupIdFrom = adGroupInfo.getAdGroupId();

        ImageHashBannerInfo bannerInfo = steps.bannerSteps().createActiveImageHashBanner(
                activeImageHashBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withDomain("yandex.ru").withReverseDomain("ur.xednay"),
                adGroupInfo);
        var image = steps.bannerSteps().createImage(bannerInfo);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertBannerImagePoolsAreCopied(copiedBannerIds, List.of(image.getImage().getImageHash()),
                false);
    }


    @Test
    public void campaignWithBroadMatch() {
        var campaign = defaultTextCampaignWithSystemFields()
                .withClientId(clientInfo.getClientId().asLong())
                .withUid(clientInfo.getUid())
                .withDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY)
                .withStatusShow(true);
        campaign.getBroadMatch().setBroadMatchGoalId(1L);

        int shard = clientInfo.getShard();
        RestrictedCampaignsAddOperationContainer paramContainer = RestrictedCampaignsAddOperationContainer.create(shard,
                uid, clientId, clientInfo.getUid(), clientInfo.getUid());
        campaignRepository.addCampaigns(contextProvider.ppc(shard), paramContainer, List.of(campaign));

        CopyOperation copyOperation = buildCopyOperation(List.of(campaign.getId()), true);

        var copyResult = copyOperation.copy();
        asserts.checkErrors(copyResult);

        Set<Long> copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        asserts.assertCampaignIsCopied(copiedCampaignIds, campaign.getId());
    }

    @Test
    public void campaignWithOneCallout() {
        var callout = steps.calloutSteps().createDefaultCallout(clientInfo);
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var bannerInfo = steps.bannerSteps().createBanner(
                activeTextBanner().withHref("http://banners.com/1")
                        .withDomain("banners.com").withReverseDomain("moc.srennab")
                        .withCalloutIds(List.of(callout.getId())),
                adGroupInfo);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var banners = copyResult.getEntityContext().getEntities(BannerWithAdGroupId.class).values();
        var copiedCalloutIds = StreamEx.of(banners).select(BannerWithCallouts.class)
                .flatMap(b -> b.getCalloutIds().stream()).toSet();
        asserts.assertEntitiesAreCopied(Callout.class, copiedCalloutIds, List.of(callout), COPIED);
    }

    @Test
    public void campaignWithTwoCallouts() {
        var callout1 = steps.calloutSteps().createDefaultCallout(clientInfo);
        var callout2 = steps.calloutSteps().createDefaultCallout(clientInfo);
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var bannerInfo = steps.bannerSteps().createBanner(
                activeTextBanner().withHref("http://banners.com/1")
                        .withDomain("banners.com").withReverseDomain("moc.srennab")
                        .withCalloutIds(List.of(callout1.getId(), callout2.getId())),
                adGroupInfo);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var banners = copyResult.getEntityContext().getEntities(BannerWithAdGroupId.class).values();
        var copiedCalloutIds = StreamEx.of(banners).select(BannerWithCallouts.class)
                .flatMap(b -> b.getCalloutIds().stream()).toSet();
        asserts.assertEntitiesAreCopied(Callout.class, copiedCalloutIds, List.of(callout1, callout2), COPIED);
    }

    @Test
    public void notificationSettingsErasedWhenCopyNotificationSettingsFlagIsFalse() {
        xerox = buildCopyOperation(List.of(campaignId), false);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);
        var newCampaign = (CommonCampaign) baseCampaignService.get(clientIdTo, uid,
                singletonList((Long) copyResult.getEntityMapping(BaseCampaign.class).get(campaignId))).get(0);

        var newEmail = newCampaign.getEmail();
        assertThat(newEmail)
                .isNotEqualTo(USER_EMAIL)
                .isEqualTo(clientInfoTo.getChiefUserInfo().getUser().getEmail());
    }

    @Test
    public void notificationSettingsCopiedWhenCopyNotificationSettingsFlagIsTrue() {
        xerox = buildCopyOperation(List.of(campaignId), true);

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);
        var newCampany = (CommonCampaign) baseCampaignService.get(clientIdTo, uid,
                singletonList((Long) copyResult.getEntityMapping(BaseCampaign.class).get(campaignId))).get(0);

        var newEmail = newCampany.getEmail();

        assertThat(newEmail).isEqualTo(USER_EMAIL);
    }

    @Test
    public void campaignWithPromoExtension() {
        var promoExtension = defaultPromoExtension(clientInfo.getClientId());
        promoExtension.setStatusModerate(PromoactionsStatusmoderate.Yes);
        var promoExtensionInfo = steps.promoExtensionSteps().createPromoExtension(clientInfo, promoExtension);

        var campaignWithPromoExtensionInfo = steps.textCampaignSteps()
                .createCampaign(defaultTextCampaignWithSystemFields(clientInfo)
                        .withPromoExtensionId(promoExtensionInfo.getPromoExtensionId()));

        CopyOperation copyOperation = buildCopyOperation(List.of(campaignWithPromoExtensionInfo.getCampaignId()), true);

        var copyResult = copyOperation.copy();
        asserts.checkErrors(copyResult);

        var copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        asserts.assertCampaignIsCopied(copiedCampaignIds, campaignWithPromoExtensionInfo.getCampaignId());

        var copiedPromoExtensionIds = getCopiedPromoExtensionIds(copyResult);
        asserts.assertEntitiesAreCopied(PromoExtension.class,
                copiedPromoExtensionIds, List.of(promoExtensionInfo.getPromoExtension()), COPIED);

        var copiedPromoExtensions =
                StreamEx.of(copyResult.getEntityContext().getEntities(PromoExtension.class).values())
                        .select(PromoExtension.class).toList();
        assertThat(copiedPromoExtensions).allMatch(p -> p.getStatusModerate() == PromoactionsStatusmoderate.Ready);
    }

    @Test
    public void twoCampaignsWithSamePromoExtension() {
        var promoExtension = defaultPromoExtension(clientInfo.getClientId());
        promoExtension.setStatusModerate(PromoactionsStatusmoderate.Yes);
        var promoExtensionInfo = steps.promoExtensionSteps().createPromoExtension(clientInfo, promoExtension);

        var campaignInfo1 = steps.textCampaignSteps()
                .createCampaign(defaultTextCampaignWithSystemFields(clientInfo)
                        .withPromoExtensionId(promoExtensionInfo.getPromoExtensionId()));
        var campaignInfo2 = steps.textCampaignSteps()
                .createCampaign(defaultTextCampaignWithSystemFields(clientInfo)
                        .withPromoExtensionId(promoExtensionInfo.getPromoExtensionId()));

        CopyOperation copyOperation = buildCopyOperation(List.of(campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId()), true);

        var copyResult = copyOperation.copy();

        var copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        asserts.assertCampaignsAreCopied(copiedCampaignIds,
                Set.of(campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId()));

        var copiedPromoExtensionIds = getCopiedPromoExtensionIds(copyResult);
        asserts.assertEntitiesAreCopied(PromoExtension.class,
                copiedPromoExtensionIds, List.of(promoExtensionInfo.getPromoExtension()), COPIED);
    }

    private Set<Long> getCopiedPromoExtensionIds(CopyResult<Long> copyResult) {
        return StreamEx.of(copyResult.getEntityContext().getEntities(BaseCampaign.class).values())
                .select(CampaignWithPromoExtension.class)
                .map(CampaignWithPromoExtension::getPromoExtensionId)
                .toSet();
    }

    private CopyOperation buildCopyOperation(List<Long> cids, boolean copyNotificationSettings) {
        return factory.build(
                clientInfo.getShard(), clientInfo.getClient(),
                clientInfoTo.getShard(), clientInfoTo.getClient(),
                uid,
                BaseCampaign.class, cids,
                new CopyCampaignFlags.Builder()
                        .withCopyNotificationSettings(copyNotificationSettings)
                        .build());
    }
}
