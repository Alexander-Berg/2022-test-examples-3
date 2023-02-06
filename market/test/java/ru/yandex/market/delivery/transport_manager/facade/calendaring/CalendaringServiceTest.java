package ru.yandex.market.delivery.transport_manager.facade.calendaring;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import ru.yandex.market.delivery.transport_manager.config.caledaring_service.CalendaringServiceClientConfig;
import ru.yandex.market.delivery.transport_manager.config.properties.TmProperties;
import ru.yandex.market.delivery.transport_manager.converter.calendaring.BookSlotConverter;
import ru.yandex.market.delivery.transport_manager.converter.calendaring.CalendaringTransportationMetadataService;
import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.WeeklySchedule;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.BookedTimeSlotMapper;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;
import ru.yandex.market.delivery.transport_manager.service.WeeklyScheduleService;
import ru.yandex.market.delivery.transport_manager.service.calendaring.CalendaringService;
import ru.yandex.market.delivery.transport_manager.service.calendaring.SlotSelectionCriteriaBuilder;
import ru.yandex.market.delivery.transport_manager.service.calendaring.SlotSelectionService;
import ru.yandex.market.delivery.transport_manager.service.calendaring.exception.BookingSlotNotFoundException;
import ru.yandex.market.delivery.transport_manager.service.calendaring.exception.BookingSlotNotFoundReason;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.delivery.transport_manager.service.transportation_unit.TransportationUnitService;
import ru.yandex.market.delivery.transport_manager.util.JsonArgumentMatcher;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.AvailableLimitResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotRequest;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsForDayResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.GetAvailableLimitRequest;
import ru.yandex.market.logistics.calendaring.client.dto.RequestSizeResponse;
import ru.yandex.market.logistics.calendaring.client.dto.TimeSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.UpdateExpiresAtRequest;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseFreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CalendaringServiceTest {

    private final LocalTime from1 = LocalTime.of(13, 0, 0);
    private final LocalTime to1 = LocalTime.of(14, 0, 0);
    private final LocalTime from2 = LocalTime.of(19, 0, 0);
    private final LocalTime to2 = LocalTime.of(20, 0, 0);
    private CalendaringServiceClientApi calendaringServiceClient;
    private CalendaringService calendaringService;
    private TransportationService transportationService;
    private Transportation transportation;
    private TransportationUnitService transportationUnitService;
    private BookedTimeSlotMapper timeSlotMapper;
    private CalendaringTransportationMetadataService transportationMetadataService;
    private static WeeklyScheduleService weeklyScheduleService = mock(WeeklyScheduleService.class);

    @BeforeAll
    public static void beforeAll() {
        when(weeklyScheduleService.findAll()).thenReturn(mockedWeeklyScheduleMap());
    }

    private static Transportation getTransportation() {
        return new Transportation()
            .setId(1L)
            .setTransportationType(TransportationType.INTERWAREHOUSE)
            .setStatus(TransportationStatus.SCHEDULED)
            .setOutboundUnit(new TransportationUnit()
                .setId(1L)
                .setPartnerId(1L)
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.OUTBOUND)
                .setLogisticPointId(10L)
                .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 10, 12, 0, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 10, 15, 0, 0))
                .setRequestId(11L)
                .setBookedTimeSlot(new TimeSlot().setToDate(LocalDateTime.now()))
            )
            .setInboundUnit(new TransportationUnit()
                .setId(1L)
                .setExternalId("ID_AT_PARTNER_01")
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.INBOUND)
                .setPartnerId(1L)
                .setLogisticPointId(20L)
                .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 10, 17, 0, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 10, 20, 0, 0))
            )
            .setMovement(new Movement()
                .setId(4L)
                .setExternalId("4L")
                .setPartnerId(2L)
                .setMaxPallet(2)
                .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 12, 12, 0, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 13, 12, 0, 0))
                .setStatus(MovementStatus.NEW)
                .setWeight(94)
                .setVolume(15)
            )
            .setScheme(TransportationScheme.NEW);
    }

    @BeforeEach
    void init() {
        transportationService = mock(TransportationService.class);
        calendaringServiceClient = mock(CalendaringServiceClientApi.class);
        transportationUnitService = mock(TransportationUnitService.class);
        timeSlotMapper = mock(BookedTimeSlotMapper.class);
        transportationMetadataService = new CalendaringTransportationMetadataService(
            mock(RegisterService.class)
        );
        transportation = getTransportation();
        TmProperties tmProperties = mock(TmProperties.class);
        BookSlotConverter bookSlotConverter = new BookSlotConverter(
            tmProperties,
            transportationMetadataService,
            new IdPrefixConverter()
        );

        calendaringService = new CalendaringService(
            calendaringServiceClient,
            bookSlotConverter,
            transportationService,
            transportationUnitService,
            timeSlotMapper,
            new SlotSelectionService(weeklyScheduleService),
            new SlotSelectionCriteriaBuilder(),
            new IdPrefixConverter(),
            Clock.systemDefaultZone()
        );

        when(transportationService.getById(any())).thenReturn(transportation);
    }

    @Test
    @SneakyThrows
    void bookSlotTest() {
        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(getFreeSlotResponse());
        when(calendaringServiceClient.bookSlot(any())).thenReturn(getBookSlotResponse());

        transportation = calendaringService.bookSlot(transportation.getId(), null);

        assertThat(transportation.getInboundUnit().getBookedTimeSlot()).isNotNull();
        assertThat(transportation.getInboundUnit().getBookedTimeSlot()).isNotNull();
    }

    @Test
    void cancelBookedSlotTest() {
        calendaringService.cancelBookedSlot(1L);
        Mockito.verify(calendaringServiceClient).cancelSlot(1L);
    }

    @Test
    void approveBookedSlot() {
        calendaringService.approveBookedSlot(1L);
        Mockito.verify(calendaringServiceClient).updateExpiresAt(Mockito.eq(new UpdateExpiresAtRequest(
            1L,
            null
        )));
    }

    @Test
    void bookSlotTestWithExceptionInGetFreeSlots() {
        when(calendaringServiceClient.getFreeSlots(any())).thenThrow(new HttpTemplateException(404, "Not found"));

        Assertions.assertThrows(
            HttpTemplateException.class,
            () -> calendaringService.bookSlot(transportation.getId(), null)
        );
    }

    @Test
    void bookSlotTestWithExceptionInBookSlot() {
        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(getFreeSlotResponse());
        when(calendaringServiceClient.bookSlot(any())).thenThrow(new HttpTemplateException(500, "Something bad"));

        Assertions.assertThrows(
            HttpTemplateException.class,
            () -> calendaringService.bookSlot(transportation.getId(), null)
        );
    }

    @Test
    void bookSlotTestWithNoFreeSlots() {
        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(getEmptyFreeSlotResponse());
        when(calendaringServiceClient.getAvailableLimit(any())).thenReturn(new AvailableLimitResponse(List.of(
            new RequestSizeResponse(LocalDate.of(2020, 7, 10), Long.MAX_VALUE, Long.MAX_VALUE)
        )));

        Assertions.assertThrows(
            BookingSlotNotFoundException.class,
            () -> calendaringService.bookSlot(transportation.getId(), null)
        );
    }

    @Test
    @SneakyThrows
    void saveReturningTransportationIdsTest() {
        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(getFreeSlotResponse());
        when(calendaringServiceClient.bookSlot(any())).thenReturn(getBookSlotResponse());
        when(transportationService.persist(any(Transportation.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());
        transportation = calendaringService.bookSlot(transportation.getId(), null);

        Transportation transportationResult = calendaringService.insertTimeSlots(transportation);

        assertThat(transportationResult).isNotNull();
        assertThat(transportationResult.getInboundUnit()).isNotNull();
        assertThat(transportationResult.getOutboundUnit()).isNotNull();
    }

    @Test
    @SneakyThrows
    void bookCopyOfExistingSlotInTrip() {
        final long slotId = 1L;
        long warehouseId = 172L;
        long unitId = 67890L;
        String tripExternalId = "TMT123";
        long gateId = 10L;

        BookSlotRequest expectedBookingRequest = new BookSlotRequest(
            SupplierType.FIRST_PARTY,
            null,
            warehouseId,
            null,
            BookingType.XDOCK_TRANSPORT_WITHDRAW,
            LocalDateTime.of(2022, 6, 22, 12, 0),
            LocalDateTime.of(2022, 6, 22, 13, 0),
            "TMU%d".formatted(unitId),
            CalendaringServiceClientConfig.SOURCE,
            tripExternalId,
            0,
            0,
            null,
            null,
            false,
            null
        );

        when(calendaringServiceClient.getSlot(eq(slotId)))
            .thenReturn(new BookingResponseV2(
                slotId,
                CalendaringServiceClientConfig.SOURCE,
                "TMU12345",
                tripExternalId,
                gateId,
                ZonedDateTime.of(2022, 6, 22, 12, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                ZonedDateTime.of(2022, 6, 22, 13, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                BookingStatus.ACTIVE,
                LocalDateTime.of(2022, 6, 22, 0, 0),
                warehouseId
            ));
        when(calendaringServiceClient.bookSlot(argThat(new JsonArgumentMatcher<>(expectedBookingRequest))))
            .thenReturn(new BookSlotResponse(
                2L,
                gateId,
                ZonedDateTime.of(2022, 6, 22, 12, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                ZonedDateTime.of(2022, 6, 22, 13, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
            ));

        TimeSlot timeSlot = calendaringService.bookCopyOfExistingSlotInTrip(
            slotId,
            unitId,
            TransportationType.XDOC_TRANSPORT,
            TransportationUnitType.OUTBOUND
        );

        assertThat(timeSlot).isEqualTo(
            new TimeSlot()
                .setGateId(gateId)
                .setCalendaringServiceId(2L)
                .setZoneId(TimeUtil.DEFAULT_ZONE_OFFSET.getId())
                .setFromDate(
                    ZonedDateTime.of(2022, 6, 22, 12, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET).toLocalDateTime())
                .setToDate(
                    ZonedDateTime.of(2022, 6, 22, 13, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET).toLocalDateTime())
        );

        verify(calendaringServiceClient).getSlot(eq(slotId));
        verify(calendaringServiceClient).bookSlot(argThat(new JsonArgumentMatcher<>(expectedBookingRequest)));
        verifyNoMoreInteractions(calendaringServiceClient);
    }

    @ParameterizedTest
    @MethodSource("getNoSlotsReasonTestParams")
    public void getNoSlotsReason(
        long items,
        long pallets,
        boolean throwEx,
        BookingSlotNotFoundReason commonReason,
        BookingSlotNotFoundReason quotaReason,
        BookingSlotNotFoundReason resultReason
    ) {
        final long warehouseId = 400L;

        OngoingStubbing<AvailableLimitResponse> stubbing =
            when(calendaringServiceClient.getAvailableLimit(Mockito.argThat(new GetAvailableLimitRequestArgumentMatcher(
                warehouseId,
                BookingType.XDOCK_TRANSPORT_WITHDRAW,
                LocalDate.of(2021, 9, 1)
            ))));

        if (throwEx) {
            stubbing.thenThrow(new RuntimeException("Bad request"));
        } else {
            stubbing
                .thenReturn(new AvailableLimitResponse(List.of(
                    new RequestSizeResponse(LocalDate.of(2021, 9, 1), items, pallets)
                )));
        }

        BookingSlotNotFoundReason reason = calendaringService.getNoSlotsReason(
            new Transportation(),
            new TransportationUnit()
                .setPartnerId(warehouseId)
                .setPlannedIntervalStart(LocalDateTime.of(2021, 9, 1, 10, 0)),
            BookingType.XDOCK_TRANSPORT_WITHDRAW,
            commonReason,
            quotaReason
        );

        Assertions.assertEquals(resultReason, reason);

        verify(calendaringServiceClient).getAvailableLimit(Mockito.argThat(new GetAvailableLimitRequestArgumentMatcher(
            warehouseId,
            BookingType.XDOCK_TRANSPORT_WITHDRAW,
            LocalDate.of(2021, 9, 1)
        )));
        verifyNoMoreInteractions(calendaringServiceClient);
    }

    static Stream<Arguments> getNoSlotsReasonTestParams() {
        return Stream.of(
            Arguments.of(
                0,
                0,
                false,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_SLOTS_AVAILABLE,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_QUOTA_AVAILABLE,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_QUOTA_AVAILABLE
            ),
            Arguments.of(
                1,
                0,
                false,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_SLOTS_AVAILABLE,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_QUOTA_AVAILABLE,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_QUOTA_AVAILABLE
            ),
            Arguments.of(
                1,
                1,
                false,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_SLOTS_AVAILABLE,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_QUOTA_AVAILABLE,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_SLOTS_AVAILABLE
            ),
            Arguments.of(
                0,
                0,
                true,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_SLOTS_AVAILABLE,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_QUOTA_AVAILABLE,
                BookingSlotNotFoundReason.NO_SOURCE_WAREHOUSE_SLOTS_AVAILABLE
            )
        );
    }

    private FreeSlotsResponse getFreeSlotResponse() {
        TimeSlotResponse timeSlotResponse1 = new TimeSlotResponse(from1, to1);
        TimeSlotResponse timeSlotResponse2 = new TimeSlotResponse(from2, to2);
        FreeSlotsForDayResponse freeSlotsForDayResponse =
            new FreeSlotsForDayResponse(
                LocalDate.of(2020, 7, 10),
                ZoneOffset.UTC,
                List.of(timeSlotResponse1, timeSlotResponse2)
            );
        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
            new WarehouseFreeSlotsResponse(1L, List.of(freeSlotsForDayResponse));
        return new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse));
    }

    private FreeSlotsResponse getEmptyFreeSlotResponse() {
        return new FreeSlotsResponse(Collections.emptyList());
    }

    private BookSlotResponse getBookSlotResponse() {
        return new BookSlotResponse(1L, 1L, ZonedDateTime.now(), ZonedDateTime.now());
    }

    private static Map<Integer, WeeklySchedule> mockedWeeklyScheduleMap() {
        return IntStream.rangeClosed(1, 5)
            .mapToObj(i -> new WeeklySchedule(i, LocalTime.of(9, 0), LocalTime.of(18, 0)))
            .collect(Collectors.toMap(WeeklySchedule::getDayOfWeek, Function.identity()));
    }

    @Value
    public class GetAvailableLimitRequestArgumentMatcher implements ArgumentMatcher<GetAvailableLimitRequest> {
        long warehouseId;
        BookingType bookingType;
        LocalDate date;

        @Override
        public boolean matches(GetAvailableLimitRequest argument) {
            return argument != null &&
                argument.getWarehouseId() == warehouseId &&
                argument.getBookingType() == bookingType &&
                argument.getSupplierType() == SupplierType.FIRST_PARTY &&
                Objects.equals(argument.getDates(), List.of(date));
        }
    }

}
