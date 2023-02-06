package ru.yandex.market.ff.service.implementation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.dto.RequestedSlotDTO;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.configuration.DateTimeTestConfig;
import ru.yandex.market.ff.exception.http.BadRequestException;
import ru.yandex.market.ff.service.CalendaringService;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.LmsClientCachingService;
import ru.yandex.market.ff.service.RequestItemAttributeService;
import ru.yandex.market.ff.service.TimeSlotsService;
import ru.yandex.market.ff.util.CalendaringServiceUtils;
import ru.yandex.market.logistic.api.utils.TimeZoneUtil;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotRequest;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponse;
import ru.yandex.market.logistics.calendaring.client.dto.UpdateSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGatesScheduleResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDateTimeResponse;
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff.configuration.DateTimeTestConfig.FIXED_NOW;
import static ru.yandex.market.ff.service.implementation.calendaring.CalendaringSlotsHandler.VALID_STATUSES_FOR_GET_INBOUND_SLOTS;

@ActiveProfiles("CalendaringServiceTest")
public class CalendaringServiceTest extends IntegrationTest {

    @Autowired
    private CalendaringService service;

    @Autowired
    private LmsClientCachingService lmsClientCachingService;

    @Autowired
    private DateTimeService dateTimeService;

    @Autowired
    private TimeSlotsService timeSlotsService;

    @Autowired
    private CalendaringServiceClientApi calendaringServiceClient;

    @Autowired
    private RequestItemAttributeService requestItemAttributeService;

    @AfterEach
    void invalidate() {
        super.resetMocks();
        Mockito.reset(timeSlotsService);
        lmsClientCachingService.invalidateCache();
        when(dateTimeService.localDateTimeNow()).thenReturn(FIXED_NOW);
        requestItemAttributeService.invalidateCache();
    }

