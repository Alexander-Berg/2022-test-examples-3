package ru.yandex.market.tsup.util.converter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.calendaring.client.dto.BookSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsForDayResponse;
import ru.yandex.market.logistics.calendaring.client.dto.GetFreeSlotsRequest;
import ru.yandex.market.logistics.calendaring.client.dto.TimeSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType;
import ru.yandex.market.tsup.service.data_provider.entity.calendaring.BookedTimeSlotDto;
import ru.yandex.market.tsup.service.data_provider.entity.calendaring.TimeSlotDto;
import ru.yandex.market.tsup.service.data_provider.primitive.external.calendaring_service.search.SlotSearchProviderFilter;

class CalendaringServiceConverterTest {

    public static final LocalDate DATE = LocalDate.of(2022, 1, 25);

    @Test
    void getTimeSlotDtoStream() {
        List<TimeSlotDto> actual =
            CalendaringServiceConverter.getTimeSlotDtoStream(new FreeSlotsForDayResponse(
                    DATE,
                    ZoneOffset.UTC,
                    List.of(
                        new TimeSlotResponse(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                        new TimeSlotResponse(LocalTime.of(13, 0), LocalTime.of(14, 0)),
                        new TimeSlotResponse(LocalTime.of(14, 0), LocalTime.of(15, 0))
                    )
                ))
                .collect(Collectors.toList());

        Assertions.assertEquals(
            List.of(
                new TimeSlotDto(DATE, LocalTime.of(10, 0), LocalTime.of(11, 0), ZoneOffset.UTC),
                new TimeSlotDto(DATE, LocalTime.of(13, 0), LocalTime.of(14, 0), ZoneOffset.UTC),
                new TimeSlotDto(DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), ZoneOffset.UTC)
            ),
            actual
        );
    }

    @Test
    @SneakyThrows
    void createGetFreeSlotsFilter() {
        ObjectMapper om = new ObjectMapper();
        GetFreeSlotsRequest actual =
            CalendaringServiceConverter.createGetFreeSlotsFilter(new SlotSearchProviderFilter(
                172L,
                BookingType.XDOCK_TRANSPORT_WITHDRAW,
                60,
                DATE.atTime(10, 0),
                DATE.atTime(11, 0),
                SupplierType.FIRST_PARTY,
                1,
                2
            ));

        GetFreeSlotsRequest expected = new GetFreeSlotsRequest(
            Set.of(172L),
            null,
            BookingType.XDOCK_TRANSPORT_WITHDRAW,
            60,
            null,
            DATE.atTime(10, 0),
            DATE.atTime(11, 0),
            SupplierType.FIRST_PARTY,
            null,
            2,
            1,
            null
        );
        Assertions.assertEquals(om.writeValueAsString(expected), om.writeValueAsString(actual));
    }

    @Test
    void convert() {
        ZonedDateTime from = ZonedDateTime.of(2022, 1, 25, 10, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime to = ZonedDateTime.of(2022, 1, 25, 11, 0, 0, 0, ZoneOffset.UTC);

        BookedTimeSlotDto actual = CalendaringServiceConverter.convert(new BookSlotResponse(
            1L,
            10L,
            from,
            to
        ));

        Assertions.assertEquals(new BookedTimeSlotDto(
            1L,
            10L,
            from,
            to
        ), actual);
    }
}
