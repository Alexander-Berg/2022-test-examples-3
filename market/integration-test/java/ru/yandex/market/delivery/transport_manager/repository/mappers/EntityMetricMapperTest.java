package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityMetric;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.service.health.event.Metric;

public class EntityMetricMapperTest extends AbstractContextualTest {

    @Autowired
    private EntityMetricMapper eventMapper;

    @Test
    @ExpectedDatabase(
        value = "/service/event/entity/entity_event_data.xml", assertionMode =
        DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insertData() {
        eventMapper.addStart(
            1L,
            EntityType.TRANSPORTATION,
            Metric.FROM_LES_TO_OUTBOUND,
            toInstant("2022-05-05T14:00:00")
        );
        eventMapper.addEnd(
            1L,
            EntityType.TRANSPORTATION,
            Metric.FROM_LES_TO_OUTBOUND,
            toInstant("2022-05-05T14:02:00")
        );

        eventMapper.addStart(
            1L,
            EntityType.TRANSPORTATION,
            Metric.FROM_LES_TO_TRACKER,
            toInstant("2022-05-05T14:00:00")
        );
        eventMapper.addEnd(
            1L,
            EntityType.TRANSPORTATION,
            Metric.FROM_LES_TO_TRACKER,
            toInstant("2022-05-05T14:10:00")
        );

        eventMapper.addStart(
            2L,
            EntityType.TRANSPORTATION,
            Metric.FROM_LES_TO_OUTBOUND,
            toInstant("2022-05-05T14:01:00")
        );

        // запись идемпотентна: данные не перезапишутся
        eventMapper.addStart(
            1L,
            EntityType.TRANSPORTATION,
            Metric.FROM_LES_TO_TRACKER,
            toInstant("2022-05-05T16:01:00")
        );
        eventMapper.addEnd(
            1L,
            EntityType.TRANSPORTATION,
            Metric.FROM_LES_TO_TRACKER,
            toInstant("2022-05-05T16:01:00")
        );
    }

    @Test
    @DatabaseSetup(value = "/service/event/entity/entity_event_data.xml")
    @ExpectedDatabase(
        value = "/service/event/entity/entity_event_data_deleted.xml", assertionMode =
        DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void retrieveData() {
        var actual = eventMapper.retrieveUnpublished();
        softly.assertThat(actual).containsExactlyInAnyOrder(
            new EntityMetric(Metric.FROM_LES_TO_OUTBOUND, 120L, EntityType.TRANSPORTATION, 1L),
            new EntityMetric(Metric.FROM_LES_TO_TRACKER, 600L, EntityType.TRANSPORTATION, 1L)
        );
    }

    @Test
    @DatabaseSetup(value = "/service/event/entity/entity_event_data.xml")
    void checkHanging() {
        clock.setFixed(Instant.parse("2023-01-01T10:39:00.00Z"), ZoneOffset.UTC);
        var actual = eventMapper.countUnmatched(clock.instant());
        softly.assertThat(actual).isEqualTo(1);
        clock.setFixed(Instant.parse("2022-01-01T10:39:00.00Z"), ZoneOffset.UTC);
        var actualNoneMatching = eventMapper.countUnmatched(clock.instant());
        softly.assertThat(actualNoneMatching).isEqualTo(0);

    }
}
