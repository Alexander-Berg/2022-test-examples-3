package ru.yandex.market.delivery.transport_manager.converter;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDockTimeSlotDto;

class TimeSlotConverterTest {

    static ZonedDateTime d1 = ZonedDateTime.of(
        2021, 4, 22, 16, 0, 0, 0, ZoneId.of("Z")
    );
    static ZonedDateTime d2 = ZonedDateTime.of(
        2021, 4, 22, 17, 0, 0, 0, ZoneId.of("Z")
    );
    private TimeSlotConverter timeSlotConverter;

    @BeforeEach
    void setUp() {
        timeSlotConverter = new TimeSlotConverter();
    }

    @Test
    void convert() {
        Assertions.assertThat(timeSlotConverter
                .convert(new XDockTimeSlotDto()
                    .setCalendaringServiceId(1L)
                    .setGateId(10L)
                    .setFromDate(d1)
                    .setToDate(d2)
                ))
            .isEqualTo(new TimeSlot()
                .setCalendaringServiceId(1L)
                .setGateId(10L)
                .setFromDate(d1.toLocalDateTime())
                .setZoneId("Z")
                .setToDate(d2.toLocalDateTime())
                .setId(null)
            );
    }
}
