package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.List;

import com.yandex.direct.api.v5.ads.ResumeRequest;
import com.yandex.direct.api.v5.general.IdsCriteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.ads.converter.AdsSuspendResumeRequestConverter;
import ru.yandex.direct.api.v5.entity.ads.validation.AdsSuspendResumeRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannerSuspendResumeService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewCpmBannerInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceBannerInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.CpmBannerSteps;
import ru.yandex.direct.core.testing.steps.PerformanceBannerSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.fullPerformanceBanner;

@Api5Test
@RunWith(SpringRunner.class)
public class ResumeAdsDelegateTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CpmBannerSteps cpmBannerSteps;

    @Autowired
    private PerformanceBannerSteps performanceBannerSteps;

    @Autowired
    private AdsSuspendResumeRequestValidator adsSuspendResumeRequestValidator;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private BannerSuspendResumeService bannerService;

    @Autowired
    private AdsSuspendResumeRequestConverter requestConverter;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private ResultConverter resultConverter;

    @Mock
    private ApiAuthenticationSource auth;

    private ResumeAdsDelegate delegate;
    private ClientInfo clientInfo;
    private ClientId clientId;


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        delegate = new ResumeAdsDelegate(
                bannerService,
                auth,
                adsSuspendResumeRequestValidator,
                requestConverter,
                resultConverter,
                ppcPropertiesSupport,
                featureService
        );
    }

    @Test
    public void resumeCmpAd_success() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var creative = defaultCanvas(clientId, null);
        var creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();
        var campaignId = adGroupInfo.getCampaignId();

        CpmBanner cpmBanner = fullCpmBanner(creativeId)
                .withAdGroupId(adGroupId)
                .withCampaignId(campaignId)
                .withStatusShow(true);

        Long bannerId = cpmBannerSteps.createCpmBanner(new NewCpmBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withClientInfo(clientInfo)
                .withBanner(cpmBanner)).getBannerId();

        ValidationResult<List<ModelChanges<BannerWithSystemFields>>, DefectType> vr = validate(bannerId);
        var apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getResult().get(0).getErrors()).isEmpty();
        checkSuccessfulResume(adGroupInfo.getShard(), bannerId);
    }

    @Test
    public void resumePerformanceAd_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var creative = defaultPerformanceCreative(clientId, null);
        var creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();
        var campaignId = adGroupInfo.getCampaignId();

        PerformanceBanner performanceBanner =
                fullPerformanceBanner(campaignId, adGroupId, creativeId).withStatusShow(true);

        Long bannerId = performanceBannerSteps.createPerformanceBanner(new NewPerformanceBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withClientInfo(clientInfo)
                .withBanner(performanceBanner)).getBannerId();

        ValidationResult<List<ModelChanges<BannerWithSystemFields>>, DefectType> vr = validate(bannerId);
        var apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getResult().get(0).getErrors()).isEmpty();
        checkSuccessfulResume(adGroupInfo.getShard(), bannerId);
    }

    private ValidationResult<List<ModelChanges<BannerWithSystemFields>>, DefectType> validate(Long bannerId) {
        var idsCriteria = new IdsCriteria().withIds(List.of(bannerId));
        var resumeRequest = new ResumeRequest().withSelectionCriteria(idsCriteria);
        List<ModelChanges<BannerWithSystemFields>> modelChanges = delegate.convertRequest(resumeRequest);
        return delegate.validateInternalRequest(modelChanges);
    }

    private void checkSuccessfulResume(int shard, Long bannerId) {
        List<BannerWithSystemFields> banners =
                bannerTypedRepository.getSafely(shard, List.of(bannerId), BannerWithSystemFields.class);
        var actualBanner = banners.get(0);
        assertThat(actualBanner.getStatusShow()).isTrue();
    }
}
