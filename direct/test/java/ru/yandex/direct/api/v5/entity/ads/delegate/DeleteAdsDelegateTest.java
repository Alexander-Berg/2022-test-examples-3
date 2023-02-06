package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.List;

import com.yandex.direct.api.v5.ads.DeleteRequest;
import com.yandex.direct.api.v5.general.IdsCriteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.ads.validation.AdsAdGroupTypeValidator;
import ru.yandex.direct.api.v5.entity.ads.validation.AdsDeleteRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.fullPerformanceBanner;

@Api5Test
@RunWith(SpringRunner.class)
public class DeleteAdsDelegateTest {

    @Autowired
    private Steps steps;

    @Autowired
    private AdsAdGroupTypeValidator adsAdGroupTypeValidator;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private ResultConverter resultConverter;

    @Autowired
    private AdsDeleteRequestValidator requestValidator;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private FeatureService featureService;

    @Mock
    private ApiAuthenticationSource auth;

    private DeleteAdsDelegate delegate;

    private ClientInfo clientInfo;
    private ClientId clientId;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));

        delegate = new DeleteAdsDelegate(
                auth,
                resultConverter,
                requestValidator,
                bannerService,
                adsAdGroupTypeValidator,
                ppcPropertiesSupport,
                featureService
        );
    }

    @Test
    public void deletePerformanceAd_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var campaignId = adGroupInfo.getCampaignId();
        var creative = defaultPerformanceCreative(clientId, null);
        var creativeId = steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();
        var banner = fullPerformanceBanner(campaignId, adGroupId, creativeId)
                .withBsBannerId(0L)
                .withStatusModerate(BannerStatusModerate.NO)
                .withStatusPostModerate(BannerStatusPostModerate.NO);
        var bannerId = steps.performanceBannerSteps().createBanner(banner, adGroupInfo).getBannerId();

        ValidationResult<List<Long>, DefectType> vr = validate(bannerId);
        var apiResult = delegate.processList(vr.getValue());
        assertThat(apiResult.getResult().get(0).getErrors()).isEmpty();
        checkSuccessfulDelete(adGroupInfo.getShard(), bannerId);
    }

    private ValidationResult<List<Long>, DefectType> validate(Long bannerId) {
        var deleteRequest = new DeleteRequest().withSelectionCriteria(new IdsCriteria().withIds(bannerId));
        List<Long> request = delegate.convertRequest(deleteRequest);
        return delegate.validateInternalRequest(request);
    }

    private void checkSuccessfulDelete(int shard, Long bannerId) {
        var banners = bannerTypedRepository.getTyped(shard, List.of(bannerId));
        assertThat(banners).isEmpty();
    }
}
