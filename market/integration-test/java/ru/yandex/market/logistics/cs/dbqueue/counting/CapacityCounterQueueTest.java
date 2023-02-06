package ru.yandex.market.logistics.cs.dbqueue.counting;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener.LifecycleEventType;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener.QueueCoordinates;
import ru.yandex.market.logistics.cs.dbqueue.dayoff.bycapacity.ServiceToDayByCapacityProducer;
import ru.yandex.market.logistics.cs.dbqueue.valuecounter.CapacityCounterRecalculationPayload;
import ru.yandex.market.logistics.cs.domain.jdbc.VersionedServiceCapacityMapping;
import ru.yandex.market.logistics.cs.domain.jdbc.VersionedServiceCapacityMappingTuple;
import ru.yandex.market.logistics.cs.facade.NotificationsFacade;
import ru.yandex.market.logistics.cs.service.CapacityValueCounterService;
import ru.yandex.market.logistics.cs.service.LmsSnapshotService;
import ru.yandex.money.common.dbqueue.api.QueueConsumer;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.cs.dbqueue.common.SingleQueueShardRouter.MASTER;
import static ru.yandex.money.common.dbqueue.api.TaskExecutionResult.Type.FINISH;

@DisplayName("Очередь recalculate_capacity_value_counter")
@DatabaseSetup("/repository/counting/before/base_recounting.xml")
class CapacityCounterQueueTest extends AbstractRecountingTest {
    private static final QueueCoordinates COUNTER_QUEUE_COORDINATES = new QueueCoordinates(
        MASTER,
        ru.yandex.market.logistics.cs.config.dbqueue.CapacityCounterQueueConfig.QUEUE_LOCATION
    );
    private static final QueueCoordinates DAY_OFF_QUEUE_COORDINATES = new QueueCoordinates(
        MASTER,
        ru.yandex.market.logistics.cs.config.dbqueue.DayOffByCapacityQueueConfiguration.QUEUE_LOCATION
    );

    @Autowired
    private QueueConsumer<CapacityCounterRecalculationPayload> consumer;

    @Autowired
    private CapacityValueCounterService service;

    @Autowired
    private AccountingTaskLifecycleListener taskLifecycleListener;

    @Autowired
    private ServiceToDayByCapacityProducer serviceToDayByCapacityProducer;

    @Autowired
    private LmsSnapshotService lmsSnapshotService;

    @MockBean
    private NotificationsFacade notificationsFacade;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setUp() {
        taskLifecycleListener.reset();
        clock.setFixed(
            LocalDateTime.now(ZoneId.of("UTC"))
                .withHour(12)
                .withMinute(0)
                .truncatedTo(ChronoUnit.MINUTES)
                .toInstant(ZoneOffset.UTC),
            ZoneId.of("UTC")
        );
    }

    @AfterEach
    void tearDown() {
        clock.clearFixed();
    }

    /**
     * service_id 20:
     * capacity_value_counter 22: +1
     * capacity_value_counter 23: -1
     * <p>
     * service_id 30:
     * capacity_value_counter 31: +1
     * capacity_value_counter 35: +1
     * <p>
     * service_id 40:
     * capacity_value_counter 31: +1
     * capacity_value_counter 35: +1
     * capacity_value_counter 32: -1
     * capacity_value_counter 33: -1
     * capacity_value_counter 21: -1
     * capacity_value_counter 25: -1
     * capacity_value_counter 22: +1
     * capacity_value_counter 23: +1
     * <p>
     * Операции в порядке их выполнения: service_id - capacity_id : [cvc_id diff => propagated (cvc.id diff)]*
     * S30 - C3 : 31 +1 => 5 +1 (add)
     * S40 - C2 : 21 -1 => 5 -1, 22 +1 => 3 +1, 23 +1 => 11 +1 (update add)
     * S20 - C2 : 22 +1 => 3 +1, 23 -1 => 11 -1; (unique delete)
     * S40 - C3 : 31 +1 => 5 +1, 32 -1 => 3 -1, 33 -1 => 11 -1. (unique delete)
     */
    @Test
    @DisplayName("Снепшот из лмс приводит к пересчету счетчиков")
    @ExpectedDatabase(
        value = "/repository/counting/after/full_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void wholeQueueFlowTest() {
        List<VersionedServiceCapacityMapping> expectedToDelete = List.of(
            versionedMapping(2L, SERVICE_ID20, CAPACITY_ID2, 6L)
        );
        List<VersionedServiceCapacityMappingTuple> expectedToUpdate = List.of(new VersionedServiceCapacityMappingTuple(
            versionedMapping(4L, SERVICE_ID40, CAPACITY_ID3, 2L),
            versionedMapping(4L, SERVICE_ID40, CAPACITY_ID2, 2L)
        ));
        List<VersionedServiceCapacityMapping> expectedToAdd = List.of(
            versionedMapping(3L, SERVICE_ID30, CAPACITY_ID3, 6L)
        );

        lmsSnapshotService.snapshot();
        await().atMost(Duration.ofMinutes(1))
            .until(() -> containsTaskLifecycleEventOfType(1, COUNTER_QUEUE_COORDINATES));

        verify(service).recalculateForAdding(eq(expectedToAdd), eq(false));

        verify(service).recalculateForUpdating(eq(Set.of()), eq(List.of(expectedToUpdate.get(0).getNewMapping())));
        verify(service).recalculateForUpdating(eq(Set.of()), eq(List.of(expectedToUpdate.get(0).getNewMapping())));

        verify(service).recalculateForRemoving(
            eq(List.of(expectedToDelete.get(0), expectedToUpdate.get(0).getOldMapping())),
            eq(true)
        );
    }

