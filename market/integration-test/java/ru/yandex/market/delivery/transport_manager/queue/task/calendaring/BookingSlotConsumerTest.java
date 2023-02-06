package ru.yandex.market.delivery.transport_manager.queue.task.calendaring;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationSubstatus;
import ru.yandex.market.delivery.transport_manager.facade.TransportationUpdateFacade;
import ru.yandex.market.delivery.transport_manager.queue.base.exception.DbQueueTaskExecutionException;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.booking.BookingSlotConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.booking.BookingSlotDto;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.booking.BookingSlotProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.cancelation.CancelBookedSlotProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.cancelation_and_retry.CancelBookedSlotAndRetryProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.cancellation.CancellationService;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.master.TransportationMasterConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.put.PutMovementProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.outbound.PutOutboundProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.shipment.ShipmentProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.util.DbQueueUtils;
import ru.yandex.market.delivery.transport_manager.util.matcher.GetFreeSlotsRequestArgumentMatcher;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.AvailableLimitResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsForDayResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.RequestSizeResponse;
import ru.yandex.market.logistics.calendaring.client.dto.TimeSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseFreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.transport_manager.util.DbQueueUtils.createTask;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class BookingSlotConsumerTest extends AbstractContextualTest {

    private final LocalTime from1 = LocalTime.of(12, 0);
    private final LocalTime to1 = LocalTime.of(12, 30);
    private final LocalTime from2 = LocalTime.of(16, 30);
    private final LocalTime to2 = LocalTime.of(17, 0);

    @Autowired
    TransportationMapper transportationMapper;
    @Autowired
    TransportationMasterConsumer transportationMasterConsumer;
    @Autowired
    BookingSlotConsumer bookingSlotConsumer;
    @Autowired
    BookingSlotProducer bookingSlotProducer;
    @Autowired
    ShipmentProducer shipmentProducer;
    @Autowired
    PutOutboundProducer putOutboundProducer;
    @Autowired
    CalendaringServiceClientApi calendaringServiceClient;
    @Autowired
    PutMovementProducer putMovementProducer;
    @Autowired
    CancelBookedSlotAndRetryProducer cancelBookedSlotAndRetryProducer;
    @Autowired
    CancelBookedSlotProducer cancelBookedSlotProducer;
    @Autowired
    TransportationUpdateFacade transportationUpdateFacade;
    @Autowired
    CancellationService cancellationService;

    @BeforeEach
    void before() {
        clock.setFixed(Instant.parse("2020-07-09T21:00:00.00Z"), ZoneOffset.UTC);
        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(getFreeSlotResponse(1L));
        when(calendaringServiceClient.bookSlot(any())).thenReturn(getBookSlotResponse());
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/transportation_booking_slot_task/new_transportation.xml",
        "/repository/facade/transportation_booking_slot_task/weekly_schedule.xml",
        "/repository/service/transportation_task_creator/delivery_partner.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/transportation_booking_slot_task/after/transportation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void bookingSlotForInterwarehouseSuccess() {
        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(getFreeSlotResponse(1L));

        DbQueueUtils.assertExecutedSuccessfully(
            bookingSlotConsumer,
            new BookingSlotDto().setTransportationIds(List.of(1L))
        );
        verify(calendaringServiceClient, times(2)).bookSlot(any());
        verify(putMovementProducer).enqueue(eq(1L));
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/transportation_booking_slot_task/new_xdoc_transportation.xml",
        "/repository/facade/transportation_booking_slot_task/weekly_schedule.xml",
        "/repository/service/transportation_task_creator/delivery_partner.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/transportation_booking_slot_task/after/xdoc_transportation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void bookingSlotForXDocSuccess() {
        when(calendaringServiceClient.getFreeSlots(
            argThat(r -> r != null && r.getWarehouseIds().contains(1L)))
        )
            .thenReturn(getFreeSlotResponse(1L));
        when(calendaringServiceClient.getFreeSlots(
            argThat(r -> r != null && r.getWarehouseIds().contains(2L)))
        )
            .thenReturn(getFreeSlotResponse(2L));

        DbQueueUtils.assertExecutedSuccessfully(
            bookingSlotConsumer,
            new BookingSlotDto().setTransportationIds(List.of(1L))
        );
        verify(calendaringServiceClient, times(2)).bookSlot(any());
        verify(putMovementProducer).enqueue(eq(1L));
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/transportation_booking_slot_task/new_orders_transportation.xml",
        "/repository/service/transportation_task_creator/delivery_partner.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/transportation_booking_slot_task/after/orders_transportation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void slotsBookingIgnored() {
        DbQueueUtils.assertExecutedSuccessfully(
            bookingSlotConsumer,
            new BookingSlotDto().setTransportationIds(List.of(1L))
        );
        verify(calendaringServiceClient, never()).bookSlot(any());
        verify(putMovementProducer).enqueue(eq(1L));
    }

    @Test
    @DatabaseSetup({
        "/repository/service/transportation_task_creator/transportation_in_shipment.xml",
        "/repository/service/transportation_task_creator/delivery_partner.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/transportation_booking_slot_task/after/transportation_with_errors.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void bookingSlotFailed() {
        softly.assertThatThrownBy(() -> bookingSlotConsumer.execute(
            createTask(new BookingSlotDto().setTransportationIds(List.of(2L))))
        )
            .isInstanceOf(DbQueueTaskExecutionException.class)
            .hasMessage("Failed to find [TRANSPORTATION] with ids [[2]]");
        verify(putMovementProducer, never()).enqueue(any());
    }

    @Test
    @DatabaseSetup({
            "/repository/service/transportation_task_creator/transportation_in_shipment.xml",
            "/repository/service/transportation_task_creator/delivery_partner.xml",
    })
    @ExpectedDatabase(
            value = "/repository/facade/transportation_booking_slot_task/after/transportation_with_errors.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void bookingSlotFailedIfPutIsDisabled() {
        softly.assertThatThrownBy(() -> bookingSlotConsumer.execute(
                        createTask(new BookingSlotDto().setTransportationIds(List.of(2L))))
                )
                .isInstanceOf(DbQueueTaskExecutionException.class)
                .hasMessage("Failed to find [TRANSPORTATION] with ids [[2]]");
        verify(putMovementProducer, never()).enqueue(any());
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/transportation_booking_slot_task/new_transportation.xml",
        "/repository/service/transportation_task_creator/delivery_partner.xml",
    })
    void cancelBookedSlotAndRetryFailForInterwarehouse() {
        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(new FreeSlotsResponse(Collections.emptyList()));
        when(calendaringServiceClient.getAvailableLimit(any())).thenReturn(new AvailableLimitResponse(List.of(
            new RequestSizeResponse(LocalDate.now(clock), Long.MAX_VALUE, Long.MAX_VALUE)
        )));
        transportationMapper.getById(1L);

        DbQueueUtils.assertExecutedWithFailure(
            bookingSlotConsumer,
            new BookingSlotDto().setTransportationIds(List.of(1L))
        );

        verify(transportationUpdateFacade).shiftTimeTransportation(any());
        verify(cancelBookedSlotAndRetryProducer).enqueueIds(eq(List.of(1L)));
    }

    @Test
    @DatabaseSetup({
            "/repository/facade/transportation_booking_slot_task/new_transportation.xml",
            "/repository/service/transportation_task_creator/delivery_partner.xml",
    })
    void cancelBookedSlotAndRetryFailForInterwarehouseIfPutIsDisabled() {
        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(new FreeSlotsResponse(Collections.emptyList()));

        transportationMapper.getById(1L);

        DbQueueUtils.assertExecutedWithFailure(
            bookingSlotConsumer,
            new BookingSlotDto().setTransportationIds(List.of(1L))
        );

        verify(transportationUpdateFacade).shiftTimeTransportation(any());
        verify(cancelBookedSlotAndRetryProducer).enqueueIds(eq(List.of(1L)));
    }

    @ParameterizedTest
    @MethodSource("cancelBookedSlotForXdocParameters")
    @DatabaseSetup({
        "/repository/facade/transportation_booking_slot_task/new_xdoc_transportation.xml",
        "/repository/service/transportation_task_creator/delivery_partner.xml",
    })
    void cancelBookedSlotForXdoc(
        long items,
        long pallets,
        long warehouseId,
        BookingType bookingType,
        long anotherWarehouseId,
        BookingType anotherBookingType,
        TransportationSubstatus expectedReason
    ) {
        when(calendaringServiceClient.getFreeSlots(argThat(new GetFreeSlotsRequestArgumentMatcher(
            bookingType, SupplierType.FIRST_PARTY, Set.of(warehouseId)
        )))).thenReturn(new FreeSlotsResponse(Collections.emptyList()));
        when(calendaringServiceClient.getFreeSlots(argThat(new GetFreeSlotsRequestArgumentMatcher(
            anotherBookingType, SupplierType.FIRST_PARTY, Set.of(anotherWarehouseId)
        )))).thenReturn(new FreeSlotsResponse(List.of(
            new WarehouseFreeSlotsResponse(anotherWarehouseId, List.of(
                new FreeSlotsForDayResponse(LocalDate.now(clock), ZoneOffset.UTC, List.of(
                    new TimeSlotResponse(LocalTime.MIN, LocalTime.MAX)
                ))
            ))
        )));

        when(calendaringServiceClient.getAvailableLimit(any())).thenReturn(new AvailableLimitResponse(List.of(
            new RequestSizeResponse(LocalDate.now(clock), items, pallets)
        )));

        DbQueueUtils.assertExecutedWithFailure(
            bookingSlotConsumer,
            new BookingSlotDto().setTransportationIds(List.of(1L))
        );

        var transportationCaptor = ArgumentCaptor.forClass(Transportation.class);
        verify(cancelBookedSlotProducer).enqueue(transportationCaptor.capture());
        softly.assertThat(transportationCaptor.getValue().getId())
            .isEqualTo(1L);

        transportationCaptor = ArgumentCaptor.forClass(Transportation.class);
        var substatusCaptor = ArgumentCaptor.forClass(TransportationSubstatus.class);
        verify(cancellationService).cancelTransportation(
            transportationCaptor.capture(),
            substatusCaptor.capture()
        );
        softly.assertThat(transportationCaptor.getValue().getId())
            .isEqualTo(1L);
        softly.assertThat(substatusCaptor.getValue())
            .isEqualTo(expectedReason);
    }

    @Test
    @DatabaseSetup({
            "/repository/facade/transportation_booking_slot_task/new_xdoc_transportation.xml",
            "/repository/service/transportation_task_creator/delivery_partner.xml",
    })
    void cancelBookedSlotForXdocIfPutIsDisabled() {
        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(new FreeSlotsResponse(Collections.emptyList()));

        DbQueueUtils.assertExecutedWithFailure(
            bookingSlotConsumer,
            new BookingSlotDto().setTransportationIds(List.of(1L))
        );

        var transportationCaptor = ArgumentCaptor.forClass(Transportation.class);
        verify(cancelBookedSlotProducer).enqueue(transportationCaptor.capture());
        softly.assertThat(transportationCaptor.getValue().getId())
                .isEqualTo(1L);

        transportationCaptor = ArgumentCaptor.forClass(Transportation.class);
        var substatusCaptor = ArgumentCaptor.forClass(TransportationSubstatus.class);
        verify(cancellationService).cancelTransportation(
                transportationCaptor.capture(),
                substatusCaptor.capture()
        );
        softly.assertThat(transportationCaptor.getValue().getId())
                .isEqualTo(1L);
        softly.assertThat(substatusCaptor.getValue())
            .isEqualTo(TransportationSubstatus.NO_SOURCE_WAREHOUSE_SLOTS_AVAILABLE);
    }

    static Stream<Arguments> cancelBookedSlotForXdocParameters() {
        return Stream.of(
            // Для  склада  источника не будет слотов, но квота большая
            Arguments.of(
                Long.MAX_VALUE,
                Long.MAX_VALUE,
                1L,
                BookingType.XDOCK_TRANSPORT_WITHDRAW,
                2L,
                BookingType.XDOCK_TRANSPORT_SUPPLY,
                TransportationSubstatus.NO_SOURCE_WAREHOUSE_SLOTS_AVAILABLE
            ),
            // Для  склада  источника не будет слотов, нет квоты
            Arguments.of(
                0,
                0,
                1L,
                BookingType.XDOCK_TRANSPORT_WITHDRAW,
                2L,
                BookingType.XDOCK_TRANSPORT_SUPPLY,
                TransportationSubstatus.NO_SOURCE_WAREHOUSE_QUOTA_AVAILABLE
            ),
            // Для  склада  приёмника не будет слотов, но квота большая
            Arguments.of(
                Long.MAX_VALUE,
                Long.MAX_VALUE,
                2L,
                BookingType.XDOCK_TRANSPORT_SUPPLY,
                1L,
                BookingType.XDOCK_TRANSPORT_WITHDRAW,
                TransportationSubstatus.NO_TARGET_WAREHOUSE_SLOTS_AVAILABLE
            ),
            // Для  склада  приёмника не будет слотов, нет квоты
            Arguments.of(
                0L,
                0L,
                2L,
                BookingType.XDOCK_TRANSPORT_SUPPLY,
                1L,
                BookingType.XDOCK_TRANSPORT_WITHDRAW,
                TransportationSubstatus.NO_TARGET_WAREHOUSE_QUOTA_AVAILABLE
            )
        );
    }

    @Test
    @DatabaseSetup({
        "/repository/facade/transportation_booking_slot_task/transportation_with_different_dates.xml",
        "/repository/facade/transportation_booking_slot_task/weekly_schedule.xml",
        "/repository/service/transportation_task_creator/delivery_partner.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/transportation_booking_slot_task/after/transportation_with_different_dates.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void bookingSlotForInterwarehouseWithDifferentDatesSuccess() {
        TimeSlotResponse timeSlotResponse1 = new TimeSlotResponse(from1, to1);
        TimeSlotResponse timeSlotResponse2 = new TimeSlotResponse(from2, to2);

        FreeSlotsForDayResponse freeSlotsForDayResponse10 = new FreeSlotsForDayResponse(
            LocalDate.of(2020, 7, 10),
            ZoneOffset.ofHours(3), List.of(timeSlotResponse1, timeSlotResponse2)
        );

        FreeSlotsForDayResponse freeSlotsForDayResponse11 = new FreeSlotsForDayResponse(
            LocalDate.of(2020, 7, 11),
            ZoneOffset.ofHours(3), List.of(timeSlotResponse1, timeSlotResponse2)
        );
        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
            new WarehouseFreeSlotsResponse(1L, List.of(freeSlotsForDayResponse10, freeSlotsForDayResponse11));

        when(calendaringServiceClient.getFreeSlots(any()))
            .thenReturn(new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse)));

        DbQueueUtils.assertExecutedSuccessfully(
            bookingSlotConsumer,
            new BookingSlotDto().setTransportationIds(List.of(1L))
        );
        verify(calendaringServiceClient, times(2)).bookSlot(any());
        verify(putMovementProducer).enqueue(eq(1L));
    }

    private FreeSlotsResponse getFreeSlotResponse(long warehouseId) {
        TimeSlotResponse timeSlotResponse1 = new TimeSlotResponse(from1, to1);
        TimeSlotResponse timeSlotResponse2 = new TimeSlotResponse(from2, to2);
        FreeSlotsForDayResponse freeSlotsForDayResponse = new FreeSlotsForDayResponse(
            LocalDate.of(2020, 7, 10),
            ZoneOffset.ofHours(3), List.of(timeSlotResponse1, timeSlotResponse2)
        );
        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
            new WarehouseFreeSlotsResponse(warehouseId, List.of(freeSlotsForDayResponse));
        return new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse));
    }

    private BookSlotResponse getBookSlotResponse() {
        return new BookSlotResponse(1L, 1L, ZonedDateTime.now(), ZonedDateTime.now());
    }
}
