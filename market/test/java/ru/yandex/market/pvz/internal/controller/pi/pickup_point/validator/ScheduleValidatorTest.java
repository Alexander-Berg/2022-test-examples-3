package ru.yandex.market.pvz.internal.controller.pi.pickup_point.validator;

import java.time.DayOfWeek;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointScheduleDaysDto;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleValidatorTest {

    private final ScheduleValidator validator = new ScheduleValidator();

    @Test
    void validSchedule() {
        var schedule = List.of(
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.TUESDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.THURSDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.FRIDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.SATURDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.SUNDAY)
                        .build()
        );

        assertThat(validator.isValid(schedule, null)).isTrue();
    }

    @Test
    void NotAllDaysOfWeek() {
        var schedule = List.of(
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.TUESDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.FRIDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.SATURDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.SUNDAY)
                        .build()
        );

        assertThat(validator.isValid(schedule, null)).isFalse();
    }

    @Test
    void extraDaysOfWeek() {
        var schedule = List.of(
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.TUESDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.THURSDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.FRIDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.SATURDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.SUNDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build()
        );

        assertThat(validator.isValid(schedule, null)).isFalse();
    }

    @Test
    void RepeatedDaysOfWeek() {
        var schedule = List.of(
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.THURSDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.FRIDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.SATURDAY)
                        .build(),
                PickupPointScheduleDaysDto.builder()
                        .dayOfWeek(DayOfWeek.SUNDAY)
                        .build()
        );

        assertThat(validator.isValid(schedule, null)).isFalse();
    }

    @Test
    void nullSchedule() {
        assertThat(validator.isValid(null, null)).isFalse();
    }
}