    /**
     * 21 - снимается.
     * <p>
     * 31, 37 - щелкаются из-за превышения трешхолда после пересчета.
     * <p>
     * 33, 22 - не изменяются, но переносятся на новые сервисы (30, 20), т.к. там уже дейофф.
     * <p>
     * Операции в порядке их выполнения: service_id - capacity_id : [cvc_id diff => propagated (cvc.id diff)]*
     * S30 - C3 : 31 +1 => 5 +1, 37 +1 => 8 +1, 33 +1 => 11 +1 (add)
     * S40 - C2 : 21 -1 => 5 -1, 22 +1 => 3 +1, 23 +1 => 11 +1 (update add)
     * S20 - C3 : 32 -1 => 3 -1, 37 +1 => 8 +1, 33 +2 => 11 +2 (unique add)
     * S20 - C2 : 22 +1 => 3 +1, 23 -1 => 11 -1; (unique delete)
     * S40 - C3 : 31 +1 => 5 +1, 32 -1 => 3 -1, 33 -1 => 11 -1. (unique delete)
     */
    @Test
    @DisplayName("При пересчете маппинга выставляются и снимаются дейоффы, если это необходимо")
    @DatabaseSetup(
        value = "/repository/counting/before/value_counters_with_dayoff.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/counting/after/day_off_recalculating_after_change_mappings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeDayOffStatusAfterRecalculationTest() {
        List<VersionedServiceCapacityMapping> expectedToDelete = List.of(
            versionedMapping(1L, SERVICE_ID20, CAPACITY_ID2, 6L)
        );
        List<VersionedServiceCapacityMappingTuple> expectedToUpdate = List.of(new VersionedServiceCapacityMappingTuple(
            versionedMapping(2L, SERVICE_ID40, CAPACITY_ID3, 2L),
            versionedMapping(2L, SERVICE_ID40, CAPACITY_ID2, 2L)
        ));
        List<VersionedServiceCapacityMapping> expectedToAdd = List.of(
            versionedMapping(4L, SERVICE_ID30, CAPACITY_ID3, 7L),
            versionedMapping(6L, SERVICE_ID20, CAPACITY_ID3, 7L)
        );

        CapacityCounterRecalculationPayload payload = new CapacityCounterRecalculationPayload(
            expectedToDelete,
            expectedToUpdate,
            expectedToAdd
        );
        consumer.execute(createTask(payload));
        await().atMost(Duration.ofMinutes(1))
            .until(() -> containsTaskLifecycleEventOfType(5, DAY_OFF_QUEUE_COORDINATES));
        verify(serviceToDayByCapacityProducer, times(5)).enqueue(any(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Дейоффы пропагируются на новый сервис, если по сервису еще нет евентов")
    @DatabaseSetup(
        value = "/repository/counting/before/days_off_propagate_to_new_service.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/counting/after/days_off_propagate_to_new_service.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void daysOffPropagateToNewService() {
        List<VersionedServiceCapacityMapping> expectedToAdd = List.of(
            versionedMapping(3L, SERVICE_ID50, CAPACITY_ID2, 1L)
        );

        CapacityCounterRecalculationPayload payload = new CapacityCounterRecalculationPayload(
            List.of(),
            List.of(),
            expectedToAdd
        );
        consumer.execute(createTask(payload));
        await().atMost(Duration.ofMinutes(1))
            .until(() -> containsTaskLifecycleEventOfType(2, DAY_OFF_QUEUE_COORDINATES));
        verify(serviceToDayByCapacityProducer, times(2)).enqueue(any(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Пересчет только для удаления")
    void onlyDelete() {
        List<VersionedServiceCapacityMapping> expectedToDelete = List.of(
            versionedMapping(2L, SERVICE_ID50, CAPACITY_ID3, 6L),
            versionedMapping(10L, SERVICE_ID50, CAPACITY_ID4, 6L)
        );

        CapacityCounterRecalculationPayload payload = new CapacityCounterRecalculationPayload(
            expectedToDelete,
            List.of(),
            List.of()
        );
        softly.assertThat(consumer.execute(createTask(payload)))
            .matches(result -> result.getActionType() == FINISH, "FINISH");

        verify(service).recalculateForRemoving(
            eq(List.of(versionedMapping(2L, SERVICE_ID50, CAPACITY_ID3, 6L))),
            eq(true)
        );
        verify(service).recalculateForRemoving(
            eq(Set.of(versionedMapping(10L, SERVICE_ID50, CAPACITY_ID4, 6L))),
            eq(false)
        );
    }

    /**
     * Операции в порядке их выполнения: service_id - capacity_id : [cvc_id diff => propagated (cvc.id diff)]*
     * S50 - C3 : 31 +2 => 5 +1, 32 +1 => 3 +1, 33 +1 => 8 +1; (unique add)
     * S50 - C2 : 21 -2 => 5 -1, 22 -1 => 3 -1, 23 -1 => 8 -1; (unique delete)
     */
    @Test
    @DisplayName("Уникальные сервисные счетчики корректно пересчитываются для добавления/удаления")
    @DatabaseSetup(
        value = "/repository/counting/before/with_unique_counters.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/counting/after/with_unique_counters.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void withUniqueEvents() {
        List<VersionedServiceCapacityMapping> expectedToDelete = List.of(
            versionedMapping(1L, SERVICE_ID50, CAPACITY_ID2, 7L)
        );

        List<VersionedServiceCapacityMapping> expectedToAdd = List.of(
            versionedMapping(6L, SERVICE_ID50, CAPACITY_ID3, 7L)
        );

        CapacityCounterRecalculationPayload payload = new CapacityCounterRecalculationPayload(
            expectedToDelete,
            List.of(),
            expectedToAdd
        );
        softly.assertThat(consumer.execute(createTask(payload)))
            .matches(result -> result.getActionType() == FINISH, "FINISH");

        verify(service).recalculateForAdding(eq(expectedToAdd), eq(true));
        verify(service).recalculateForRemoving(eq(expectedToDelete), eq(true));
    }

    @Test
    @DisplayName("Уникальные сервисные счетчики корректно пересчитываются для апдейта маппинга")
    @DatabaseSetup(
        value = "/repository/counting/before/with_unique_counters.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/repository/counting/after/with_unique_counters.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateWithUniqueCounters() {
        List<VersionedServiceCapacityMappingTuple> expected = List.of(new VersionedServiceCapacityMappingTuple(
            versionedMapping(6L, SERVICE_ID50, CAPACITY_ID2, 2L),
            versionedMapping(6L, SERVICE_ID50, CAPACITY_ID3, 2L)
        ));

        CapacityCounterRecalculationPayload payload = new CapacityCounterRecalculationPayload(
            List.of(),
            expected,
            List.of()
        );
        softly.assertThat(consumer.execute(createTask(payload)))
            .matches(result -> result.getActionType() == FINISH, "FINISH");

        verify(service).recalculateForAdding(eq(List.of(expected.get(0).getNewMapping())), eq(true));
        verify(service).recalculateForRemoving(eq(List.of(expected.get(0).getOldMapping())), eq(true));
    }

    private boolean containsTaskLifecycleEventOfType(int finishedTaskCount, QueueCoordinates coordinates) {
        return taskLifecycleListener.getEvents(coordinates).stream()
            .filter(event -> LifecycleEventType.FINISHED.equals(event.getType()))
            .count() == finishedTaskCount;
    }

    @Nonnull
    private Task<CapacityCounterRecalculationPayload> createTask(CapacityCounterRecalculationPayload payload) {
        return new Task<>(
            new QueueShardId("master"),
            payload,
            0,
            ZonedDateTime.now(ZoneId.of("UTC")),
            "traceInfo",
            "actor"
        );
    }
}
