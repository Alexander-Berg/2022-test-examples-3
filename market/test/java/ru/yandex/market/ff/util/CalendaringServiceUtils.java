package ru.yandex.market.ff.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ru.yandex.market.ff.configuration.DateTimeTestConfig;
import ru.yandex.market.logistic.api.utils.TimeZoneUtil;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsForDayResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.TimeSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseFreeSlotsResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CalendaringServiceUtils {

    private CalendaringServiceUtils() {
        throw new AssertionError();
    }

    public static BookSlotResponse createBookSlotResponse(long id) {
        return new BookSlotResponse(
                id,
                2,
                ZonedDateTime.now(),
                ZonedDateTime.now()
        );
    }

    public static void mockGetFreeSlots(int daysFromNow, CalendaringServiceClientApi csClient) {
        mockGetFreeSlots(DateTimeTestConfig.FIXED_NOW_INSTANT, daysFromNow, csClient);
    }

    public static void mockGetSlotsWithoutQuotaCheck(int daysFromNow, CalendaringServiceClientApi csClient) {
        mockGetSlotsWithoutQuotaCheck(DateTimeTestConfig.FIXED_NOW_INSTANT, daysFromNow, csClient);
    }

    public static void mockGetFreeSlots(Instant nowInstant, int daysFromNow, CalendaringServiceClientApi csClient) {
        FreeSlotsResponse freeSlotsResponse =
                getFreeSlotsResponse(nowInstant, daysFromNow);

        when(csClient.getFreeSlots(any())).thenReturn(freeSlotsResponse);
    }

    public static void mockGetSlotsWithoutQuotaCheck(Instant nowInstant, int daysFromNow,
                                                     CalendaringServiceClientApi csClient) {
        FreeSlotsResponse freeSlotsResponse =
                getFreeSlotsResponse(nowInstant, daysFromNow);

        when(csClient.getSlotsWithoutQuotaCheck(any())).thenReturn(freeSlotsResponse);
    }

    @NotNull
    private static FreeSlotsResponse getFreeSlotsResponse(Instant nowInstant, int daysFromNow) {
        List<TimeSlotResponse> slots = List.of(
                new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                new TimeSlotResponse(LocalTime.of(9, 30), LocalTime.of(10, 0)),
                new TimeSlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 30)),
                new TimeSlotResponse(LocalTime.of(10, 30), LocalTime.of(11, 0)),
                new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(11, 30)),
                new TimeSlotResponse(LocalTime.of(11, 30), LocalTime.of(12, 0))
        );

        LocalDate now = LocalDateTime.ofInstant(nowInstant, TimeZoneUtil.DEFAULT_OFFSET)
                .toLocalDate();

        List<WarehouseFreeSlotsResponse> responses = new ArrayList<>();
        for (int i = 0; i < daysFromNow; i++) {
            LocalDate date = now.plusDays(i);

            List<FreeSlotsForDayResponse> freeSlotsForDayResponses =
                    List.of(new FreeSlotsForDayResponse(date, ZoneOffset.of("+03:00"), slots));

            responses.add(new WarehouseFreeSlotsResponse(172, freeSlotsForDayResponses));
        }

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(responses);
        return freeSlotsResponse;
    }
}
