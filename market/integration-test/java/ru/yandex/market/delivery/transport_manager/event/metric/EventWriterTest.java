package ru.yandex.market.delivery.transport_manager.event.metric;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityMetric;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.MetricEventType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.EntityMetricMapper;
import ru.yandex.market.delivery.transport_manager.service.health.event.EventWriter;
import ru.yandex.market.delivery.transport_manager.service.health.event.Metric;

public class EventWriterTest extends AbstractContextualTest {

    @Autowired
    EventWriter eventWriter;

    @Autowired
    EntityMetricMapper mapper;

    @Autowired
    TestableClock testableClock;

    @Test
    void fromLesToOutbound() {
        testableClock.setFixed(Instant.parse("2022-05-24T01:00:00Z"), ZoneId.systemDefault());
        eventWriter.log(1L, EntityType.TRANSPORTATION, MetricEventType.LES_CHECKPOINT_ARRIVED);
        testableClock.setFixed(Instant.parse("2022-05-24T01:01:00Z"), ZoneId.systemDefault());
        eventWriter.log(1L, EntityType.TRANSPORTATION, MetricEventType.COURIER_DATA_SENT);
        List<EntityMetric> entityMetrics = mapper.retrieveUnpublished();
        softly.assertThat(entityMetrics).isNotEmpty();
        softly.assertThat(entityMetrics.get(0)).isEqualTo(new EntityMetric(
            Metric.FROM_LES_TO_OUTBOUND,
            60L,
            EntityType.TRANSPORTATION,
            1L
        ));
    }

    @Test
    void fromLesToTracker() {
        testableClock.setFixed(Instant.parse("2022-05-24T01:00:00Z"), ZoneId.systemDefault());
        eventWriter.log(1L, EntityType.TRANSPORTATION, MetricEventType.LES_CHECKPOINT_ARRIVED);
        testableClock.setFixed(Instant.parse("2022-05-24T01:02:00Z"), ZoneId.systemDefault());
        eventWriter.log(1L, EntityType.TRANSPORTATION, MetricEventType.TRACKER_CHECKPOINT_ARRIVED);
        List<EntityMetric> entityMetrics = mapper.retrieveUnpublished();
        softly.assertThat(entityMetrics).isNotEmpty();
        softly.assertThat(entityMetrics.get(0)).isEqualTo(new EntityMetric(
            Metric.FROM_LES_TO_TRACKER,
            120L,
            EntityType.TRANSPORTATION,
            1L
        ));
    }
}
