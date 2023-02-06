package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.logistic_point_feature.FeatureType;
import ru.yandex.market.delivery.transport_manager.domain.entity.logistic_point_feature.LogisticPointFeature;
import ru.yandex.market.delivery.transport_manager.domain.filter.LogPointFeaturesFilter;

class LogisticPointFeatureMapperTest extends AbstractContextualTest {
    @Autowired
    private LogisticPointFeatureMapper mapper;

    @Test
    void shouldInsertAndThenFind() {
        mapper.insert(List.of(
            logisticPointFeature(1L, FeatureType.DROPOFF_WITH_ENABLED_RETURN),
            logisticPointFeature(2L, FeatureType.DROPOFF_WITH_ENABLED_RETURN),
            logisticPointFeature(3L, FeatureType.DROPOFF_WITH_ENABLED_RETURN)
        ));

        List<LogisticPointFeature> points = mapper.findByLogisticPointIdsAndFeatureType(
            List.of(1L, 3L),
            FeatureType.DROPOFF_WITH_ENABLED_RETURN);

        softly.assertThat(points).containsExactlyInAnyOrder(
            logisticPointFeature(1L, FeatureType.DROPOFF_WITH_ENABLED_RETURN).setId(1L),
            logisticPointFeature(3L, FeatureType.DROPOFF_WITH_ENABLED_RETURN).setId(3L)
        );
    }

    @Test
    @DatabaseSetup("/repository/dropoff/logistic_point_feature.xml")
    void findPointsByFeatureType() {
        Set<Long> points = mapper.findPointsByFeatureType(FeatureType.DROPOFF_WITH_ENABLED_RETURN);
        softly.assertThat(points).containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    @Test
    void shouldAddDeleteAndSearch() {
        mapper.add(101L, FeatureType.DROPOFF_WITH_ENABLED_RETURN);
        mapper.add(102L, FeatureType.DROPOFF_WITH_ENABLED_RETURN);
        mapper.add(103L, FeatureType.DROPOFF_WITH_ENABLED_RETURN);
        mapper.add(104L, FeatureType.DROPOFF_WITH_ENABLED_RETURN);
        mapper.add(105L, FeatureType.DROPOFF_WITH_ENABLED_RETURN);

        var pointIds = mapper.findPointsByFeatureType(FeatureType.DROPOFF_WITH_ENABLED_RETURN);
        softly.assertThat(pointIds).hasSize(5).containsExactlyInAnyOrder(101L, 102L, 103L, 104L, 105L);

        var points = mapper.searchFiltered(
            new LogPointFeaturesFilter().setLogisticPoint(103L),
            PageRequest.of(0, 10)
        );
        softly.assertThat(points).hasSize(1).contains(
            logisticPointFeature(103L, FeatureType.DROPOFF_WITH_ENABLED_RETURN).setId(3L)
        );

        var count = mapper.countFiltered(new LogPointFeaturesFilter().setLogisticPoint(103L));
        softly.assertThat(count).isEqualTo(1L);

        mapper.delete(Set.of(2L, 3L, 4L));
        pointIds = mapper.findPointsByFeatureType(FeatureType.DROPOFF_WITH_ENABLED_RETURN);
        softly.assertThat(pointIds).hasSize(2).containsExactlyInAnyOrder(101L, 105L);
    }

    private LogisticPointFeature logisticPointFeature(long logisticPointId, FeatureType featureType) {
        return new LogisticPointFeature().setLogisticsPointId(logisticPointId).setFeatureType(featureType);
    }
}
