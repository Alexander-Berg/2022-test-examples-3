package ru.yandex.market.tsup.core.converter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleHolidayDto;
import ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.DateIntervalDto;

class RouteScheduleFromTMConverterUnitTest {

    @Test
    void intervalsFromDates1() {
        Set<RouteScheduleHolidayDto> holidaysFromTm = Set.of(
            new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 3, 7)),
            new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 3, 8)),

            new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 1, 1)),
            new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 1, 2)),
            new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 1, 3)),
            new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 1, 4)),

            new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 5, 1)),

            new RouteScheduleHolidayDto().setDate(LocalDate.of(2020, 12, 31)),
            new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 1, 5)),

            new RouteScheduleHolidayDto().setDate(LocalDate.of(2021, 10, 4))
        );

        List<DateIntervalDto> intervals = List.of(
            new DateIntervalDto(LocalDate.of(2020, 12, 31), LocalDate.of(2021, 1, 5)),
            new DateIntervalDto(LocalDate.of(2021, 3, 7), LocalDate.of(2021, 3, 8)),
            new DateIntervalDto(LocalDate.of(2021, 5, 1), LocalDate.of(2021, 5, 1)),
            new DateIntervalDto(LocalDate.of(2021, 10, 4), LocalDate.of(2021, 10, 4))
        );

        Assertions.assertThat(RouteScheduleFromTMConverter.intervalsFromDates(holidaysFromTm)).isEqualTo(intervals);
    }
}