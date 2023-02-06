package ru.yandex.market.delivery.transport_manager.task;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.TransportationDto;
import ru.yandex.market.delivery.transport_manager.domain.dto.TransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationAdditionalData;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationRoutingConfig;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheduleRoutingConfig;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationSubstatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.DimensionsClass;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.facade.TransportationUpdateFacade;
import ru.yandex.market.delivery.transport_manager.facade.transportation.TransportationFacade;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.cancellation.TransportationCancellationProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.checker.TransportationCheckerProducer;
import ru.yandex.market.delivery.transport_manager.service.TransportationStatusService;
import ru.yandex.market.delivery.transport_manager.service.checker.validation.TransportationValidator;
import ru.yandex.market.delivery.transport_manager.service.order.OrderBindingService;
import ru.yandex.market.delivery.transport_manager.task.dto.TransportationWithPreviousStatus;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static com.github.springtestdbunit.annotation.DatabaseOperation.UPDATE;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.transport_manager.domain.entity.OrderBindingType.ON_TRANSPORTATION_POINT_CHANGED;

class RefreshTransportationsByConfigTaskTest extends AbstractContextualTest {
    public static final Set<TransportationType> IGNORED_INTERWAREHOUSE_TYPES = Set.of(
        TransportationType.INTERWAREHOUSE,
        TransportationType.ANOMALY_LINEHAUL,
        TransportationType.SCRAP_LINEHAUL,
        TransportationType.INTERWAREHOUSE_VIRTUAL,
        TransportationType.FULFILLMENT_ASSEMBLAGE
    );
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private TransportationFacade transportationFacade;
    @Autowired
    private TransportationUpdateFacade transportationUpdateFacade;
    @Autowired
    private TransportationCheckerProducer checkerProducer;
    @Autowired
    private TransportationStatusService transportationStatusService;
    @Autowired
    private RefreshTransportationsByConfigTask refreshTransportationsByConfigTask;
    @Autowired
    private TransportationValidator transportationValidator;
    @Autowired
    private TransportationCancellationProducer cancellationProducer;
    @Autowired
    private OrderBindingService orderBindingService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setClock() {
        clock.setFixed(Instant.parse("2020-11-29T19:00:00.00Z"), ZoneOffset.UTC);

        when(refreshTransportationsByConfigTask.merge(any(), anyList(), anyList())).thenCallRealMethod();

        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    }

