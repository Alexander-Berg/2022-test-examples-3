package ru.yandex.direct.core.entity.feature.repository;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.FeatureSteps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Preconditions.checkState;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeatureRepositoryTest {
    @Autowired
    private FeatureSteps featureSteps;

    @Autowired
    private FeatureRepository featureRepository;

    @Test
    public void add_success() {
        var feature = featureSteps.getDefaultFeature();
        featureRepository.add(List.of(feature));
        var addedFeature = featureRepository.get(List.of(feature.getId())).get(0);
        assertThat(addedFeature).isEqualTo(feature);
    }

    @Test
    public void delete_success() {
        var feature = featureSteps.getDefaultFeature();
        featureRepository.add(List.of(feature));
        checkState(!featureRepository.get(List.of(feature.getId())).isEmpty(), "Фича успешно добавлена");

        featureRepository.delete(feature.getId());
        var deletedFeature = featureRepository.get(List.of(feature.getId()));
        assertThat(deletedFeature).isEmpty();
    }
}
