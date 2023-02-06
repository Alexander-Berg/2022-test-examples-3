package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.yandex.direct.api.v5.ads.AdBuilderAdGetItem;
import com.yandex.direct.api.v5.ads.AdGetItem;
import com.yandex.direct.api.v5.ads.GetResponse;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.entity.ads.container.AdsGetContainer;
import ru.yandex.direct.api.v5.entity.ads.converter.GetResponseConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.security.utils.ApiAuthenticationSourceMockBuilder;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.container.AdsSelectionCriteria;
import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.banner.type.creative.BannerCreativeRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository;
import ru.yandex.direct.core.entity.moderationreason.service.ModerationReasonService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.repository.ApiUserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceMainBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceMainBanners.fullPerformanceMainBanner;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@Api5Test
@RunWith(JUnitParamsRunner.class)
public class GetAdsDelegateTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    Steps steps;

    @Autowired
    ApiUserRepository apiUserRepository;

    @Autowired
    ApiAuthenticationSource apiAuthenticationSourceMock;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private BannerCommonRepository bannerRepository;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CalloutRepository calloutRepository;

    @Autowired
    private BannerCreativeRepository newBannerCreativeRepository;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private ModerationReasonService moderationReasonService;

    @Autowired
    private MobileContentRepository mobileContentRepository;

    @Autowired
    private GetResponseConverter getResponseConverter;

    @Autowired
    private AdGroupService adGroupService;

    @Autowired
    private AdGroupRepository adGroupRepository;

    private GetAdsDelegate delegate;

    private List<Long> bannerIds;
    private AdGroupInfo performanceAdGroupInfo;
    private CreativeInfo performanceCreative;
    private NewPerformanceMainBannerInfo performanceMainBannerInfo;
    private ClientInfo clientInfo;
    private ClientId clientId;


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();

        ApiUser operatorUser = apiUserRepository.fetchByUid(clientInfo.getShard(), clientInfo.getUid());
        new ApiAuthenticationSourceMockBuilder()
                .withOperator(operatorUser)
                .tuneAuthSourceMock(apiAuthenticationSourceMock);

        delegate = new GetAdsDelegate(apiAuthenticationSourceMock, shardHelper,
                bannerService, bannerRepository, campaignService, calloutRepository, newBannerCreativeRepository,
                creativeRepository, moderationReasonService, mobileContentRepository,
                getResponseConverter, adGroupService, adGroupRepository);

        bannerIds = new ArrayList<>();
        bannerIds.add(steps.contentPromotionBannerSteps()
                .createDefaultBanner(clientInfo, ContentPromotionContentType.COLLECTION)
                .getBannerId());
        bannerIds.add(steps.contentPromotionBannerSteps()
                .createDefaultBanner(clientInfo, ContentPromotionContentType.VIDEO)
                .getBannerId());
        bannerIds.add(steps.contentPromotionBannerSteps()
                .createDefaultBanner(clientInfo, ContentPromotionContentType.EDA)
                .getBannerId());
        bannerIds.add(steps.bannerSteps().createActiveCpmVideoBanner(clientInfo).getBannerId());

        performanceAdGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        performanceCreative = steps.creativeSteps().createCreative(
                defaultPerformanceCreative(clientInfo.getClientId(), null), clientInfo);
        bannerIds.add(steps.bannerSteps().createBanner(
                activePerformanceBanner(performanceAdGroupInfo.getCampaignId(), performanceAdGroupInfo.getAdGroupId(),
                        performanceCreative.getCreativeId()).withStatusShow(true),
                performanceAdGroupInfo).getBannerId());

        BannerImageFormat bannerImageFormat = steps.bannerSteps()
                .createLogoImageFormat(performanceAdGroupInfo.getClientInfo());
        performanceMainBannerInfo = steps.performanceMainBannerSteps()
                .createPerformanceMainBanner(new NewPerformanceMainBannerInfo()
                        .withAdGroupInfo(performanceAdGroupInfo)
                        .withBanner(fullPerformanceMainBanner()
                                .withLogoImageHash(bannerImageFormat.getImageHash())
                                .withLogoStatusModerate(BannerLogoStatusModerate.SENT)));
        bannerIds.add(performanceMainBannerInfo.getBannerId());
    }

    @Test
    public void getPerformanceAd_success() {
        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(ImmutableSet.of(AdAnyFieldEnum.AD_ID, AdAnyFieldEnum.AD_CAMPAIGN_ID,
                        AdAnyFieldEnum.SMART_AD_BUILDER_AD_CREATIVE),
                        new AdsSelectionCriteria()
                                .withAdIds(new HashSet<>(bannerIds))
                                .withSelectCpmBanner(false)
                                .withSelectCpmVideo(true)
                                .withTypes(BannersBannerType.performance),
                        LimitOffset.maxLimited());
        GetResponse response = get(request);
        assertThat(response.getAds()).hasSize(1);
        AdGetItem actualBanner = response.getAds().get(0);
        assertThat(actualBanner.getCampaignId()).isEqualTo(performanceAdGroupInfo.getCampaignId());
        AdBuilderAdGetItem actualCreative = actualBanner.getSmartAdBuilderAd().getCreative();
        assertThat(actualCreative.getCreativeId()).isEqualTo(performanceCreative.getCreativeId());
        assertThat(actualCreative.getPreviewUrl()).isEqualTo(performanceCreative.getCreative().getLivePreviewUrl());
        assertThat(actualCreative.getThumbnailUrl()).isEqualTo(performanceCreative.getCreative().getPreviewUrl());
    }

    @Test
    public void getPerformanceMainAd_success() {
        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(ImmutableSet.of(AdAnyFieldEnum.AD_ID, AdAnyFieldEnum.AD_CAMPAIGN_ID,
                        AdAnyFieldEnum.SMART_AD_BUILDER_AD_CREATIVE,
                        AdAnyFieldEnum.SMART_AD_BUILDER_AD_LOGO_IMAGE_HASH),
                        new AdsSelectionCriteria()
                                .withAdIds(new HashSet<>(bannerIds))
                                .withSelectCpmBanner(false)
                                .withSelectCpmVideo(true)
                                .withTypes(BannersBannerType.performance_main),
                        LimitOffset.maxLimited());

        GetResponse response = get(request);

        assertThat(response.getAds()).isNotEmpty();
        assertSoftly(softly -> {
            softly.assertThat(response.getAds()).hasSize(1);
            AdGetItem ad = response.getAds().get(0);
            softly.assertThat(ad.getCampaignId()).isEqualTo(performanceAdGroupInfo.getCampaignId());
            softly.assertThat(ad.getSmartAdBuilderAd()).satisfies(smartAd -> {
                assertThat(smartAd).isNotNull();
                assertThat(smartAd.getCreative()).isNull();
            });
            softly.assertThat(ad.getSmartAdBuilderAd()).satisfies(smartAd -> {
                assertThat(smartAd).isNotNull();
                assertThat(smartAd.getLogoExtensionHash()).isNotNull();
                assertThat(smartAd.getLogoExtensionHash().getValue())
                        .isEqualTo(((PerformanceBannerMain) performanceMainBannerInfo.getBanner()).getLogoImageHash());
            });
        });
    }

    @Test
    public void get_CpmVideoAdGroup_ReturnResult() {
        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(ImmutableSet.of(AdAnyFieldEnum.AD_ID),
                        new AdsSelectionCriteria()
                                .withAdIds(new HashSet<>(bannerIds))
                                .withSelectCpmBanner(false)
                                .withSelectCpmVideo(true)
                                .withTypes(BannersBannerType.cpm_banner),
                        LimitOffset.maxLimited());
        List<AdsGetContainer> result = delegate.get(request);

        assertThat(result).hasSize(1);
    }

    @Test
    public void get_ContentPromotionVideoBanners_ReturnResult() {
        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(ImmutableSet.of(AdAnyFieldEnum.AD_ID),
                        new AdsSelectionCriteria()
                                .withAdIds(new HashSet<>(bannerIds))
                                .withSelectCpmBanner(true)
                                .withSelectCpmVideo(false)
                                .withTypes(BannersBannerType.content_promotion, BannersBannerType.cpm_banner)
                                .withContentPromotionAdgroupTypes(Set.of(ContentPromotionAdgroupType.VIDEO)),
                        LimitOffset.maxLimited());
        List<AdsGetContainer> result = delegate.get(request);

        assertThat(result).hasSize(1);
        ContentPromotionAdgroupType type = result.stream()
                .filter(t -> t.getAdGroupType() == AdGroupType.CONTENT_PROMOTION)
                .findFirst()
                .map(AdsGetContainer::getContentPromotionAdgroupType)
                .get();
        assertThat(type).isEqualTo(ContentPromotionAdgroupType.VIDEO);
    }

    @Test
    public void get_ContentPromotionEdaBanners_ReturnResult() {
        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(ImmutableSet.of(AdAnyFieldEnum.AD_ID),
                        new AdsSelectionCriteria()
                                .withAdIds(new HashSet<>(bannerIds))
                                .withSelectCpmBanner(true)
                                .withSelectCpmVideo(false)
                                .withTypes(BannersBannerType.content_promotion, BannersBannerType.cpm_banner)
                                .withContentPromotionAdgroupTypes(Set.of(ContentPromotionAdgroupType.EDA)),
                        LimitOffset.maxLimited());
        List<AdsGetContainer> result = delegate.get(request);

        assertThat(result).hasSize(1);
        ContentPromotionAdgroupType type = result.stream()
                .filter(t -> t.getAdGroupType() == AdGroupType.CONTENT_PROMOTION)
                .findFirst()
                .map(AdsGetContainer::getContentPromotionAdgroupType)
                .get();
        assertThat(type).isEqualTo(ContentPromotionAdgroupType.EDA);
    }

    @Test
    public void get_ContentPromotionAllBanners_ReturnResult() {
        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(ImmutableSet.of(AdAnyFieldEnum.AD_ID),
                        new AdsSelectionCriteria()
                                .withAdIds(new HashSet<>(bannerIds))
                                .withSelectCpmBanner(true)
                                .withSelectCpmVideo(false)
                                .withTypes(BannersBannerType.content_promotion),
                        LimitOffset.maxLimited());
        List<AdsGetContainer> result = delegate.get(request);

        assertThat(result).hasSize(3);
        Set<ContentPromotionAdgroupType> types = result.stream()
                .filter(t -> t.getAdGroupType() == AdGroupType.CONTENT_PROMOTION)
                .map(AdsGetContainer::getContentPromotionAdgroupType)
                .collect(Collectors.toSet());
        assertThat(types).containsExactlyInAnyOrder(ContentPromotionAdgroupType.VIDEO,
                ContentPromotionAdgroupType.COLLECTION, ContentPromotionAdgroupType.EDA);
    }

    @Test
    public void get_CpcVideoBannerInMobileAdGroup_ReturnResult() {
        AdGroupInfo mobileContentAdGroup = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        Long mobileContentBannerId = steps.bannerSteps()
                .createActiveMobileAppBanner(mobileContentAdGroup)
                .getBannerId();

        Creative cpcVideoCreative = defaultCpcVideoForCpcVideoBanner(clientInfo.getClientId(), null);
        Long cpcVideoCreativeId = steps.creativeSteps().createCreative(cpcVideoCreative, clientInfo).getCreativeId();

        AdGroupInfo textAdGroup = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        Long cpcVideoBannerInTextAdGroup = steps.bannerSteps()
                .createDefaultCpcVideoBanner(textAdGroup, cpcVideoCreativeId)
                .getBannerId();
        Long cpcVideoBannerInMobileAdGroup = steps.bannerSteps()
                .createDefaultCpcVideoBanner(mobileContentAdGroup, cpcVideoCreativeId)
                .getBannerId();

        Set<Long> adIds = Set.of(mobileContentBannerId, cpcVideoBannerInTextAdGroup, cpcVideoBannerInMobileAdGroup);
        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(ImmutableSet.of(AdAnyFieldEnum.AD_ID),
                        new AdsSelectionCriteria()
                                .withAdIds(adIds)
                                .withSelectCpmBanner(false)
                                .withSelectCpmVideo(true)
                                .withTypes(BannersBannerType.mobile_content, BannersBannerType.cpc_video),
                        LimitOffset.maxLimited());
        List<AdsGetContainer> result = delegate.get(request);

        Set<Long> actualIds = listToSet(result, r -> r.getAd().getId());
        Set<Long> expectedIds =
                Set.of(mobileContentBannerId, cpcVideoBannerInTextAdGroup, cpcVideoBannerInMobileAdGroup);
        assertThat(actualIds)
                .as("видеобъявления поддерживаются в РМП и ТГО в методе ads.get")
                .containsExactlyInAnyOrderElementsOf(expectedIds);
    }

    Iterable<Object[]> paramsForCpcVideoBanner() {
        var data = StreamEx.of(BannersBannerType.values())
                .map(bannerType -> new Object[]{bannerType.name(),
                        Set.of(bannerType), bannerType == BannersBannerType.cpc_video})
                .toList();
        data.add(new Object[]{"All types", StreamEx.of(BannersBannerType.values()).toSet(), true});
        data.add(new Object[]{"All types, except cpc_video", StreamEx.of(BannersBannerType.values())
                .filter(bannerType -> bannerType != BannersBannerType.cpc_video)
                .toSet(), false});
        return data;
    }

    @Test
    @Parameters(method = "paramsForCpcVideoBanner")
    @TestCaseName("{0} -> get banner data: {2}")
    public void get_CpcVideoBannerInMobileAdGroup(String description,
            Set<BannersBannerType> selectBannerTypes,
            boolean expectIdInResult)
    {
        Creative cpcVideoCreative = defaultCpcVideoForCpcVideoBanner(clientInfo.getClientId(), null);
        Long cpcVideoCreativeId = steps.creativeSteps().createCreative(cpcVideoCreative, clientInfo).getCreativeId();

        AdGroupInfo mobileContentAdGroup = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        Long cpcVideoBanner = steps.bannerSteps()
                .createDefaultCpcVideoBanner(mobileContentAdGroup, cpcVideoCreativeId)
                .getBannerId();

        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(ImmutableSet.of(AdAnyFieldEnum.AD_ID),
                        new AdsSelectionCriteria()
                                .withAdIds(Collections.singleton(cpcVideoBanner))
                                .withSelectCpmBanner(false)
                                .withSelectCpmVideo(false)
                                .withTypes(selectBannerTypes),
                        LimitOffset.maxLimited());
        List<AdsGetContainer> result = delegate.get(request);

        Set<Long> actualIds = listToSet(result, r -> r.getAd().getId());
        Set<Long> expectedIds = expectIdInResult
                ? Collections.singleton(cpcVideoBanner)
                : Collections.emptySet();
        assertThat(actualIds)
                .as("получение cpc_video РМП в методе ads.get")
                .containsExactlyInAnyOrderElementsOf(expectedIds);
    }

    @Test
    @Parameters(method = "paramsForCpcVideoBanner")
    @TestCaseName("{0}")
    public void get_CpcVideoBannerInTextAdGroup(String description,
            Set<BannersBannerType> selectBannerTypes,
            boolean expectIdInResult)
    {
        Creative cpcVideoCreative = defaultCpcVideoForCpcVideoBanner(clientInfo.getClientId(), null);
        Long cpcVideoCreativeId = steps.creativeSteps().createCreative(cpcVideoCreative, clientInfo).getCreativeId();

        AdGroupInfo textAdGroup = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        Long cpcVideoBanner = steps.bannerSteps()
                .createDefaultCpcVideoBanner(textAdGroup, cpcVideoCreativeId)
                .getBannerId();

        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(ImmutableSet.of(AdAnyFieldEnum.AD_ID),
                        new AdsSelectionCriteria()
                                .withAdIds(Collections.singleton(cpcVideoBanner))
                                .withSelectCpmBanner(false)
                                .withSelectCpmVideo(false)
                                .withTypes(selectBannerTypes),
                        LimitOffset.maxLimited());
        List<AdsGetContainer> result = delegate.get(request);

        Set<Long> actualIds = listToSet(result, r -> r.getAd().getId());
        Set<Long> expectedIds = expectIdInResult
                ? Collections.singleton(cpcVideoBanner)
                : Collections.emptySet();
        assertThat(actualIds).containsExactlyInAnyOrderElementsOf(expectedIds);
    }

    @Test
    public void get_adWithTrackingPhone_success() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var campaignId = adGroupInfo.getCampaignId();
        long permalinkId = RandomUtils.nextLong();

        Long phoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkId).getId();
        var banner = activeTextBanner(campaignId, adGroupId).withPermalinkId(permalinkId).withPhoneId(phoneId);
        var bannerId = steps.bannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request =
                new GenericGetRequest<>(ImmutableSet.of(AdAnyFieldEnum.AD_ID),
                        new AdsSelectionCriteria()
                                .withAdIds(Set.of(bannerId))
                                .withSelectCpmBanner(false)
                                .withSelectCpmVideo(false)
                                .withTypes(BannersBannerType.text),
                        LimitOffset.maxLimited());
        List<AdsGetContainer> apiResult = delegate.get(request);
        var actualBanner = (TextBanner) apiResult.get(0).getAd();
        assertSoftly(softly -> {
            Assertions.assertThat(actualBanner.getPermalinkId()).isEqualTo(permalinkId);
            Assertions.assertThat(actualBanner.getPhoneId()).isEqualTo(phoneId);
        });
    }

    private GetResponse get(GenericGetRequest<AdAnyFieldEnum, AdsSelectionCriteria> request) {
        List<AdsGetContainer> adsGetContainers = delegate.get(request);
        long limit = request.getLimitOffset().limit();
        return delegate.convertGetResponse(adsGetContainers, request.getRequestedFields(), limit);
    }
}
