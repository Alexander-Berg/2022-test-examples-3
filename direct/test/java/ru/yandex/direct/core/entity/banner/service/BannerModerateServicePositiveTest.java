package ru.yandex.direct.core.entity.banner.service;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.old.DisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldDynamicBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldImageCreativeBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldMobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusImageModerate;
import ru.yandex.direct.core.entity.banner.model.old.StatusPhoneFlagModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.banner.service.moderation.BannerModerateService;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerCollectionType;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageCreativeBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns.fullContentPromotionCampaign;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerModerateServicePositiveTest {
    private static final ru.yandex.direct.core.entity.adgroup.model.StatusModerate ADGROUPS_MODERATE_NEW =
            ru.yandex.direct.core.entity.adgroup.model.StatusModerate.NEW;
    private static final ru.yandex.direct.core.entity.adgroup.model.StatusModerate ADGROUPS_MODERATE_READY =
            ru.yandex.direct.core.entity.adgroup.model.StatusModerate.READY;
    private static final ru.yandex.direct.core.entity.adgroup.model.StatusModerate ADGROUPS_MODERATE_YES =
            ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES;
    private static final ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate ADGROUPS_POSTMODERATE_NO =
            ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.NO;
    private static final StatusModerate CAMPAIGN_MODERATE_NEW =
            StatusModerate.NEW;
    private static final StatusModerate CAMPAIGN_MODERATE_YES =
            StatusModerate.YES;

    int shard;
    long adGroupId;
    long campaignId;
    ClientId clientId;
    long operatorUid;

    private AbstractBannerInfo bannerInfo;
    private AdGroupInfo adGroupInfo;
    private CampaignInfo campaignInfo;

    @Autowired
    private Steps steps;
    @Autowired
    private BannerModerateService service;
    @Autowired
    private OldBannerRepository bannerRepository;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private TestContentPromotionBanners testContentPromotionBanners;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private CampaignRepository campaignRepository;

    @Before
    public void setUp() throws Exception {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        campaignInfo = steps.campaignSteps().createCampaign(
                newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStatusModerate(CAMPAIGN_MODERATE_NEW), clientInfo);
        adGroupInfo = steps.adGroupSteps().createAdGroup(
                defaultTextAdGroup(campaignInfo.getCampaignId())
                        .withStatusPostModerate(ADGROUPS_POSTMODERATE_NO)
                        .withStatusModerate(ADGROUPS_MODERATE_NEW),
                campaignInfo);
        steps.keywordSteps().createKeyword(adGroupInfo);
        shard = adGroupInfo.getShard();

        clientId = clientInfo.getClientId();
        operatorUid = clientInfo.getUid();
        adGroupId = adGroupInfo.getAdGroupId();
        campaignId = campaignInfo.getCampaignId();
    }

    private long createDraftBanner(Function<OldTextBanner, OldTextBanner> addedParameters) {
        bannerInfo = steps.bannerSteps().createBanner(addedParameters.apply(
                activeTextBanner(campaignId, adGroupId).withStatusShow(false).withStatusModerate(OldBannerStatusModerate.NEW)),
                adGroupInfo);
        return bannerInfo.getBanner().getId();
    }

    private <B extends OldBanner> B oldModerateAndGetSavedBanner(long bannerId, Class<B> clazz) {
        MassResult<Long> result = service.moderateBanners(clientId, operatorUid,
                singletonList(bannerId));
        assertThat(result, isSuccessfulWithMatchers(equalTo(bannerId)));
        return clazz.cast(bannerRepository.getBanners(shard, singletonList(bannerId)).get(0));
    }

    private <B extends Banner> B moderateAndGetSavedBanner(long bannerId, Class<B> clazz) {
        MassResult<Long> result = service.moderateBanners(clientId, operatorUid,
                singletonList(bannerId));
        assertThat(result, isSuccessfulWithMatchers(equalTo(bannerId)));
        return clazz.cast(bannerTypedRepository.getStrictly(shard, singletonList(bannerId), clazz).get(0));
    }

    @Test
    public void moderateTextBanner_statusModerateReadyStatusPostModerateNo() {
        long bannerId = createDraftBanner(identity());
        OldTextBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldTextBanner.class);
        assertThat(savedBanner.getStatusModerate(), is(OldBannerStatusModerate.READY));
        assertThat(savedBanner.getStatusPostModerate(), is(OldBannerStatusPostModerate.NO));
    }

    @Test
    public void moderateMobileAppBanner_statusModerateReadyStatusPostModerateNo() {
        long bannerId = steps.bannerSteps().createActiveMobileAppBanner(adGroupInfo).getBannerId();
        bannerRepository.updateStatusModerate(shard, singletonList(bannerId), OldBannerStatusModerate.NEW);
        OldMobileAppBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldMobileAppBanner.class);
        assertThat(savedBanner.getStatusModerate(), is(OldBannerStatusModerate.READY));
        assertThat(savedBanner.getStatusPostModerate(), is(OldBannerStatusPostModerate.NO));
    }

    @Test
    public void moderateContentPromotionBanner_StatusModerateReadyStatusPostModerateNo() {
        var bannerInfo = steps.contentPromotionBannerSteps().createDefaultBanner(adGroupInfo.getClientInfo(),
                ContentPromotionContentType.COLLECTION);
        steps.newKeywordSteps().createKeyword(bannerInfo.getAdGroupInfo());
        clientId = bannerInfo.getClientId();
        operatorUid = bannerInfo.getUid();
        long bannerId = bannerInfo.getBannerId();
        bannerRepository.updateStatusModerate(shard, singletonList(bannerId), OldBannerStatusModerate.NEW);
        var savedBanner = moderateAndGetSavedBanner(bannerId, ContentPromotionBanner.class);
        assertThat(savedBanner.getStatusModerate(), is(BannerStatusModerate.READY));
        assertThat(savedBanner.getStatusPostModerate(), is(BannerStatusPostModerate.NO));
    }

    @Test
    public void moderateContentPromotionBannerInDraftAdGroup_AdGroupStatusModerateReady() {
        var adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createAdGroup(fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO)
                        .withStatusPostModerate(ADGROUPS_POSTMODERATE_NO)
                        .withStatusModerate(ADGROUPS_MODERATE_NEW));
        steps.newKeywordSteps().createKeyword(adGroupInfo);

        var bannerInfo = steps.contentPromotionBannerSteps().createBanner(
                adGroupInfo,
                testContentPromotionBanners.fullContentPromoBanner()
                        .withStatusShow(false)
                        .withStatusModerate(BannerStatusModerate.NEW));

        MassResult<Long> result = service.moderateBanners(adGroupInfo.getClientId(), adGroupInfo.getUid(),
                singletonList(bannerInfo.getBannerId()));
        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(bannerInfo.getAdGroupId())).get(0);

        assertThat(result, isSuccessfulWithMatchers(equalTo(bannerInfo.getBannerId())));
        assertThat(adGroup.getStatusModerate(), is(ADGROUPS_MODERATE_READY));
    }

    @Test
    public void moderateContentPromotionBannerInNonDraftAdGroup_AdGroupStatusModerateYes() {
        var adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createAdGroup(fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO)
                        .withStatusPostModerate(ADGROUPS_POSTMODERATE_NO)
                        .withStatusModerate(ADGROUPS_MODERATE_YES));
        steps.newKeywordSteps().createKeyword(adGroupInfo);
        shard = adGroupInfo.getShard();

        var bannerInfo = steps.contentPromotionBannerSteps().createBanner(
                adGroupInfo,
                testContentPromotionBanners.fullContentPromoBanner()
                        .withStatusShow(false)
                        .withStatusModerate(BannerStatusModerate.NEW));
        MassResult<Long> result = service.moderateBanners(adGroupInfo.getClientId(), adGroupInfo.getUid(),
                singletonList(bannerInfo.getBannerId()));
        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(bannerInfo.getAdGroupId())).get(0);

        assertThat(result, isSuccessfulWithMatchers(equalTo(bannerInfo.getBannerId())));
        assertThat(adGroup.getStatusModerate(), is(ADGROUPS_MODERATE_YES));
    }

    @Test
    public void moderateContentPromotionBannerInCampaign_CampaignStatusModerateReady() {
        campaignInfo = steps.campaignSteps().createCampaign(
                newTextCampaign(campaignInfo.getClientId(), campaignInfo.getUid())
                        .withStatusModerate(CAMPAIGN_MODERATE_NEW), campaignInfo.getClientInfo());
        adGroupInfo = steps.adGroupSteps().createAdGroup(
                defaultTextAdGroup(campaignInfo.getCampaignId())
                        .withStatusPostModerate(ADGROUPS_POSTMODERATE_NO)
                        .withStatusModerate(ADGROUPS_MODERATE_NEW),
                campaignInfo);
        steps.keywordSteps().createKeyword(adGroupInfo);
        shard = adGroupInfo.getShard();

        BannerInfo bannerInfo = steps.bannerSteps().createBanner(
                activeContentPromotionBannerCollectionType(campaignId, adGroupId)
                        .withStatusShow(false)
                        .withStatusModerate(OldBannerStatusModerate.NEW),
                adGroupInfo);
        MassResult<Long> result = service.moderateBanners(clientId, operatorUid,
                singletonList(bannerInfo.getBannerId()));
        Campaign campaign = campaignRepository.getCampaigns(shard, singletonList(bannerInfo.getCampaignId())).get(0);

        assertThat(result, isSuccessfulWithMatchers(equalTo(bannerInfo.getBannerId())));
        assertThat(campaign.getStatusModerate(), is(CampaignStatusModerate.READY));
    }

    @Test
    public void moderateYetAnotherContentPromotionBannerInCampaign_campaignStatusModerateYes() {
        campaignInfo = steps.contentPromotionCampaignSteps()
                .createCampaign(fullContentPromotionCampaign().withStatusModerate(CampaignStatusModerate.YES));

        var adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createAdGroup((ContentPromotionCampaignInfo) campaignInfo,
                        fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO)
                                .withStatusPostModerate(ADGROUPS_POSTMODERATE_NO)
                                .withStatusModerate(ADGROUPS_MODERATE_NEW));
        steps.newKeywordSteps().createKeyword(adGroupInfo);
        shard = adGroupInfo.getShard();

        var bannerInfo = steps.contentPromotionBannerSteps().createBanner(
                adGroupInfo,
                testContentPromotionBanners.fullContentPromoBanner()
                        .withStatusShow(false)
                        .withStatusModerate(BannerStatusModerate.NEW));
        shard = adGroupInfo.getShard();

        MassResult<Long> result = service.moderateBanners(campaignInfo.getClientId(), campaignInfo.getUid(),
                singletonList(bannerInfo.getBannerId()));
        Campaign campaign = campaignRepository.getCampaigns(shard, singletonList(bannerInfo.getCampaignId())).get(0);

        assertThat(result, isSuccessfulWithMatchers(equalTo(bannerInfo.getBannerId())));
        assertThat(campaign.getStatusModerate(), is(CampaignStatusModerate.YES));
    }

    @Test
    public void moderateRejectedTextBanner_statusModerateReadyStatusPostModerateRejected() {
        long bannerId = createDraftBanner(b -> b.withStatusPostModerate(OldBannerStatusPostModerate.REJECTED));
        OldTextBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldTextBanner.class);
        assertThat(savedBanner.getStatusModerate(), is(OldBannerStatusModerate.READY));
        assertThat(savedBanner.getStatusPostModerate(), is(OldBannerStatusPostModerate.REJECTED));
    }

    @Test
    public void moderateBannerWithDisplayHref_displayHrefStatusModerateReady() {
        long bannerId = createDraftBanner(b -> b.withDisplayHref("href"));
        OldTextBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldTextBanner.class);
        assertThat(savedBanner.getDisplayHrefStatusModerate(), is(DisplayHrefStatusModerate.READY));
    }

    @Test
    public void moderateBannerWithVcard_phoneFlagStatusModerateReady() {
        long bannerId = createDraftBanner(b -> b.withVcardId(1L));
        OldTextBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldTextBanner.class);
        assertThat(savedBanner.getPhoneFlag(), is(StatusPhoneFlagModerate.READY));
    }

    @Test
    public void moderateBannerWithBannerImage_bannerImageStatusModerateReady() {
        long bannerId = steps.bannerSteps().createActiveDynamicBanner(adGroupInfo).getBannerId();
        bannerRepository.updateStatusModerate(shard, singletonList(bannerId), OldBannerStatusModerate.NEW);
        OldDynamicBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldDynamicBanner.class);
        assertThat(savedBanner.getBannerImage().getStatusModerate(), is(OldStatusBannerImageModerate.READY));
    }

    @Test
    public void moderateDynamicBannerInDynamicTextAdGroups_AdGroupStatusProcessing() {
        adGroupInfo = steps.adGroupSteps().createAdGroup(
                activeDynamicTextAdGroup(adGroupInfo.getCampaignId())
                        .withStatusModerate(ADGROUPS_MODERATE_NEW)
                        .withStatusBLGenerated(StatusBLGenerated.NO),
                adGroupInfo.getClientInfo());
        steps.keywordSteps().createKeyword(adGroupInfo);
        long bannerId = steps.bannerSteps().createActiveDynamicBanner(adGroupInfo).getBannerId();
        bannerRepository.updateStatusModerate(shard, singletonList(bannerId), OldBannerStatusModerate.NEW);
        service.moderateBanners(clientId, operatorUid, singletonList(bannerId));
        DynamicTextAdGroup savedAdGroup = (DynamicTextAdGroup) adGroupRepository
                .getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);
        assertThat(savedAdGroup.getStatusBLGenerated(), is(StatusBLGenerated.PROCESSING));
    }

    @Test
    public void moderateDynamicBannerInDynamicFeedAdGroups_AdGroupStatusProcessing() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(adGroupInfo.getClientInfo());
        adGroupInfo = steps.adGroupSteps().createAdGroup(
                activeDynamicFeedAdGroup(adGroupInfo.getCampaignId(), feedInfo.getFeedId())
                        .withStatusModerate(ADGROUPS_MODERATE_NEW)
                        .withStatusBLGenerated(StatusBLGenerated.NO),
                adGroupInfo.getClientInfo());
        steps.keywordSteps().createKeyword(adGroupInfo);
        long bannerId = steps.bannerSteps().createActiveDynamicBanner(adGroupInfo).getBannerId();
        bannerRepository.updateStatusModerate(shard, singletonList(bannerId), OldBannerStatusModerate.NEW);
        service.moderateBanners(clientId, operatorUid, singletonList(bannerId));
        DynamicFeedAdGroup savedAdGroup = (DynamicFeedAdGroup) adGroupRepository
                .getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);
        assertThat(savedAdGroup.getStatusBLGenerated(), is(StatusBLGenerated.PROCESSING));
    }

    @Test
    public void moderateBannerWithImage_imageStatusModerateReady() {
        long bannerId = steps.bannerSteps().createActiveImageHashBanner(adGroupInfo).getBannerId();
        bannerRepository.updateStatusModerate(shard, singletonList(bannerId), OldBannerStatusModerate.NEW);
        OldImageHashBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldImageHashBanner.class);
        assertThat(savedBanner.getImage().getStatusModerate(), is(StatusImageModerate.READY));
    }

    @Test
    public void moderateBannerWithImageCreative_creativeStatusModerateReady() {
        long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCanvasCreative(adGroupInfo.getClientInfo(), creativeId);

        long bannerId = steps.bannerSteps().createActiveImageCreativeBanner(
                activeImageCreativeBanner(campaignId, adGroupId, creativeId)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW)
                        .withStatusModerate(OldBannerStatusModerate.NEW),
                adGroupInfo).getBannerId();
        OldImageCreativeBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldImageCreativeBanner.class);
        assertThat(savedBanner.getCreativeStatusModerate(), is(OldBannerCreativeStatusModerate.READY));
    }

    @Test
    public void moderateCpcVideoBannerWithDraftCreative_creativeStatusModerateReady() {
        long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpcVideoCreative(adGroupInfo.getClientInfo(), creativeId);
        OldCpcVideoBanner banner = activeCpcVideoBanner(campaignId, adGroupId, creativeId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);

        long bannerId = steps.bannerSteps().createActiveCpcVideoBanner(banner, adGroupInfo).getBannerId();
        OldCpcVideoBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldCpcVideoBanner.class);
        assertThat(savedBanner.getCreativeStatusModerate(), is(OldBannerCreativeStatusModerate.READY));
    }

    @Test
    public void moderateCpcVideoBannerWithModeratedCreative_creativeStatusModerateYes() {
        long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultVideoAdditionCreative(adGroupInfo.getClientInfo(), creativeId);
        OldCpcVideoBanner banner = activeCpcVideoBanner(campaignId, adGroupId, creativeId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);

        long bannerId = steps.bannerSteps().createActiveCpcVideoBanner(banner, adGroupInfo).getBannerId();
        OldCpcVideoBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldCpcVideoBanner.class);
        assertThat(savedBanner.getCreativeStatusModerate(), is(OldBannerCreativeStatusModerate.YES));
    }

    @Test
    public void moderateCpmBannerWithNonVideoCreative_creativeStatusModerateReady() {
        long creativeId = steps.creativeSteps().addDefaultCanvasCreative(adGroupInfo.getClientInfo()).getCreativeId();
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, creativeId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);

        long bannerId = steps.bannerSteps().createActiveCpmBanner(banner, adGroupInfo).getBannerId();
        OldCpmBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldCpmBanner.class);
        assertThat(savedBanner.getCreativeStatusModerate(), is(OldBannerCreativeStatusModerate.READY));
    }

    @Test
    public void moderateCpmBannerWithVideoAdditionCreative_creativeStatusModerateYes() {
        long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultVideoAdditionCreative(adGroupInfo.getClientInfo(), creativeId);
        OldCpmBanner banner = activeCpmBanner(campaignId, adGroupId, creativeId)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW);

        long bannerId = steps.bannerSteps().createActiveCpmBanner(banner, adGroupInfo).getBannerId();
        OldCpmBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldCpmBanner.class);
        assertThat(savedBanner.getCreativeStatusModerate(), is(OldBannerCreativeStatusModerate.YES));
    }

    @Test
    public void moderateBannerWithTurboLanding_bannerTurboLandingStatusModerateReady() {
        OldBannerTurboLanding defaultBannerTurboLanding =
                steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId);
        long bannerId = createDraftBanner(b -> b
                .withTurboLandingId(defaultBannerTurboLanding.getId())
                .withTurboLandingStatusModerate(defaultBannerTurboLanding.getStatusModerate()));
        OldTextBanner savedBanner = oldModerateAndGetSavedBanner(bannerId, OldTextBanner.class);
        assertThat(savedBanner.getTurboLandingStatusModerate(), is(OldBannerTurboLandingStatusModerate.READY));
    }

    @Test
    public void moderateTextBannerInDraftAdGroup_adGroupStatusModerateReady() {
        long bannerId = createDraftBanner(identity());
        MassResult<Long> result = service.moderateBanners(clientId, operatorUid,
                singletonList(bannerId));
        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(bannerInfo.getAdGroupId())).get(0);
        assertThat(result, isSuccessfulWithMatchers(equalTo(bannerId)));
        assertThat(adGroup.getStatusModerate(), is(ADGROUPS_MODERATE_READY));
    }

    @Test
    public void moderateTextBannerInNonDraftAdGroup_adGroupStatusModerateYes() {
        adGroupInfo = steps.adGroupSteps().createAdGroup(
                defaultTextAdGroup(campaignInfo.getCampaignId())
                        .withStatusPostModerate(ADGROUPS_POSTMODERATE_NO)
                        .withStatusModerate(ADGROUPS_MODERATE_YES),
                campaignInfo);
        steps.keywordSteps().createKeyword(adGroupInfo);
        shard = adGroupInfo.getShard();

        long bannerId = createDraftBanner(identity());
        MassResult<Long> result = service.moderateBanners(clientId, operatorUid,
                singletonList(bannerId));
        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(bannerInfo.getAdGroupId())).get(0);
        assertThat(result, isSuccessfulWithMatchers(equalTo(bannerId)));
        assertThat(adGroup.getStatusModerate(), is(ADGROUPS_MODERATE_YES));
    }

    @Test
    public void moderateSingleTextBannerInCampaign_campaignStatusModerateReady() {
        campaignInfo = steps.campaignSteps().createCampaign(
                newTextCampaign(campaignInfo.getClientId(), campaignInfo.getUid())
                        .withStatusModerate(CAMPAIGN_MODERATE_NEW), campaignInfo.getClientInfo());
        adGroupInfo = steps.adGroupSteps().createAdGroup(
                defaultTextAdGroup(campaignInfo.getCampaignId())
                        .withStatusPostModerate(ADGROUPS_POSTMODERATE_NO)
                        .withStatusModerate(ADGROUPS_MODERATE_NEW),
                campaignInfo);
        steps.keywordSteps().createKeyword(adGroupInfo);
        shard = adGroupInfo.getShard();

        long bannerId = createDraftBanner(identity());
        MassResult<Long> result = service.moderateBanners(clientId, operatorUid,
                singletonList(bannerId));
        Campaign campaign = campaignRepository.getCampaigns(shard, singletonList(bannerInfo.getCampaignId())).get(0);
        assertThat(result, isSuccessfulWithMatchers(equalTo(bannerId)));
        assertThat(campaign.getStatusModerate(), is(CampaignStatusModerate.READY));
    }

    @Test
    public void moderateYetAnotherTextBannerInCampaign_campaignStatusModerateYes() {
        campaignInfo = steps.campaignSteps().createCampaign(
                newTextCampaign(campaignInfo.getClientId(), campaignInfo.getUid())
                        .withStatusModerate(CAMPAIGN_MODERATE_YES), campaignInfo.getClientInfo());
        adGroupInfo = steps.adGroupSteps().createAdGroup(
                defaultTextAdGroup(campaignInfo.getCampaignId())
                        .withStatusPostModerate(ADGROUPS_POSTMODERATE_NO)
                        .withStatusModerate(ADGROUPS_MODERATE_NEW),
                campaignInfo);
        steps.keywordSteps().createKeyword(adGroupInfo);
        shard = adGroupInfo.getShard();

        long bannerId = createDraftBanner(identity());
        MassResult<Long> result = service.moderateBanners(clientId, operatorUid,
                singletonList(bannerId));
        Campaign campaign = campaignRepository.getCampaigns(shard, singletonList(bannerInfo.getCampaignId())).get(0);
        assertThat(result, isSuccessfulWithMatchers(equalTo(bannerId)));
        assertThat(campaign.getStatusModerate(), is(CampaignStatusModerate.YES));
    }
}
