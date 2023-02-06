package ru.yandex.market.delivery.transport_manager.repository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.StatusHistoryInfo;
import ru.yandex.market.delivery.transport_manager.repository.mappers.StatusHistoryMapper;

class StatusHistoryMapperTest extends AbstractContextualTest {

    @Autowired
    private StatusHistoryMapper mapper;

    private static final List<StatusHistoryInfo> TRANSPORTATION_STATUS_HISTORY =
        List.of(
            new StatusHistoryInfo()
                .setId(2L)
                .setEntityId(1L)
                .setType(EntityType.TRANSPORTATION)
                .setNewStatus("COULD_NOT_BE_MATCHED")
                .setOldStatus("SCHEDULED_WAITING_RESPONSE")
                .setChangedAt(toInstant("2020-12-30T14:56:40.515453")),
            new StatusHistoryInfo()
                .setId(1L)
                .setEntityId(1L)
                .setType(EntityType.TRANSPORTATION)
                .setNewStatus("SCHEDULED_WAITING_RESPONSE")
                .setOldStatus("SCHEDULED")
                .setChangedAt(toInstant("2020-12-30T14:46:40.515453")),
            new StatusHistoryInfo()
                .setId(3L)
                .setEntityId(1L)
                .setType(EntityType.TRANSPORTATION)
                .setNewStatus("SCHEDULED")
                .setOldStatus("SCHEDULED")
                .setChangedAt(toInstant("2020-12-30T14:06:40.515453"))
        );

    private static final List<StatusHistoryInfo> MOVEMENT_STATUS_HISTORY =
        List.of(
            new StatusHistoryInfo()
                .setId(5L)
                .setEntityId(4L)
                .setType(EntityType.MOVEMENT)
                .setNewStatus("CANCELLED")
                .setOldStatus("LGW_CREATED")
                .setChangedAt(toInstant("2020-12-30T14:56:40.515453")),
            new StatusHistoryInfo()
                .setId(4L)
                .setEntityId(4L)
                .setType(EntityType.MOVEMENT)
                .setNewStatus("LGW_CREATED")
                .setOldStatus("LGW_SENT")
                .setChangedAt(toInstant("2020-12-30T14:46:40.515453")),
            new StatusHistoryInfo()
                .setId(6L)
                .setEntityId(4L)
                .setType(EntityType.MOVEMENT)
                .setNewStatus("LGW_SENT")
                .setOldStatus("NEW")
                .setChangedAt(toInstant("2020-12-30T14:06:40.515453"))
        );

    private static final List<StatusHistoryInfo> OUTBOUND_UNIT_STATUS_HISTORY =
        List.of(
            new StatusHistoryInfo()
                .setId(8L)
                .setEntityId(2L)
                .setType(EntityType.TRANSPORTATION_UNIT)
                .setNewStatus("PROCESSED")
                .setOldStatus("ACCEPTED")
                .setChangedAt(toInstant("2020-12-30T14:56:40.515453")),
            new StatusHistoryInfo()
                .setId(7L)
                .setEntityId(2L)
                .setType(EntityType.TRANSPORTATION_UNIT)
                .setNewStatus("ACCEPTED")
                .setOldStatus("SENT")
                .setChangedAt(toInstant("2020-12-30T14:46:40.515453")),
            new StatusHistoryInfo()
                .setId(9L)
                .setEntityId(2L)
                .setType(EntityType.TRANSPORTATION_UNIT)
                .setNewStatus("SENT")
                .setOldStatus("NEW")
                .setChangedAt(toInstant("2020-12-30T14:06:40.515453"))
        );