    @Test
    @DatabaseSetup(value = "classpath:service/calendaring/2/save_slots_before.xml")
    @ExpectedDatabase(value = "classpath:service/calendaring/2/save_slots_after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void takeSlot() {
        when(csClient.bookSlot(any())).thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));
        RequestedSlotDTO dto = new RequestedSlotDTO();
        dto.setDate(LocalDate.of(2018, 1, 5));
        dto.setFrom(LocalTime.of(10, 0));
        dto.setTo(LocalTime.of(10, 30));
        service.validateAndBookSlotForRequest(1L, dto);
    }

    @Test
    @DatabaseSetup(value = "classpath:service/calendaring/5/requested_date_change_before.xml")
    @ExpectedDatabase(value = "classpath:service/calendaring/5/requested_date_change_after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void requestedDateChangedIfSlotSelectedOnAnotherDate() {
        when(csClient.bookSlot(any())).thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));
        RequestedSlotDTO dto = new RequestedSlotDTO();
        dto.setDate(LocalDate.of(2018, 1, 6));
        dto.setFrom(LocalTime.of(10, 0));
        dto.setTo(LocalTime.of(10, 30));
        service.validateAndBookSlotForRequest(1L, dto);
    }


    @Test
    @DatabaseSetup(value = "classpath:service/calendaring/5/update_calendar_booking_before.xml")
    @ExpectedDatabase(value = "classpath:service/calendaring/5/update_calendar_booking_after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testUpdateCalendarBooking() {

        BookingResponse bookingResponse = new BookingResponse(1, "FFWF", "id100", null, 1,
                ZonedDateTime.of(2021, 10, 1, 10, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2021, 10, 1, 11, 0, 0, 0, ZoneId.of("UTC")),
                BookingStatus.ACTIVE,
                LocalDateTime.of(2021, 10, 1, 10, 0, 0, 0),
                123L
        );

        when(lmsClient.getWarehousesGatesScheduleByPartnerId(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(ImmutableList.of(
                        LogisticsPointGatesScheduleResponse.newBuilder()
                                .gates(getGate()).schedule(generateDaysFromSupplyFrom(2))
                                .build()
                ));
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0,
                ZoneId.systemDefault());
        when(calendaringServiceClient.updateSlot(any()))
                .thenReturn(new UpdateSlotResponse(2, 1, zonedDateTime, zonedDateTime.plusHours(1)));

        when(calendaringServiceClient.getSlotByExternalIdentifiers(any(), anyString(), any()))
                .thenReturn(new BookingListResponse(List.of(bookingResponse)));

        RequestedSlotDTO dto = new RequestedSlotDTO();
        dto.setDate(LocalDate.of(2018, 1, 6));
        dto.setFrom(LocalTime.of(10, 0));
        dto.setTo(LocalTime.of(10, 30));
        service.validateAndBookSlotForRequest(1L, dto);
    }

    @Test
    @DatabaseSetup(value = "classpath:" +
            "service/calendaring/11/update_calendar_booking_before.xml")
    @ExpectedDatabase(value = "classpath:service/calendaring/11/update_calendar_booking_after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void testUpdateCalendarBookingWhenNoCalendarBooking() {

        BookingResponse bookingResponse = new BookingResponse(1, "FFWF", "id100", null, 1,
                ZonedDateTime.of(2021, 10, 1, 10, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2021, 10, 1, 11, 0, 0, 0, ZoneId.of("UTC")),
                BookingStatus.ACTIVE,
                LocalDateTime.of(2021, 10, 1, 10, 0, 0, 0),
                123L
        );

        when(lmsClient.getWarehousesGatesScheduleByPartnerId(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(ImmutableList.of(
                        LogisticsPointGatesScheduleResponse.newBuilder()
                                .gates(getGate()).schedule(generateDaysFromSupplyFrom(2))
                                .build()
                ));
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0,
                ZoneId.systemDefault());
        when(calendaringServiceClient.updateSlot(any()))
                .thenReturn(new UpdateSlotResponse(2, 1, zonedDateTime, zonedDateTime.plusHours(1)));

        when(calendaringServiceClient.getSlotByExternalIdentifiers(any(), anyString(), any()))
                .thenReturn(new BookingListResponse(List.of(bookingResponse)));

        RequestedSlotDTO dto = new RequestedSlotDTO();
        dto.setDate(LocalDate.of(2018, 1, 6));
        dto.setFrom(LocalTime.of(10, 0));
        dto.setTo(LocalTime.of(10, 30));
        service.validateAndBookSlotForRequest(1L, dto);
    }

    @Test
    @DatabaseSetup(value = "classpath:service/calendaring/3/before.xml")
    @ExpectedDatabase(value = "classpath:service/calendaring/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void testValidateAndBookSlotForRequestWithCtm() {
        ArgumentMatcher<BookSlotRequest> matchers = x -> {
            Map<String, Boolean> attribute = (Map<String, Boolean>) x.getMeta().get("attribute");
            return Boolean.TRUE.equals(attribute.get("CTM")) && x.getSkipValidatedSlotDuration();
        };

        when(csClient.bookSlot(argThat(matchers)))
                .thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));

        RequestedSlotDTO dto = new RequestedSlotDTO();
        dto.setDate(LocalDate.of(2018, 1, 6));
        dto.setFrom(LocalTime.of(10, 0));
        dto.setTo(LocalTime.of(11, 0));

        service.validateAndBookSlotForRequest(1L, dto);

        verify(csClient).bookSlot(argThat(matchers));
    }

    private static List<ScheduleDateTimeResponse> generateDaysFromNow(int numberOfDays) {
        return generateDays(DateTimeTestConfig.FIXED_NOW_INSTANT, numberOfDays);
    }

    private static List<ScheduleDateTimeResponse> generateDaysFromSupplyFrom(int numberOfDays) {
        return generateDays(DateTimeTestConfig.FIXED_SUPPLY_FROM, numberOfDays);
    }

    private static List<ScheduleDateTimeResponse> generateDays(Instant instantFrom, int numberOfDays) {
        LocalDateTime from = LocalDateTime.ofInstant(instantFrom, TimeZoneUtil.DEFAULT_OFFSET);
        List<ScheduleDateTimeResponse> days = new ArrayList<>(numberOfDays);
        LocalDate dateIterator = from.toLocalDate();
        for (int day = 0; day < numberOfDays; day++) {
            days.add(
                    ScheduleDateTimeResponse.newBuilder().date(dateIterator)
                            .from(LocalTime.of(0, 0))
                            .to(LocalTime.of(0, 0)).build()
            );
            dateIterator = dateIterator.plusDays(1);
        }
        return days;
    }

    private void assertGetFreeSlotsThrowsException(long requestId, @Nonnull String status,
                                                   @Nonnull String calendaringMode) {
        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> service.getFreeSlotsForRequest(requestId, null,
                        null, null));
        assertExpectedExceptionThrowsForIncorrectRequest(requestId, status, calendaringMode, exception,
                VALID_STATUSES_FOR_GET_INBOUND_SLOTS);
    }

    private void assertTakeSlotThrowsException(long requestId, @Nonnull String status,
                                               @Nonnull String calendaringMode,
                                               Set<RequestStatus> validStatuses) {
        BadRequestException exception =
                assertThrows(BadRequestException.class,
                        () -> service.validateAndBookSlotForRequest(requestId, createRequestedSlot()));
        assertExpectedExceptionThrowsForIncorrectRequest(requestId, status, calendaringMode, exception, validStatuses);
    }

    private void assertExpectedExceptionThrowsForIncorrectRequest(long requestId, @Nonnull String status,
                                                                  @Nonnull String calendaringMode,
                                                                  @Nonnull BadRequestException exception,
                                                                  Set<RequestStatus> validStatuses) {
        String statuses = validStatuses.stream().map(RequestStatus::name).collect(Collectors.joining(", "));
        String expectedMessage =
                String.format("Select slot is possible only for calendaring requests in next statuses: " +
                                "%s, but request %d has status %s and calendaring mode %s",
                        statuses, requestId, status, calendaringMode);
        assertions.assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @NotNull
    private ImmutableSet<LogisticsPointGateResponse> getGate() {
        return ImmutableSet.of(
                LogisticsPointGateResponse.newBuilder().id(0L).gateNumber("0")
                        .types(EnumSet.of(GateTypeResponse.INBOUND)).enabled(true)
                        .build()
        );
    }

    @Nonnull
    private RequestedSlotDTO createRequestedSlot() {
        RequestedSlotDTO slot = new RequestedSlotDTO();
        slot.setDate(LocalDate.of(2018, 1, 7));
        slot.setFrom(LocalTime.of(10, 0));
        slot.setTo(LocalTime.of(11, 0));
        return slot;
    }
}
