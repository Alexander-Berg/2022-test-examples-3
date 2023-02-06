package ru.yandex.market.logistics.cs.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.config.dbqueue.DayOffByCapacityQueueConfiguration;
import ru.yandex.market.logistics.cs.config.dbqueue.DayOffByServiceQueueConfiguration;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener.QueueCoordinates;
import ru.yandex.market.logistics.cs.dbqueue.dayoff.bycapacity.ServiceToDayByCapacityProducer;
import ru.yandex.market.logistics.cs.dbqueue.dayoff.byservice.DayOffByServiceProducer;
import ru.yandex.market.logistics.cs.dbqueue.valuecounter.CapacityCounterRecalculationProducer;
import ru.yandex.market.logistics.cs.domain.entity.ServiceCapacityMapping;
import ru.yandex.market.logistics.cs.domain.exception.SnapshotValidationException;
import ru.yandex.market.logistics.cs.domain.jdbc.VersionedServiceCapacityMapping;
import ru.yandex.market.logistics.cs.domain.jdbc.VersionedServiceCapacityMappingTuple;
import ru.yandex.market.logistics.cs.repository.ServiceCapacityMappingTestRepository;
import ru.yandex.market.logistics.cs.repository.custom.LmsServiceForcedDayOffCustomRepository;
import ru.yandex.money.common.dbqueue.settings.QueueLocation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener.LifecycleEventType.FINISHED;
import static ru.yandex.market.logistics.cs.dbqueue.common.SingleQueueShardRouter.MASTER;

@DisplayName("Тестирование снапшота схемы LMS в схему CS")
class LmsSnapshotServiceTest extends AbstractIntegrationTest {
    private static final long UNMODIFIED_MAPPING_ID = 1L;
    private static final long MODIFIED_MAPPING_ID = 2L;
    private static final QueueLocation DAY_OFF_BY_CAPACITY_LOCATION = DayOffByCapacityQueueConfiguration.QUEUE_LOCATION;
    private static final QueueLocation DAY_OFF_BY_SERVICE_LOCATION = DayOffByServiceQueueConfiguration.QUEUE_LOCATION;
    private static final QueueCoordinates
        DAY_OFF_BY_CAPACITY_COORDINATES = new QueueCoordinates(MASTER, DAY_OFF_BY_CAPACITY_LOCATION);
    private static final QueueCoordinates
        DAY_OFF_BY_SERVICE_COORDINATES = new QueueCoordinates(MASTER, DAY_OFF_BY_SERVICE_LOCATION);
    private static final int PRODUCER_TASK_NUM = 4;

    @Autowired
    private LmsSnapshotService lmsSnapshotService;

    @Autowired
    private ServiceCapacityMappingTestRepository serviceCapacityMappingTestRepository;

    @Autowired
    private CapacityCounterRecalculationProducer queueProducer;

    @Autowired
    private DayOffByServiceProducer dayOffByServiceProducer;

    @Autowired
    private ServiceToDayByCapacityProducer dayOffByCapacityProducer;

    @Autowired
    private AccountingTaskLifecycleListener taskLifecycleListener;

    @Autowired
    private ServiceDayOffService dayOffService;

    @Autowired
    private LmsServiceForcedDayOffCustomRepository lmsServiceForcedDayOffCustomRepository;

    @Autowired
    private TestableClock clock;

    @AfterEach
    void tearDown() {
        clock.clearFixed();
    }