    private static final List<StatusHistoryInfo> INBOUND_UNIT_STATUS_HISTORY =
        List.of(
            new StatusHistoryInfo()
                .setId(11L)
                .setEntityId(3L)
                .setType(EntityType.TRANSPORTATION_UNIT)
                .setNewStatus("PROCESSED")
                .setOldStatus("ACCEPTED")
                .setChangedAt(toInstant("2020-12-30T14:56:40.515453")),
            new StatusHistoryInfo()
                .setId(10L)
                .setEntityId(3L)
                .setType(EntityType.TRANSPORTATION_UNIT)
                .setNewStatus("ACCEPTED")
                .setOldStatus("SENT")
                .setChangedAt(toInstant("2020-12-30T14:46:40.515453")),
            new StatusHistoryInfo()
                .setId(12L)
                .setEntityId(3L)
                .setType(EntityType.TRANSPORTATION_UNIT)
                .setNewStatus("SENT")
                .setOldStatus("NEW")
                .setChangedAt(toInstant("2020-12-30T14:06:40.515453"))
        );

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-02-01T21:00:00.00Z"), ZoneOffset.UTC);
    }

    @DatabaseSetup({
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/transportation/transportation_status_history.xml",
    })
    @Test
    void getTransportationHistory() {
        List<StatusHistoryInfo> transportationHistory =
            mapper.find(EntityType.TRANSPORTATION, 1L);
        softly.assertThat(transportationHistory).isEqualTo(TRANSPORTATION_STATUS_HISTORY);
    }

    @DatabaseSetup({
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/transportation/transportation_status_history.xml",
    })
    @Test
    void getInboundUnitHistory() {
        List<StatusHistoryInfo> inboundUnitHistory =
            mapper.find(EntityType.TRANSPORTATION_UNIT, 3L);
        softly.assertThat(inboundUnitHistory).isEqualTo(INBOUND_UNIT_STATUS_HISTORY);
    }

    @DatabaseSetup({
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/transportation/transportation_status_history.xml",
    })
    @Test
    void getOutboundUnitHistory() {
        List<StatusHistoryInfo> outboundUnitHistory = mapper.find(EntityType.TRANSPORTATION_UNIT, 2L);
        softly.assertThat(outboundUnitHistory).isEqualTo(OUTBOUND_UNIT_STATUS_HISTORY);
    }

    @DatabaseSetup({
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/transportation/transportation_status_history.xml",
    })
    @Test
    void getMovementHistory() {
        List<StatusHistoryInfo> movementHistory = mapper.find(EntityType.MOVEMENT, 4L);
        softly.assertThat(movementHistory).isEqualTo(MOVEMENT_STATUS_HISTORY);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/transportation_tasks.xml",
        "/repository/transportation_task/transportation_task_history.xml"
    })
    void getTransportationTaskHistory() {
        List<StatusHistoryInfo> history =
            mapper.find(EntityType.TRANSPORTATION_TASK, 3L);

        StatusHistoryInfo first = new StatusHistoryInfo()
            .setId(2L)
            .setEntityId(3L)
            .setType(EntityType.TRANSPORTATION_TASK)
            .setChangedAt(toInstant("2021-02-01T16:46:40.515453"))
            .setOldStatus("ENRICHED")
            .setNewStatus("VALIDATING");

        StatusHistoryInfo third = new StatusHistoryInfo()
            .setId(3L)
            .setEntityId(3L)
            .setType(EntityType.TRANSPORTATION_TASK)
            .setChangedAt(toInstant("2021-02-01T14:46:40.515453"))
            .setOldStatus("NEW")
            .setNewStatus("ENRICHING");

        StatusHistoryInfo second = new StatusHistoryInfo()
            .setId(1L)
            .setEntityId(3L)
            .setType(EntityType.TRANSPORTATION_TASK)
            .setChangedAt(toInstant("2021-02-01T15:46:40.515453"))
            .setOldStatus("ENRICHING")
            .setNewStatus("ENRICHED");

        softly.assertThat(history).containsExactly(first, second, third);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transportation_tasks.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_status_history_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateTransportationTaskStatusHistory() {
        mapper.save(
            new StatusHistoryInfo()
                .setEntityId(1L)
                .setOldStatus("NEW")
                .setNewStatus("ERROR")
                .setType(EntityType.TRANSPORTATION_TASK)
        );
        mapper.save(
            new StatusHistoryInfo()
                .setEntityId(3L)
                .setOldStatus("VALIDATING")
                .setNewStatus("ERROR")
                .setType(EntityType.TRANSPORTATION_TASK)
        );
    }

    @DatabaseSetup("/repository/logbroker/unpublished_status_history.xml")
    @Test
    void listUnpublishedWithLimit() {
        List<StatusHistoryInfo> history = mapper.findUnpublished(
            1
        );
        softly.assertThat(history).containsExactly(
            new StatusHistoryInfo()
                .setId(1L)
                .setEntityId(1L)
                .setType(EntityType.TRANSPORTATION)
                .setNewStatus("SCHEDULED_WAITING_RESPONSE")
                .setOldStatus("SCHEDULED")
                .setChangedAt(toInstant("2020-12-30T14:46:40.515453"))
        );
    }

    @DatabaseSetup("/repository/logbroker/unpublished_status_history.xml")
    @ExpectedDatabase(
        value = "/repository/logbroker/after/unpublished_status_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetPublished() {
        mapper.setPublished(List.of(1L));
    }

    @DatabaseSetup("/repository/logbroker/unpublished_status_history.xml")
    @ExpectedDatabase(
        value = "/repository/logbroker/after/unpublished_status_history_after_clean.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void cleanBefore() {
        mapper.cleanBefore(toInstant("2020-12-30T14:50:00.0"));
    }

    @DatabaseSetup("/repository/logbroker/unpublished_status_history.xml")
    @Test
    void unpublishedCountOlderThen() {
        softly.assertThat(mapper.unpublishedCountOlderThen(toInstant("2020-12-30T14:46:41.0"))).isEqualTo(2);
        softly.assertThat(mapper.unpublishedCountOlderThen(toInstant("2020-12-30T14:46:40.0"))).isEqualTo(1);
        softly.assertThat(mapper.unpublishedCountOlderThen(toInstant("2020-12-30T14:06:00.0"))).isEqualTo(0);
    }

    @DatabaseSetup("/repository/logbroker/unpublished_status_history.xml")
    @Test
    void detectPublishingWrongOrder() {
        softly.assertThat(mapper.detectPublishingWrongOrder()).isTrue();
    }

    @DatabaseSetup("/repository/transportation/transportation_status_history.xml")
    @Test
    void detectPublishingOrderAllUnpublished() {
        softly.assertThat(mapper.detectPublishingWrongOrder()).isFalse();
    }

    @DatabaseSetup("/repository/logbroker/unpublished_status_history_correct_publishing_order.xml")
    @Test
    void detectPublishingCorrectOrder() {
        softly.assertThat(mapper.detectPublishingWrongOrder()).isFalse();
    }

    @DatabaseSetup({
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/transportation/transportation_status_history.xml",
    })
    @Test
    void findLastChangedMultiple() {
        Map<EntityType, Set<Long>> entityMap = Map.of(
            EntityType.MOVEMENT, Set.of(4L),
            EntityType.TRANSPORTATION_UNIT, Set.of(2L, 3L)
        );
        List<StatusHistoryInfo> lastChanged = mapper.findLastChangedMultiple(entityMap);
        softly.assertThat(lastChanged).extracting(StatusHistoryInfo::getId).containsExactlyInAnyOrder(5L, 8L, 11L);
    }

    @DatabaseSetup({
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/transportation/transportation_status_history.xml",
    })
    @Test
    void findLastChanged() {
        StatusHistoryInfo lastChanged = mapper.findLastChanged(EntityType.MOVEMENT, 4L);
        softly.assertThat(lastChanged.getId()).isEqualTo(5L);
    }

    @DatabaseSetup({
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/transportation/transportation_status_history.xml",
    })
    @Test
    void findAllByType() {
        List<StatusHistoryInfo> allByType = mapper.findAllByType(EntityType.TRANSPORTATION, List.of(1L));
        softly.assertThat(allByType).containsExactlyInAnyOrderElementsOf(TRANSPORTATION_STATUS_HISTORY);
    }

    @DatabaseSetup("/repository/movement/movement_status_history.xml")
    @Test
    void findByStatuses() {
        clock.setFixed(Instant.parse("2021-07-11T22:46:40.515453Z"), ZoneId.ofOffset("UTC", ZoneOffset.UTC));

        List<StatusHistoryInfo> historyInfos = mapper.findByStatuses(
            EntityType.MOVEMENT,
            Set.of(MovementStatus.PARTNER_CREATED),
            Set.of(MovementStatus.COURIER_FOUND, MovementStatus.CONFIRMED),
            clock.instant(),
            clock.instant().atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).plusDays(5).toInstant()
        );

        softly.assertThat(historyInfos).containsExactlyInAnyOrder(
            new StatusHistoryInfo()
                .setId(5L)
                .setEntityId(3L)
                .setOldStatus("LGW_SENT")
                .setNewStatus("PARTNER_CREATED")
                .setType(EntityType.MOVEMENT)
                .setChangedAt(Instant.parse("2021-07-13T14:46:40.515453Z")),
            new StatusHistoryInfo()
                .setId(6L)
                .setEntityId(4L)
                .setOldStatus("LGW_SENT")
                .setNewStatus("PARTNER_CREATED")
                .setType(EntityType.MOVEMENT)
                .setChangedAt(Instant.parse("2021-07-13T22:46:40.515453Z")),
            new StatusHistoryInfo()
                .setId(7L)
                .setEntityId(5L)
                .setOldStatus("LGW_SENT")
                .setNewStatus("PARTNER_CREATED")
                .setType(EntityType.MOVEMENT)
                .setChangedAt(Instant.parse("2021-07-13T22:47:40.515453Z"))
        );
    }

    @ExpectedDatabase(
            value = "/repository/transportation/expected_transportation_status_history_null_trace.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @Test
    void saveStatusHistoryWithNullTraceId() {
        clock.setFixed(Instant.parse("2020-12-30T14:46:40.515453Z"), ZoneId.ofOffset("UTC", ZoneOffset.UTC));

        mapper.insertAll(TRANSPORTATION_STATUS_HISTORY);
        mapper.save(new StatusHistoryInfo()
                .setEntityId(1L)
                .setType(EntityType.TRANSPORTATION)
                .setNewStatus("COULD_NOT_BE_MATCHED")
                .setOldStatus("SCHEDULED_WAITING_RESPONSE"));
    }

    @Test
    @ExpectedDatabase(
            value = "/repository/transportation/expected_transportation_status_history_not_null_trace.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void saveStatusHistoryWithNonNullTraceId() {
        mapper.save(new StatusHistoryInfo()
                .setEntityId(1L)
                .setType(EntityType.TRANSPORTATION)
                .setNewStatus("COULD_NOT_BE_MATCHED")
                .setOldStatus("SCHEDULED_WAITING_RESPONSE")
                .setTraceRequestId("TRACE"));
        mapper.insertAll(List.of(
                new StatusHistoryInfo()
                    .setEntityId(1L)
                    .setType(EntityType.TRANSPORTATION)
                    .setNewStatus("COULD_NOT_BE_MATCHED")
                    .setOldStatus("SCHEDULED_WAITING_RESPONSE")
                    .setTraceRequestId("TRACE2")
        ));
    }
}
