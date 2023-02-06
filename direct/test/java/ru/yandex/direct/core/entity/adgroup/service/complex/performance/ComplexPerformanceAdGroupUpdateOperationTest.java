package ru.yandex.direct.core.entity.adgroup.service.complex.performance;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexPerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.ModerationMode;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupUpdateOperationFactory;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceMainBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPathStartsWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexPerformanceAdGroupUpdateOperationTest {
    @Autowired
    private ComplexAdGroupUpdateOperationFactory updateOperationFactory;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    private GeoTree geoTree;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        geoTree = geoTreeFactory.getGlobalGeoTree();

        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        adGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(feedId);

        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.SMART_NO_CREATIVES, true);
    }

    @Test
    public void updatePerformanceAdGroup_updateEmpty_success() {
        ComplexPerformanceAdGroup complexAdGroup = new ComplexPerformanceAdGroup()
                .withAdGroup(adGroupInfo.getAdGroup());
        ComplexPerformanceAdGroupUpdateOperation operation =
                createOperation(singletonList(complexAdGroup), adGroupInfo.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();

        List<PerformanceBannerMain> banners = getBanners();
        assertSoftly(softly -> {
            softly.assertThat(result).is(matchedBy(isFullySuccessful()));
            softly.assertThat(banners).isEmpty();
        });
    }

    @Test
    public void updatePerformanceAdGroup_updateExisting_success() {
        NewPerformanceMainBannerInfo bannerInfo = steps.performanceMainBannerSteps()
                .createPerformanceMainBanner(adGroupInfo);
        String imageHash = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo()).getImageHash();

        ComplexPerformanceAdGroup complexAdGroup = new ComplexPerformanceAdGroup()
                .withAdGroup(adGroupInfo.getAdGroup())
                .withBanners(List.of(new PerformanceBannerMain()
                        .withId(bannerInfo.getBannerId())
                        .withLogoImageHash(imageHash)));
        ComplexPerformanceAdGroupUpdateOperation operation =
                createOperation(singletonList(complexAdGroup), adGroupInfo.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();

        List<PerformanceBannerMain> banners = getBanners();
        assertSoftly(softly -> {
            softly.assertThat(result).is(matchedBy(isFullySuccessful()));
            softly.assertThat(banners).hasSize(1);
            softly.assertThat(banners.get(0)).satisfies(banner -> {
                assertThat(banner).is(matchedBy(beanDiffer(bannerInfo.getBanner())
                        .useCompareStrategy(allFieldsExcept(newPath("lastChange"), newPath("statusBsSynced"),
                                newPath("logoImageHash"), newPath("logoStatusModerate")))));
                assertThat(banner.getLogoImageHash()).isEqualTo(imageHash);
            });
        });
    }

    @Test
    public void updatePerformanceAdGroup_addBannerToEmpty_success() {
        ComplexPerformanceAdGroup complexAdGroup = new ComplexPerformanceAdGroup()
                .withAdGroup(adGroupInfo.getAdGroup())
                .withBanners(List.of(new PerformanceBannerMain()));
        ComplexPerformanceAdGroupUpdateOperation operation =
                createOperation(singletonList(complexAdGroup), adGroupInfo.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();

        List<PerformanceBannerMain> banners = getBanners();
        assertSoftly(softly -> {
            softly.assertThat(result).is(matchedBy(isFullySuccessful()));
            softly.assertThat(banners).hasSize(1);
        });
    }

    @Test
    public void updatePerformanceAdGroup_addBannerToExisting_failure() {
        steps.performanceMainBannerSteps().createPerformanceMainBanner(adGroupInfo);

        ComplexPerformanceAdGroup complexAdGroup = new ComplexPerformanceAdGroup()
                .withAdGroup(adGroupInfo.getAdGroup())
                .withBanners(List.of(new PerformanceBannerMain()));
        ComplexPerformanceAdGroupUpdateOperation operation =
                createOperation(singletonList(complexAdGroup), adGroupInfo.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult()).satisfies(vr -> {
            assertThat(vr).is(matchedBy(hasDefectDefinitionWith(anyValidationErrorOnPathStartsWith(
                    path(index(0), field(ComplexPerformanceAdGroup.BANNERS), index(0))))));
            assertThat(vr.flattenErrors()).hasSize(1);
        });
    }

    @Test
    public void updatePerformanceAdGroup_addMultipleBannersToEmpty_failure() {
        ComplexPerformanceAdGroup complexAdGroup = new ComplexPerformanceAdGroup()
                .withAdGroup(adGroupInfo.getAdGroup())
                .withBanners(List.of(new PerformanceBannerMain(), new PerformanceBannerMain()));
        ComplexPerformanceAdGroupUpdateOperation operation =
                createOperation(singletonList(complexAdGroup), adGroupInfo.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult()).satisfies(vr -> {
            assertThat(vr).is(matchedBy(hasDefectDefinitionWith(anyValidationErrorOnPathStartsWith(
                    path(index(0), field(ComplexPerformanceAdGroup.BANNERS), index(0))))));
            assertThat(vr).is(matchedBy(hasDefectDefinitionWith(anyValidationErrorOnPathStartsWith(
                    path(index(0), field(ComplexPerformanceAdGroup.BANNERS), index(1))))));
            assertThat(vr.flattenErrors()).hasSize(2);
        });
    }

    private ComplexPerformanceAdGroupUpdateOperation createOperation(List<ComplexPerformanceAdGroup> complexAdGroups,
                                                                     ClientInfo clientInfo) {
        return updateOperationFactory.createComplexPerformanceAdGroupUpdateOperation(Applicability.FULL,
                complexAdGroups, geoTree, false, clientInfo.getUid(),
                Objects.requireNonNull(clientInfo.getClientId()), ModerationMode.FORCE_MODERATE);
    }

    private List<PerformanceBannerMain> getBanners() {
        return bannerTypedRepository.getBannersByGroupIds(adGroupInfo.getShard(), Set.of(adGroupInfo.getAdGroupId()),
                PerformanceBannerMain.class);
    }
}
