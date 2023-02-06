package ru.yandex.direct.api.v5.entity.ads.delegate;

import com.yandex.direct.api.v5.ads.ModerateRequest;
import com.yandex.direct.api.v5.ads.ModerateResponse;
import com.yandex.direct.api.v5.general.IdsCriteria;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.ads.validation.AdsModerateRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.moderation.BannerModerateService;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.fullPerformanceBanner;

@Api5Test
@RunWith(SpringRunner.class)
public class ModerateAdsDelegateTest {
    @Autowired
    private Steps steps;
    @Autowired
    private AdsModerateRequestValidator adsModerateRequestValidator;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private CreativeRepository creativeRepository;
    @Autowired
    private BannerModerateService bannerService;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;

    private GenericApiService genericApiService;
    private ModerateAdsDelegate delegate;
    private ClientInfo clientInfo;
    private int shard;

    private static ModerateRequest createRequest(Long id) {
        return new ModerateRequest()
                .withSelectionCriteria(
                        new IdsCriteria()
                                .withIds(singleton(id)));
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        ApiUser user = new ApiUser()
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId());
        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(user);
        when(auth.getChiefSubclient()).thenReturn(user);
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));
        delegate = new ModerateAdsDelegate(
                auth,
                bannerService,
                adsModerateRequestValidator,
                resultConverter,
                ppcPropertiesSupport,
                featureService
        );
    }

    @Test
    public void moderatePerformanceAd_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var creative = defaultPerformanceCreative(clientInfo.getClientId(), null);
        var creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();
        var adGroupId = adGroupInfo.getAdGroupId();
        var campaignId = adGroupInfo.getCampaignId();
        var banner = fullPerformanceBanner(campaignId, adGroupId, creativeId)
                .withStatusModerate(BannerStatusModerate.NEW);
        var bannerId = steps.performanceBannerSteps().createBanner(banner, adGroupInfo).getBannerId();
        steps.performanceFilterSteps().addDefaultBidsPerformance(adGroupInfo);

        ModerateRequest request = createRequest(bannerId);
        ModerateResponse response = genericApiService.doAction(delegate, request);
        checkState(response.getModerateResults().get(0).getErrors().isEmpty(), "Unexpected error");

        var actualBanner = bannerTypedRepository
                .getStrictlyFullyFilled(shard, singleton(bannerId), PerformanceBanner.class)
                .get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualBanner.getStatusModerate())
                    .as("BannerStatusModerate").isEqualTo(BannerStatusModerate.YES);
            soft.assertThat(actualBanner.getStatusPostModerate())
                    .as("BannerStatusPostModerate").isEqualTo(BannerStatusPostModerate.YES);
        });
    }

    @Test
    public void moderatePerformanceAd_whenCampaignAndGroupAreDraft_success() {
        var campaignInfo = steps.campaignSteps().createDraftPerformanceCampaign(clientInfo);
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDraftPerformanceAdGroup(campaignInfo);
        steps.performanceFilterSteps().addPerformanceFilter(adGroupInfo);
        var creative = defaultPerformanceCreative(clientInfo.getClientId(), null)
                .withStatusModerate(ru.yandex.direct.core.entity.creative.model.StatusModerate.NEW)
                .withSumGeo(null);
        var creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();
        var adGroupId = adGroupInfo.getAdGroupId();
        var campaignId = adGroupInfo.getCampaignId();
        var banner = fullPerformanceBanner(campaignId, adGroupId, creativeId)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusActive(false);
        var bannerId = steps.performanceBannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        ModerateRequest request = createRequest(bannerId);
        ModerateResponse response = genericApiService.doAction(delegate, request);
        checkState(response.getModerateResults().get(0).getErrors().isEmpty(), "Unexpected error");

        var actualBanner = bannerTypedRepository
                .getStrictlyFullyFilled(shard, singleton(bannerId), PerformanceBanner.class)
                .get(0);
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singleton(adGroupInfo.getAdGroupId())).get(0);
        Creative actualCreative = creativeRepository.getCreatives(shard, singleton(creativeId)).get(0);
        Campaign actualCampaign =
                campaignRepository.getCampaigns(shard, singleton(campaignInfo.getCampaignId())).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualCampaign.getStatusModerate())
                    .as("CampaignStatusModerate").isEqualTo(CampaignStatusModerate.READY);
            soft.assertThat(actualAdGroup.getStatusModerate())
                    .as("AdGroupStatusModerate")
                    .isEqualTo(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES);
            soft.assertThat(actualBanner.getStatusModerate())
                    .as("BannerStatusModerate").isEqualTo(BannerStatusModerate.YES);
            soft.assertThat(actualBanner.getStatusPostModerate())
                    .as("BannerStatusPostModerate").isEqualTo(BannerStatusPostModerate.YES);
            soft.assertThat(actualCreative.getStatusModerate())
                    .as("CreativeStatusModerate")
                    .isEqualTo(ru.yandex.direct.core.entity.creative.model.StatusModerate.READY);
            soft.assertThat(actualCreative.getSumGeo())
                    .as("CreativeSumGeo")
                    .isNotEmpty();
        });
    }
}