    @Test
    @DatabaseSetup(value = "/repository/schedule/setup/schedule_for_inbound_3.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_new.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/registers.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void newOnly() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/schedule/setup/schedule_for_inbound_3.xml",
            "/repository/order/existing_order.xml"
        })
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_new.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/register_with_order.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void newOnlyWithOrders() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_with_moving_with_random_minute.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_new_with_random_time.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/registers.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void newWithRandomStartingTime() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_return_3.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_return_3_new.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void newForReturn() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_new_after_cutoff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/registers_after_cutoff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void newOnlyLaunchedAfterCutoff() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock).withHour(21));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/after/existing_for_inbound_3_scheduled.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_scheduled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void withoutChanges() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/setup/one_monday.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_monday_scheduled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void appendToNewDay() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/setup/one_monday_one_tuesday.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/appended_to_existing_day.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void appendBeforeExistingOneOnDay() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/after/existing_for_inbound_3_scheduled.xml")
    @DatabaseSetup(value = "/repository/transportation/setup/update_warehouse.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_scheduled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateWarehouse() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));

        verify(orderBindingService).bindAllMatchingToTransportation(any(), eq(ON_TRANSPORTATION_POINT_CHANGED));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/after/existing_for_inbound_3_scheduled.xml")
    @DatabaseSetup(value = "/repository/transportation/setup/update_schedule.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_scheduled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateSchedule() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/transportation/after/created_for_inbound_3_new.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/all_deleted.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void markAsDeletedWhenNoRelationInConfig() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/after/one_deleted_on_wednesday.xml")
    @DatabaseSetup(value = "/repository/transportation/setup/set_deleted_false_on_wednesday.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/repository/transportation/after/one_deleted_on_wednesday.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void markAsDeletedWhenDayDeletedFromSchedule() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_return_3.xml")
    @DatabaseSetup("/repository/transportation/after/one_return_deleted_on_wednesday.xml")
    @DatabaseSetup(value = "/repository/transportation/setup/set_deleted_false_on_wednesday.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/repository/transportation/after/one_return_deleted_on_wednesday.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void markReturnAsDeletedWhenDayDeletedFromSchedule() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/after/existing_for_inbound_3_scheduled.xml")
    @DatabaseSetup("/repository/schedule/setup/calendar_for_inbound_3.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/deleted_for_holiday_inbound_3.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void markAsDeletedWhenDayIsHoliday() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/after/existing_for_inbound_3_scheduled.xml")
    @DatabaseSetup(value = "/repository/transportation/setup/set_deleted_true_on_monday.xml", type = UPDATE)
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_scheduled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void unmarkAsDeletedWhenCameBackToConfig() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/setup/one_processing_on_wednesday.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/setup/one_processing_on_wednesday.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void doNotMarkAsDeletedForProcessing() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/setup/all_processing.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/setup/all_processing.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void doNotModifyWhenSomethingChangedForProcessing() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup(value = "/repository/schedule/setup/schedule_for_inbound_5.xml", type = INSERT)
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_by_schedule_for_inbound_3_and_5.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processSuccessManyInboundPartners() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup(value = "/repository/schedule/setup/schedule_for_inbound_5.xml", type = INSERT)
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_new.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOnlySuccessfulBatchesWhenErrorInTheMiddle() {
        doThrow(new RuntimeException("Unexpected error!"))
            .when(transportationFacade).findUpcoming(any(LocalDateTime.class), refEq(Set.of(5L)), isNull());

        assertThatThrownBy(() -> refreshTransportationsByConfigTask.run(LocalDateTime.now(clock)))
            .hasMessage("Cannot refresh transportations of inboundPartnerId 5: Unexpected error!");
    }

    @Test
    @Disabled("tmp solution until TMSUPP-480") // todo TMSUPP-480 - решить гонку и вернуть тест
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/setup/one_monday_one_tuesday.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/setup/one_monday_one_tuesday.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void doNotModifyWhenChangedInParallel() {
        LocalDateTime now = LocalDateTime.now(clock);

        doAnswer(invocation -> {
            transactionTemplate.execute(status -> {
                transportationStatusService.setTransportationStatus(
                    List.of(2L, 3L),
                    TransportationStatus.SCHEDULED_WAITING_REQUEST
                );
                return null;
            });
            return invocation.callRealMethod();
        }).when(transportationUpdateFacade).updateExistingTransportationAndRecheck(Mockito.any());

        assertThatThrownBy(() -> refreshTransportationsByConfigTask.run(now))
            .hasMessage("Cannot refresh transportations of inboundPartnerId 3: " +
                "Cannot find old version of transportation with id=3, status=NEW to update");
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/after/created_for_inbound_3_scheduled.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_scheduled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testNotModifyWhenSameHash() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
        verify(transportationFacade, never()).updateFieldsFromDto(Mockito.any(), Mockito.any());
        verify(checkerProducer, never()).enqueue(anyLong());
    }

    @Test
    @DisplayName("Конфиг исчез, потом появился с тем же хэшем. Перемещения должны вернуться назад.")
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/after/created_for_inbound_3_scheduled_deleted.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_scheduled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void disabledWithSameHash() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("Часть перемещений удалена, это не свидетельствует о том, что конфиг удалялся")
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_3.xml")
    @DatabaseSetup("/repository/transportation/after/created_for_inbound_3_scheduled_some_deleted.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_for_inbound_3_scheduled_some_deleted.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void disabledWithSameHashPartialDeleted() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
        verify(transportationFacade, never()).updateFieldsFromDto(Mockito.any(), Mockito.any());
        verify(checkerProducer, never()).enqueue(anyLong());
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_5.xml")
    @DatabaseSetup("/repository/transportation/after/existing_for_inbound_5_scheduled.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/existing_for_inbound_5_scheduled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testEmptyHashIsAlwaysDifferent() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
        verify(transportationFacade, times(3)).updateFieldsFromDto(Mockito.any(), Mockito.any());
        verify(checkerProducer, times(3)).enqueue(anyLong());
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/interwarehouse_schedule.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_interwarehouse_by_config.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testXdocTransportationProcessing() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/interwarehouse_schedule.xml")
    @DatabaseSetup("/repository/transportation/after/created_interwarehouse_by_config_with_movement.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_interwarehouse_by_config_with_movement.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testXDocCreatedNoDuplicates() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/transportation/scheduled_xdoc.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_xdoc_deleted.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testDeleteXDoc() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
        Mockito.verify(cancellationProducer)
            .enqueue(102L, TransportationSubstatus.INTERWAREHOUSE_DELETED);
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/interwarehouse_schedule.xml")
    @DatabaseSetup("/repository/transportation/after/after_xdoc_deleted.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/after_xdoc_restored.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testRestoreXDoc() {
        Mockito.doReturn(List.of()).when(transportationValidator).getErrors(Mockito.any());
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/schedule/setup/interwarehouse_schedule.xml",
            "/repository/schedule/setup/interwarehouse_schedule_another_point.xml",
            "/repository/transportation/after/created_interwarehouse_by_config.xml"
        }
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/created_interwarehouse_by_config_with_different_points.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testXDocWithDifferentPoints() {
        restartTransportationSequences(100);
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/schedule/setup/same_partner_different_segments.xml"
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/same_partner_different_segments.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testDropoffDifferentMovementSegments() {
        restartTransportationSequences(100);
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/cutoff_data_after_default.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/cutoff_data_after_default.xml",
        assertionMode = NON_STRICT
    )
    void testCutoffLaunchDateAfterDefault() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/cutoff_data_before_default.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/cutoff_data_before_default.xml",
        assertionMode = NON_STRICT
    )
    void testCutoffLaunchDateBeforeDefault() {
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
    }

    @Test
    void excludeAlreadyProcessingDraft() {
        String hash1 = "hash1";
        List<TransportationDto> dtos = new ArrayList<>();
        List<Transportation> existing = new ArrayList<>();

        TransportationDto config1 = new TransportationDto()
            .setHash(hash1)
            .setTransportationType(TransportationType.LINEHAUL)
            .setOutboundUnit(new TransportationUnitDto()
                .setPlannedIntervalStart(LocalDateTime.of(2020, 11, 29, 22, 0))
            );
        dtos.add(config1);

        Transportation transportation1 = new Transportation()
            .setId(1L)
            .setHash(hash1)
            .setTransportationType(TransportationType.LINEHAUL)
            .setStatus(TransportationStatus.DRAFT)
            .setOutboundUnit(new TransportationUnit()
                .setPlannedIntervalStart(LocalDateTime.of(2020, 11, 29, 22, 0))
            );
        existing.add(transportation1);

        refreshTransportationsByConfigTask.excludeAlreadyProcessing(dtos, existing);

        softly.assertThat(existing).containsExactlyInAnyOrder(transportation1);
        softly.assertThat(dtos).containsExactlyInAnyOrder(config1);
    }

    @Test
    void excludeAlreadyProcessingMovementSent() {
        String hash2 = "hash2";

        List<TransportationDto> dtos = new ArrayList<>();
        List<Transportation> existing = new ArrayList<>();

        TransportationDto config2 = new TransportationDto()
            .setHash(hash2)
            .setTransportationType(TransportationType.LINEHAUL)
            .setOutboundUnit(new TransportationUnitDto()
                .setPlannedIntervalStart(LocalDateTime.of(2020, 11, 29, 22, 0))
            );
        dtos.add(config2);

        Transportation transportation2 = new Transportation()
            .setId(2L)
            .setHash(hash2)
            .setTransportationType(TransportationType.LINEHAUL)
            .setStatus(TransportationStatus.MOVEMENT_SENT)
            .setOutboundUnit(new TransportationUnit()
                .setPlannedIntervalStart(LocalDateTime.of(2020, 11, 29, 22, 0))
            );
        existing.add(transportation2);

        refreshTransportationsByConfigTask.excludeAlreadyProcessing(dtos, existing);

        softly.assertThat(existing).isEmpty();
        softly.assertThat(dtos).isEmpty();
    }

    @ParameterizedTest(name = "Checker не запустится для {0} перемещений")
    @MethodSource("interwarehouseTypes")
    void mergeLinehaul(TransportationType transportationType, TransportationAdditionalData additionalData) {
        TransportationDto dto = new TransportationDto()
            .setTransportationType(transportationType)
            .setHash("hash")
            .setOutboundUnit(
                new TransportationUnitDto()
                    .setPlannedIntervalStart(LocalDateTime.now(clock))
                    .setLogisticPointId(1L)
            )
            .setInboundUnit(
                new TransportationUnitDto()
                    .setPlannedIntervalStart(LocalDateTime.now(clock))
                    .setLogisticPointId(2L)
            )
            .setMovement(new MovementDto())
            .setRoutingConfigDto(new TransportationScheduleRoutingConfig(
                null, true, DimensionsClass.MEDIUM_SIZE_CARGO, 1.1D, false, "DEFAULT"
            ));

        Transportation transportation = new Transportation()
            .setTransportationType(transportationType)
            .setStatus(TransportationStatus.DRAFT)
            .setOutboundUnit(
                new TransportationUnit()
                    .setPlannedIntervalStart(LocalDateTime.now(clock))
                    .setLogisticPointId(1L)
            )
            .setInboundUnit(
                new TransportationUnit()
                    .setPlannedIntervalStart(LocalDateTime.now(clock))
                    .setLogisticPointId(2L)
            )
            .setMovement(new Movement())
            .setAdditionalData(additionalData);

        Transportation expected = deepCopy(transportation)
            .setHash("hash")
            .setAdditionalData(new TransportationAdditionalData(new TransportationRoutingConfig(
                true, DimensionsClass.MEDIUM_SIZE_CARGO, 1.1D, false, "DEFAULT"
            )));

        RefreshTransportationsByConfigTask.TransportationsForSave result = refreshTransportationsByConfigTask.merge(
            RefreshTransportationsByConfigTask.TransportationKey.of(dto),
            new ArrayList<>(List.of(dto)),
            new ArrayList<>(List.of(transportation))
        );

        softly.assertThat(result.getCreated()).isEmpty();
        softly.assertThat(result.getDeleted()).isEmpty();
        softly.assertThat(result.getUpdated()).containsExactlyInAnyOrder(
            new TransportationWithPreviousStatus(
                expected,
                TransportationStatus.DRAFT,
                false
            )
        );

        // Проверяем именно на равенство ссылок.
        // Пока в additionalData только routing config, это сложно протестировать по-дургому.
        // Тест на то, что при обновлении мы стираем/ заменяем только routingConfig, а остальные поля,
        // если такие появятся, останутся на месте. Как только мы добавим в additionalData что-либо ещё,
        // этот тест можно будет заменить на что-то лучшее
        if (additionalData != null) {
            softly.assertThat(additionalData ==
                    result.getUpdated().get(0).getTransportation().getAdditionalData())
                .isTrue();
        }
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_with_all_days.xml")
    @DisplayName("Равномерная генерация перемещений на today + 7d. Новое перемещение после plannedIntervalStart")
    void testPlannedStartInInterval() {
        clock.setFixed(Instant.parse("2020-11-29T00:45:00.00Z"), ZoneOffset.UTC);
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
        Assertions.assertEquals(
            6,
            transportationFacade.findUpcoming(LocalDateTime.now(clock), Set.of(3000L), null).size()
        );
        clock.setFixed(Instant.parse("2020-11-29T18:45:00.00Z"), ZoneOffset.UTC);
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
        Assertions.assertEquals(
            7,
            transportationFacade.findUpcoming(LocalDateTime.now(clock), Set.of(3000L), null).size()
        );
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/schedule_for_inbound_5.xml")
    @DatabaseSetup("/repository/transportation/setup/only_two_transportations_for_inbound_5.xml")
    @DisplayName("Равномерная генерация перемещений на today + 7d. Добавление перемещения на тот же день (вт)")
    void testAddNewTransportationToExistingDay() {
        clock.setFixed(Instant.parse("2020-11-29T00:45:00.00Z"), ZoneOffset.UTC);
        refreshTransportationsByConfigTask.run(LocalDateTime.now(clock));
        Assertions.assertEquals(
            3,
            transportationFacade.findUpcoming(LocalDateTime.now(clock), Set.of(5L), null).size()
        );
    }

    @SneakyThrows
    private <T> T deepCopy(T t) {
        return (T) objectMapper.readValue(objectMapper.writeValueAsString(t), t.getClass());
    }

    static Stream<Arguments> interwarehouseTypes() {
        return Arrays
            .stream(TransportationType.values())
            .filter(TransportationType::isInterwarehouse)
            .filter(
                type -> !IGNORED_INTERWAREHOUSE_TYPES.contains(type)
            ) // interwarehouse can't be scheduled
            .flatMap(t -> Stream.of(
                Pair.of(t, new TransportationAdditionalData(new TransportationRoutingConfig(
                    true, DimensionsClass.REGULAR_CARGO, 1.0F, false, "DEFAULT"
                ))),
                Pair.of(t, null)
            ))
            .map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }
}
