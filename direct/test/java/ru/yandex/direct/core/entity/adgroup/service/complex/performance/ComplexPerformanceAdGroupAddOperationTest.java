package ru.yandex.direct.core.entity.adgroup.service.complex.performance;

import java.util.List;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexPerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupAddOperationFactory;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultPerformanceAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPathStartsWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexPerformanceAdGroupAddOperationTest {
    @Autowired
    private ComplexAdGroupAddOperationFactory addOperationFactory;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    private GeoTree geoTree;
    private CampaignInfo campaignInfo;
    private FeedInfo feedInfo;

    @Before
    public void before() {
        geoTree = geoTreeFactory.getGlobalGeoTree();

        campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        feedInfo = steps.feedSteps().createDefaultFeed(campaignInfo.getClientInfo());

        steps.featureSteps().addClientFeature(campaignInfo.getClientId(), FeatureName.SMART_NO_CREATIVES, true);
    }

    @Test
    public void addPerformanceAdGroup_noBanners_success() {
        ComplexPerformanceAdGroup complexAdGroup = new ComplexPerformanceAdGroup()
                .withAdGroup(defaultPerformanceAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId()));
        ComplexPerformanceAdGroupAddOperation operation = createOperation(complexAdGroup);
        MassResult<Long> result = operation.prepareAndApply();

        List<PerformanceBannerMain> banners = getBanners();
        assertSoftly(softly -> {
            softly.assertThat(result).is(matchedBy(isFullySuccessful()));
            softly.assertThat(banners).isEmpty();
        });
    }

    @Test
    public void addPerformanceAdGroup_oneBanner_success() {
        ComplexPerformanceAdGroup complexAdGroup = new ComplexPerformanceAdGroup()
                .withAdGroup(defaultPerformanceAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId()))
                .withBanners(List.of(new PerformanceBannerMain()));
        ComplexPerformanceAdGroupAddOperation operation = createOperation(complexAdGroup);
        MassResult<Long> result = operation.prepareAndApply();

        List<PerformanceBannerMain> banners = getBanners();
        assertSoftly(softly -> {
            softly.assertThat(result).is(matchedBy(isFullySuccessful()));
            softly.assertThat(banners).hasSize(1);
        });
    }

    @Test
    public void addPerformanceAdGroup_multipleBanners_failure() {
        ComplexPerformanceAdGroup complexAdGroup = new ComplexPerformanceAdGroup()
                .withAdGroup(defaultPerformanceAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId()))
                .withBanners(List.of(new PerformanceBannerMain(), new PerformanceBannerMain()));
        ComplexPerformanceAdGroupAddOperation operation = createOperation(complexAdGroup);
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult()).satisfies(vr -> {
            assertThat(vr).is(matchedBy(hasDefectDefinitionWith(anyValidationErrorOnPathStartsWith(
                    path(index(0), field(ComplexPerformanceAdGroup.BANNERS))))));
            assertThat(vr.flattenErrors()).hasSize(1);
        });
    }

    private ComplexPerformanceAdGroupAddOperation createOperation(ComplexPerformanceAdGroup complexAdGroup) {
        return addOperationFactory.createPerformanceAdGroupAddOperation(Applicability.FULL, false, List.of(complexAdGroup),
                geoTree, campaignInfo.getUid(), Objects.requireNonNull(campaignInfo.getClientId()));
    }

    private List<PerformanceBannerMain> getBanners() {
        return bannerTypedRepository.getBannersByCampaignIdsAndClass(campaignInfo.getShard(),
                List.of(campaignInfo.getCampaignId()), PerformanceBannerMain.class);
    }
}