    @BeforeEach
    void mock() {
        doNothing().when(queueProducer).enqueue(any(), any(), any());
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

    /**
     * В этом тесте дополнительно обработан кейс при удалении capacity и capacity value:
     * <p>
     * <li>Удаляется capacity_value с id = 2,
     * capacity_value_counter'ы не удаляются, но заменяются безлимитными.
     * </li>
     * <p>
     * <li>Удаляется капасити с id = 3, трет за собой все capacity_value_counter'ы, где есть связь.</li>
     */
    @DatabaseSetup("/repository/lms/before/lms_merge.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/lms_merge.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSuccessfulSnapshot() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
    }

    @DatabaseSetup("/repository/lms/before/lms_merge_changing_values.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/lms_merge_changing_values.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testMergeChangingValues() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
        await().atMost(Duration.ofMinutes(1)).until(() -> containsTaskLifecycleEventOfType(
            8,
            DAY_OFF_BY_CAPACITY_COORDINATES
        ));
        verify(dayOffByCapacityProducer, times(8)).enqueue(any(), any());
    }

    @DatabaseSetup("/repository/lms/before/lms_merge_change_for_new_values.xml")
    @ExpectedDatabase(value = "/repository/lms/after/lms_merge_change_for_new_values.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testChangeValuesToOverflow() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
        verify(dayOffByCapacityProducer).enqueue(any(), any());
    }

    @DisplayName("Добавляем капасити велью со значением 0 при отсутствии на этот день заказов")
    @DatabaseSetup("/repository/lms/before/add_zero_value.xml")
    @ExpectedDatabase(value = "/repository/lms/after/add_zero_value.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testAddZeroValue() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
    }

    @DisplayName("Добавляем капасити велью со значением 0. Заказы на этот день присутствуют")
    @DatabaseSetup("/repository/lms/before/add_zero_value_having_orders_that_day.xml")
    @ExpectedDatabase(value = "/repository/lms/after/add_zero_value_having_orders_that_day.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testAddZeroValueHavingOrdersThatDay() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
    }

    @DisplayName("Обновляем капасити велью на значение 0 при отсутствии на этот день заказов")
    @DatabaseSetup("/repository/lms/before/update_zero_value.xml")
    @ExpectedDatabase(value = "/repository/lms/after/update_zero_value.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUpdateZeroValue() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
    }

    @DisplayName("Обновляем капасити велью на значение 0. Заказы на этот день присутствуют")
    @DatabaseSetup("/repository/lms/before/update_zero_value_having_orders_that_day.xml")
    @ExpectedDatabase(value = "/repository/lms/after/update_zero_value_having_orders_that_day.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUpdateZeroValueHavingOrdersThatDay() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
    }

    @DatabaseSetup("/repository/lms/before/lms_merge_service_capacity_mapping_before.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/lms_merge_service_capacity_mapping_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testServiceCapacityMappingSnapshot() {
        LocalDateTime unmodifiedBefore = serviceCapacityMappingTestRepository.findUpdated(UNMODIFIED_MAPPING_ID);
        LocalDateTime modifiedBefore = serviceCapacityMappingTestRepository.findUpdated(MODIFIED_MAPPING_ID);

        lmsSnapshotService.snapshot();

        LocalDateTime unmodifiedAfter = serviceCapacityMappingTestRepository.findUpdated(UNMODIFIED_MAPPING_ID);
        LocalDateTime modifiedAfter = serviceCapacityMappingTestRepository.findUpdated(MODIFIED_MAPPING_ID);

        softly.assertThat(unmodifiedBefore).isEqualTo(unmodifiedAfter);
        softly.assertThat(modifiedBefore).isNotEqualTo(modifiedAfter);

        verify(queueProducer).enqueue(
            eq(List.of(versionedMapping(3L, 3L, 3L, 2L))),
            eq(List.of(new VersionedServiceCapacityMappingTuple(
                versionedMapping(2L, 2L, 1L, 2L),
                versionedMapping(2L, 2L, 2L, 2L)
            ))),
            eq(List.of(versionedMapping(4L, 4L, 4L, 1L)))

        );
    }

    @DatabaseSetup("/repository/lms/before/lms_add_capacity_value_before.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/lms_add_capacity_value_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    @DisplayName("Добавление лимитов приводит к пересчету счётчиков")
    void testServiceCapacityValueAddingSnapshot() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
    }

    @DatabaseSetup("/repository/lms/before/lms_remove_capacity_value_before.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/lms_remove_capacity_value_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    @DisplayName("Удаление лимитов приводит к пересчету счётчиков")
    void testServiceCapacityValueRemovingSnapshot() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
    }

    @Nonnull
    private VersionedServiceCapacityMapping versionedMapping(Long id, Long serviceId, Long capacityId, Long version) {
        ServiceCapacityMapping mapping = ServiceCapacityMapping.builder()
            .id(id)
            .serviceId(serviceId)
            .capacityId(capacityId)
            .build();
        return new VersionedServiceCapacityMapping(mapping, version);
    }

    @DisplayName("Дедупликация значений уже в существущей базе")
    @DatabaseSetup("/repository/lms/before/lms_merge_duplicates.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/lms_merge_duplicates.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testOldDuplicatesRemoval() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
    }

    @DisplayName("Дедупликация значений при удалении")
    @DatabaseSetup("/repository/lms/before/lms_merge_duplicates_removal.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/lms_merge_duplicates_removal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testDeduplicationOnValueRemoval() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
    }

    @DisplayName("Дедупликация значений при добавлении")
    @DatabaseSetup("/repository/lms/before/lms_merge_duplicates_adding.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/lms_merge_duplicates_adding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testDeduplicationOnValueAdding() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
    }

    @DisplayName("Дедупликация значений при замене значения")
    @DatabaseSetup("/repository/lms/before/lms_merge_duplicates_switch.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/lms_merge_duplicates_switch.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testDeduplicationOnValueSwitch() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
    }

    @DisplayName("Дейоффы на сервисах корректно изменяются при изменениях маппингов")
    @DatabaseSetup("/repository/lms/before/update_day_offs_by_service.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/update_day_offs_by_service.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSnapshotUpdateDayOffsByService() {
        doCallRealMethod().when(queueProducer).enqueue(any(), any(), any());
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
        await().atMost(Duration.ofMinutes(1)).until(() -> containsTaskLifecycleEventOfType(
            PRODUCER_TASK_NUM,
            DAY_OFF_BY_CAPACITY_COORDINATES
        ));
        verify(dayOffByCapacityProducer, times(PRODUCER_TASK_NUM)).enqueue(any(), any());
        verify(dayOffService).removeDayOffs(anyList());
        verify(dayOffService, times(PRODUCER_TASK_NUM)).updateDayOffs(anyLong(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Счетчики родителей уменьшаются после удаления листового капасити")
    @DatabaseSetup("/repository/lms/before/lms_merge_deleted_capacity_counters.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/lms_merge_deleted_capacity_counters.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void parentCountersAreDecreasedWhenChildCapacityIsDeleted() {
        doNothing().when(dayOffByCapacityProducer).enqueue(anyLong(), any(LocalDate.class));
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
        // для 146244, 146245 и дней [now], [now-1]
        verify(dayOffByCapacityProducer, times(PRODUCER_TASK_NUM)).enqueue(anyLong(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Запускаем снапшот, но исходные таблицы пусты")
    void testSnapshotEmptySourceTables() {
        softly.assertThatThrownBy(() -> lmsSnapshotService.snapshot())
            .isInstanceOf(SnapshotValidationException.class)
            .hasMessage("One or more source table is empty. Please check LMS -> CS data transfer state");
    }

    @Test
    @DisplayName("Обновляем дейоффы с пустой таблицей service_forced_day_off")
    void testUpdateForcedDayOffsEmptyTable() {
        assertThatThrownBy(() -> lmsSnapshotService.updateDayOffsAccordingToForcedDayOffs())
            .isInstanceOf(SnapshotValidationException.class)
            .hasMessage("service_forced_day_off table is empty. Please check LMS -> CS data transfer state");
    }

    @Test
    @DisplayName("Обновляем дейоффы, откатывается при исключении на удалении")
    @DatabaseSetup("/repository/lms/forced_day_off/before/with_difference.xml")
    @ExpectedDatabase(
        value = "/repository/lms/forced_day_off/before/with_difference.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateForcedDayOffsRollbackOnDelete() {
        doThrow(RuntimeException.class).when(lmsServiceForcedDayOffCustomRepository).removeOldDayOffs();
        assertThatThrownBy(() -> lmsSnapshotService.updateDayOffsAccordingToForcedDayOffs())
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Обновляем дейоффы, откатывается при исключении на добавлении")
    @DatabaseSetup("/repository/lms/forced_day_off/before/with_difference.xml")
    @ExpectedDatabase(
        value = "/repository/lms/forced_day_off/before/with_difference.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateForcedDayOffsRollbackOnAdd() {
        doThrow(RuntimeException.class).when(lmsServiceForcedDayOffCustomRepository).addNewDayOffs();
        assertThatThrownBy(() -> lmsSnapshotService.updateDayOffsAccordingToForcedDayOffs())
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Обновляем дейоффы, нечего удалять, таблицы одинаковые")
    @DatabaseSetup("/repository/lms/forced_day_off/before/no_difference.xml")
    @ExpectedDatabase(
        value = "/repository/lms/forced_day_off/before/no_difference.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateForcedDayOffsNoDifference() {
        lmsSnapshotService.updateDayOffsAccordingToForcedDayOffs();
    }

    @Test
    @DisplayName("Обновляем дейоффы, просто различия по таблицам")
    @DatabaseSetup("/repository/lms/forced_day_off/before/with_difference.xml")
    @ExpectedDatabase(
        value = "/repository/lms/forced_day_off/after/with_difference.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateForcedDayOffsWithDifference() {
        lmsSnapshotService.updateDayOffsAccordingToForcedDayOffs();
    }

    @Test
    @DisplayName("Обновляем дейоффы, различия по таблицам + участвуют зависимые таблицы")
    @DatabaseSetup("/repository/lms/forced_day_off/before/with_difference_and_dependencies.xml")
    @ExpectedDatabase(
        value = "/repository/lms/forced_day_off/after/with_difference_and_dependencies.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateForcedDayOffsCantDeleteNotForced() {
        lmsSnapshotService.updateDayOffsAccordingToForcedDayOffs();
    }

    @Test
    @DisplayName("Счетчики для новых капасити дейоффятся, если у парента стояли дни")
    @DatabaseSetup("/repository/lms/before/propagate_day_off_for_capacity.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/propagate_day_off_for_capacity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dayOffsSetForNewCapacities() {
        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
        // для капасити 4, 5 и дней [now], [now+1]
        await().atMost(Duration.ofMinutes(1)).until(() -> containsTaskLifecycleEventOfType(
            PRODUCER_TASK_NUM,
            DAY_OFF_BY_CAPACITY_COORDINATES
        ));
        verify(dayOffByCapacityProducer, times(PRODUCER_TASK_NUM)).enqueue(any(), any());
        verify(dayOffService, times(PRODUCER_TASK_NUM)).updateDayOffs(anyLong(), any(LocalDate.class));
    }

    @Test
    @DisplayName(
        "Дейоффы для сервисов добавляются/обновляются, если маппинги сервис-капасити были добавлены/обновлены"
    )
    @DatabaseSetup("/repository/lms/before/propagate_day_off_for_service.xml")
    @ExpectedDatabase(
        value = "/repository/lms/after/propagate_day_off_for_service.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void dayOffsSetForNewMappings() {
        int tasksNumber = 3;

        assertDoesNotThrow(() -> lmsSnapshotService.snapshot());
        await().atMost(Duration.ofMinutes(1)).until(() -> containsTaskLifecycleEventOfType(
            tasksNumber,
            DAY_OFF_BY_SERVICE_COORDINATES
        ));
        verify(dayOffByServiceProducer, times(tasksNumber)).enqueue(anyLong(), anyLong());
        verify(dayOffService, times(tasksNumber)).updateDayOffs(anyLong(), anyLong());
    }

    private boolean containsTaskLifecycleEventOfType(long finishedTaskCount, QueueCoordinates queueCoordinates) {
        return taskLifecycleListener.getEvents(queueCoordinates).stream()
            .filter(event -> FINISHED.equals(event.getType()))
            .count() == finishedTaskCount;
    }
}
