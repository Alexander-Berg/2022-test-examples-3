package ru.yandex.market.delivery.transport_manager.repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.axapta.AxaptaEventFilter;
import ru.yandex.market.delivery.transport_manager.domain.entity.AxaptaEvent;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.axapta.AxaptaEventMapper;

class AxaptaEventMapperTest extends AbstractContextualTest {
    @Autowired
    private AxaptaEventMapper mapper;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-02-01T21:00:00.00Z"), ZoneOffset.UTC);
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event_correct_publishing_order.xml")
    @ExpectedDatabase(
        value = "/repository/logbroker/after/unpublished_axapta_event_correct_publishing_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetPublished() {
        mapper.setPublished(List.of(2L));
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event_correct_publishing_order.xml")
    @Test
    void listUnpublishedWithLimit() {
        List<AxaptaEvent> history = mapper.findUnpublished(
            1
        );
        softly.assertThat(history).containsExactly(
            new AxaptaEvent()
                .setId(2L)
                .setTransportationId(1L)
                .setTransportationType(TransportationType.INTERWAREHOUSE)
                .setTransportationUnitId(1L)
                .setRegisterId(1L)
                .setType(AxaptaEvent.Type.OUTBOUND_FACT)
                .setChangedAt(toInstant("2020-12-30T14:46:40.515453"))
        );
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event_correct_publishing_order.xml")
    @Test
    void listUnpublished() {
        List<AxaptaEvent> history = mapper.findUnpublished(
            2
        );
        softly.assertThat(history).containsExactly(
            new AxaptaEvent()
                .setId(2L)
                .setTransportationId(1L)
                .setTransportationType(TransportationType.INTERWAREHOUSE)
                .setTransportationUnitId(1L)
                .setRegisterId(1L)
                .setType(AxaptaEvent.Type.OUTBOUND_FACT)
                .setChangedAt(toInstant("2020-12-30T14:46:40.515453")),
            new AxaptaEvent()
                .setId(3L)
                .setTransportationId(1L)
                .setTransportationType(TransportationType.INTERWAREHOUSE)
                .setTransportationUnitId(2L)
                .setRegisterId(2L)
                .setType(AxaptaEvent.Type.INBOUND_FACT)
                .setChangedAt(toInstant("2020-12-30T14:56:40.515453"))
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/logbroker/after/saved_axapta_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTransportationTaskStatusHistory() {
        mapper.save(
            new AxaptaEvent()
                .setTransportationId(1L)
                .setTransportationType(TransportationType.INTERWAREHOUSE)
                .setTransportationUnitId(1L)
                .setRegisterId(1L)
                .setType(AxaptaEvent.Type.OUTBOUND_FACT)
                .setChangedAt(toInstant("2020-12-30T14:46:40.515453"))
        );
        mapper.save(
            new AxaptaEvent()
                .setTransportationId(1L)
                .setTransportationType(TransportationType.INTERWAREHOUSE)
                .setTransportationUnitId(2L)
                .setRegisterId(2L)
                .setType(AxaptaEvent.Type.INBOUND_FACT)
                .setChangedAt(toInstant("2020-12-30T14:56:40.515453"))
        );
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event_correct_publishing_order.xml")
    @ExpectedDatabase(
        value = "/repository/logbroker/after/unpublished_axapta_event_after_clean.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void cleanBefore() {
        mapper.cleanBefore(toInstant("2020-12-30T14:50:00.0"));
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event.xml")
    @Test
    void unpublishedCountOlderThen() {
        softly.assertThat(mapper.unpublishedCountOlderThen(toInstant("2020-12-30T14:06:00.0"))).isEqualTo(0);
        softly.assertThat(mapper.unpublishedCountOlderThen(toInstant("2020-12-30T14:46:40.0"))).isEqualTo(1);
        softly.assertThat(mapper.unpublishedCountOlderThen(toInstant("2020-12-30T14:46:41.0"))).isEqualTo(2);
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event.xml")
    @Test
    void detectPublishingWrongOrder() {
        softly.assertThat(mapper.detectPublishingWrongOrder()).isTrue();
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event_correct_publishing_order.xml")
    @Test
    void detectPublishingCorrectOrder() {
        softly.assertThat(mapper.detectPublishingWrongOrder()).isFalse();
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event.xml")
    @Test
    void countByTypeAndTransportationId() {
        softly.assertThat(mapper.count(
                new AxaptaEventFilter()
                    .setTransportationId(1L)
                    .setType(AxaptaEvent.Type.NEW_TRANSPORTATION))
            )
            .isEqualTo(1L);
    }

    @DatabaseSetup("/repository/logbroker/unpublished_axapta_event.xml")
    @Test
    void countByTypeAndTransportationIdZero() {
        softly.assertThat(mapper.count(
                new AxaptaEventFilter()
                    .setTransportationId(2L)
                    .setType(AxaptaEvent.Type.NEW_TRANSPORTATION))
            )
            .isEqualTo(0L);
    }
}
