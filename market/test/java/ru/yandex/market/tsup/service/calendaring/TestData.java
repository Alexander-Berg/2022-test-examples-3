package ru.yandex.market.tsup.service.calendaring;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType;
import ru.yandex.market.tsup.core.pipeline.data.schedule.payload.PointParams;
import ru.yandex.market.tsup.service.data_provider.entity.calendaring.PointBookingInfoDto;
import ru.yandex.market.tsup.service.data_provider.entity.calendaring.TimeSlotDto;

@UtilityClass
class TestData {
    static final long ROUTE_ID = 1L;
    static final LocalDate DATE = LocalDate.of(2022, 1, 25);
    static final ZoneOffset ZONE_OFFSET = ZoneOffset.of("+0300");

    static final PointParams POINT_PARAMS_1 =
        new PointParams(null, null, 0, 0, LocalTime.of(10, 0), LocalTime.of(12, 0), 9, null, null);
    static final PointParams POINT_PARAMS_2 = new PointParams(null, null, 1, 0, null, null, 24, null, null);
    static final PointParams POINT_PARAMS_3 = new PointParams(null, null, 2, 1440, null, null, 24, null, null);
    static final PointParams POINT_PARAMS_4 = new PointParams(null, null, 3, 240, null, null, 9, null, null);

    static PointBookingInfoDto BOOKING_INFO_1_2 = new PointBookingInfoDto(
        List.of(0, 1),
        404L,
        BookingType.XDOCK_TRANSPORT_WITHDRAW,
        SupplierType.FIRST_PARTY,
        60,
        0,
        1,
        1
    );
    static PointBookingInfoDto BOOKING_INFO_3 = new PointBookingInfoDto(
        List.of(2),
        171L,
        BookingType.XDOCK_TRANSPORT_SUPPLY,
        SupplierType.FIRST_PARTY,
        60,
        1440,
        1,
        1
    );
    static PointBookingInfoDto BOOKING_INFO_4 = new PointBookingInfoDto(
        List.of(3),
        172L,
        BookingType.XDOCK_TRANSPORT_SUPPLY,
        SupplierType.FIRST_PARTY,
        30,
        240,
        1,
        1
    );
    static TimeSlotDto SLOT_1_2 = new TimeSlotDto(
        DATE,
        LocalTime.of(10, 0),
        LocalTime.of(11, 0),
        ZONE_OFFSET
    );
    static TimeSlotDto SLOT_3 = new TimeSlotDto(
        DATE.plusDays(1),
        LocalTime.of(11, 30),
        LocalTime.of(12, 30),
        ZONE_OFFSET
    );
    static TimeSlotDto SLOT_4 = new TimeSlotDto(
        DATE.plusDays(1),
        LocalTime.of(15, 30),
        LocalTime.of(16, 0),
        ZONE_OFFSET
    );
}
