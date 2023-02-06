package ru.yandex.direct.core.entity.feature.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.repository.FeatureRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeatureCacheTest {
    @Autowired
    private FeatureSteps featureSteps;

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private FeatureCache featureCache;

    @Before
    public void before() {
        featureCache.invalidate();
        // initial caching
        featureCache.getCached();
    }

    @Test
    @Description("Кэш инвалидируется при вызове напрямую")
    public void invalidateCache_success() {
        var defaultFeature = featureSteps.getDefaultFeature();
        featureRepository.add(List.of(defaultFeature));

        featureCache.invalidate();

        var cachedFeaturesList = featureCache.getCached();
        var featuresList = featureRepository.get();
        assertThat(cachedFeaturesList).isNotEmpty();
        assertThat(featuresList).is(matchedBy(beanDiffer(cachedFeaturesList)));
    }

    @Test
    @Description("Кэш инвалидируется при операциях с фичами через сервисы")
    public void invalidateCacheIndirectly_success() {
        // addDefaultFeature calls featureManagingService.addFeatures, which invalidates cache after updating the db
        var defaultFeature = featureSteps.addDefaultFeature();
        var cachedDefaultFeature = featureCache.getCached(List.of(defaultFeature.getId())).get(0);
        assertThat(cachedDefaultFeature).isEqualTo(defaultFeature);
    }

    @Test
    @Description("Кэш не инвалидируется при операциях с фичами в обход сервисов")
    public void getCachedWithoutInvalidation_failure() {
        var cachedFeaturesCount = featureCache.getCached().size();

        var defaultFeature = featureSteps.getDefaultFeature();
        featureRepository.add(List.of(defaultFeature));

        var cachedWithoutInvalidationFeaturesCount = featureCache.getCached().size();
        assertThat(cachedFeaturesCount).isEqualTo(cachedWithoutInvalidationFeaturesCount);
    }
}
