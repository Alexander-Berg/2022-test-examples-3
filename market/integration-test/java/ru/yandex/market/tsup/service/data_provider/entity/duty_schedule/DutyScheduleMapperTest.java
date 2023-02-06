package ru.yandex.market.tsup.service.data_provider.entity.duty_schedule;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.service.data_provider.entity.duty_schedule.dto.DateIntervalDto;

class DutyScheduleMapperTest extends AbstractContextualTest {

    @Autowired
    private DutyScheduleMapper dutyScheduleMapper;

    @Test
    void shouldConvertToIntervals() {
        List<LocalDate> dates = List.of(
                LocalDate.of(2022, 5, 1),
                LocalDate.of(2022, 5, 2),
                LocalDate.of(2022, 5, 4),
                LocalDate.of(2022, 5, 5)
        );

        List<DateIntervalDto> intervalDtos = dutyScheduleMapper.toDatesInterval(dates);
        Assertions.assertThat(intervalDtos).hasSize(2);
        Assertions.assertThat(intervalDtos.get(0).getFrom()).isEqualTo(LocalDate.of(2022, 5, 1));
        Assertions.assertThat(intervalDtos.get(0).getTo()).isEqualTo(LocalDate.of(2022, 5, 2));
        Assertions.assertThat(intervalDtos.get(1).getFrom()).isEqualTo(LocalDate.of(2022, 5, 4));
        Assertions.assertThat(intervalDtos.get(1).getTo()).isEqualTo(LocalDate.of(2022, 5, 5));
    }

    @Test
    void shouldConvertFromIntervals() {
        List<DateIntervalDto> intevals = List.of(
                new DateIntervalDto(
                        LocalDate.of(2022, 5, 1),
                        LocalDate.of(2022, 5, 2)
                ),
                new DateIntervalDto(
                        LocalDate.of(2022, 5, 4),
                        LocalDate.of(2022, 5, 5)
                )
        );

        List<LocalDate> intervalDtos = dutyScheduleMapper.fromDatesInterval(intevals);
        Assertions.assertThat(intervalDtos).hasSize(4);
        Assertions.assertThat(intervalDtos).contains(
                LocalDate.of(2022, 5, 1),
                LocalDate.of(2022, 5, 2),
                LocalDate.of(2022, 5, 4),
                LocalDate.of(2022, 5, 5)
        );
    }

}
