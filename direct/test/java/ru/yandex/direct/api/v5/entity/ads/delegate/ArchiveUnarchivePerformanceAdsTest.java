package ru.yandex.direct.api.v5.entity.ads.delegate;

import com.yandex.direct.api.v5.ads.ArchiveRequest;
import com.yandex.direct.api.v5.ads.ArchiveResponse;
import com.yandex.direct.api.v5.ads.UnarchiveRequest;
import com.yandex.direct.api.v5.ads.UnarchiveResponse;
import com.yandex.direct.api.v5.general.IdsCriteria;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.ads.converter.AdsArchiveUnarchiveRequestConverter;
import ru.yandex.direct.api.v5.entity.ads.validation.AdsArchiveUnarchiveRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannerArchiveUnarchiveService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.NewPerformanceBannerInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusarch;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusshow;
import ru.yandex.direct.dbutil.model.ClientId;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Api5Test
@RunWith(SpringRunner.class)
public class ArchiveUnarchivePerformanceAdsTest {

    @Autowired
    BannerArchiveUnarchiveService service;
    @Autowired
    AdsArchiveUnarchiveRequestValidator requestValidator;
    @Autowired
    AdsArchiveUnarchiveRequestConverter requestConverter;
    @Autowired
    ResultConverter resultConverter;
    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    FeatureService featureService;
    @Autowired
    BannerTypedRepository bannerTypedRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private GenericApiService genericApiService;
    @Autowired
    private TestBannerRepository testBannerRepository;

    private ArchiveAdsDelegate archiveAdsDelegate;
    private UnarchiveAdsDelegate unarchiveAdsDelegate;

    private NewPerformanceBannerInfo bannerInfo;
    private PerformanceAdGroupInfo adGroupInfo;
    private Long bannerId;
    private PerformanceBanner banner;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        bannerInfo = steps.performanceBannerSteps().createPerformanceBanner(
                new NewPerformanceBannerInfo()
                        .withAdGroupInfo(adGroupInfo));
        banner = bannerInfo.getBanner();
        bannerId = banner.getId();

        ApiAuthenticationSource auth = getApiAuthenticationSource(adGroupInfo.getUid(), adGroupInfo.getClientId());
        archiveAdsDelegate = new ArchiveAdsDelegate(service, auth, requestValidator, requestConverter, resultConverter,
                ppcPropertiesSupport, featureService);
        unarchiveAdsDelegate = new UnarchiveAdsDelegate(service, auth, requestValidator, requestConverter,
                resultConverter, ppcPropertiesSupport, featureService);
    }

    @Test
    public void archiveUnarchiveBanners_archiveSuccess_forPerformanceBanners() {
        checkState(banner.getStatusModerate() == BannerStatusModerate.YES);
        checkState(banner.getStatusPostModerate() == BannerStatusPostModerate.YES);
        checkState(!banner.getStatusArchived());
        testBannerRepository.updateStatusShow(bannerInfo.getShard(), banner.getId(), BannersStatusshow.No);

        ArchiveRequest request = new ArchiveRequest()
                .withSelectionCriteria(
                        new IdsCriteria()
                                .withIds(bannerId)
                );
        ArchiveResponse response = genericApiService.doAction(archiveAdsDelegate, request);

        var actual = bannerTypedRepository
                .getStrictlyFullyFilled(bannerInfo.getShard(), singletonList(bannerId), PerformanceBanner.class)
                .get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getArchiveResults().get(0).getErrors()).as("no errors")
                    .isEmpty();
            softly.assertThat(response.getArchiveResults().get(0).getId()).as("bannerId is equal")
                    .isEqualTo(bannerId);
            softly.assertThat(actual.getStatusArchived()).as("statusArchived").isTrue();
            // не трогаем статусы модерации при архивации
            softly.assertThat(actual.getStatusModerate()).as("statusModerate")
                    .isEqualTo(BannerStatusModerate.YES);
            softly.assertThat(actual.getStatusPostModerate()).as("statusPostModerate")
                    .isEqualTo(BannerStatusPostModerate.YES);
        });
    }

    @Test
    public void unarchiveArchiveBanners_unarchiveKeepModerationStatus_forPerformanceBanners() {
        checkState(banner.getStatusModerate() == BannerStatusModerate.YES);
        checkState(banner.getStatusPostModerate() == BannerStatusPostModerate.YES);
        testBannerRepository.updateStatusShow(bannerInfo.getShard(), banner.getId(), BannersStatusshow.No);
        testBannerRepository.updateStatusArchive(bannerInfo.getShard(), banner.getId(), BannersStatusarch.Yes);

        UnarchiveRequest request = new UnarchiveRequest()
                .withSelectionCriteria(
                        new IdsCriteria()
                                .withIds(bannerId)
                );
        UnarchiveResponse response = genericApiService.doAction(unarchiveAdsDelegate, request);

        var actual = bannerTypedRepository
                .getStrictlyFullyFilled(bannerInfo.getShard(), singletonList(bannerId), PerformanceBanner.class)
                .get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getUnarchiveResults().get(0).getErrors()).as("no errors")
                    .isEmpty();
            softly.assertThat(response.getUnarchiveResults().get(0).getId()).as("bannerId is equal")
                    .isEqualTo(bannerId);
            softly.assertThat(actual.getStatusArchived()).as("statusArchived").isFalse();
            // не трогаем статусы модерации при архивации
            softly.assertThat(actual.getStatusModerate()).as("statusModerate")
                    .isEqualTo(BannerStatusModerate.YES);
            softly.assertThat(actual.getStatusPostModerate()).as("statusPostModerate")
                    .isEqualTo(BannerStatusPostModerate.YES);
        });
    }

    private static ApiAuthenticationSource getApiAuthenticationSource(Long uid, ClientId clientId) {
        ApiUser user = new ApiUser()
                .withUid(uid)
                .withClientId(clientId);
        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(user);
        when(auth.getChiefOperator()).thenReturn(user);
        when(auth.getChiefSubclient()).thenReturn(user);
        when(auth.getSubclient()).thenReturn(user);
        return auth;
    }

}
