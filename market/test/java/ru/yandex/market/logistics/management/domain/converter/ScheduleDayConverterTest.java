package ru.yandex.market.logistics.management.domain.converter;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

class ScheduleDayConverterTest extends AbstractTest {

    private static final ScheduleDayResponse SCHEDULE_DAY_DTO = new ScheduleDayResponse(
        1L,
        2,
        LocalTime.of(10, 0),
        LocalTime.of(18, 0),
        false
    );
    private static final ScheduleDay SCHEDULE_DAY_ENTITY =
        new ScheduleDay()
            .setId(1L)
            .setDay(2)
            .setFrom(LocalTime.of(10, 0))
            .setTo(LocalTime.of(18, 0));

    private static final ScheduleDayConverter CONVERTER = new ScheduleDayConverter();

    @Test
    void convertToDto() {
        softly.assertThat(SCHEDULE_DAY_DTO).isEqualToComparingFieldByField(CONVERTER.toDto(SCHEDULE_DAY_ENTITY));
    }

    @Test
    void convertToEntity() {
        softly.assertThat(SCHEDULE_DAY_ENTITY).isEqualTo(CONVERTER.toEntity(SCHEDULE_DAY_DTO));
    }

}
