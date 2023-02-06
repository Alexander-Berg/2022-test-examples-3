package ru.yandex.market.logistics.management.domain.converter.admin;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.dto.front.ScheduleDto;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.exception.BadRequestException;

class AdminScheduleConverterTest extends AbstractTest {

    @Test
    void toScheduleDayResponse_shouldReturnEmptyResult_whenTimeFromAndTimeToAreNull() {
        // when:
        var actual = AdminScheduleConverter.toScheduleDayResponse(DayOfWeek.MONDAY, null, null);

        // then:
        softly.assertThat(actual).isEmpty();
    }

    @Test
    void toScheduleDtoTest() {
        Set<ScheduleDay> scheduleDays = Set.of(
            new ScheduleDay().setDay(1).setFrom(LocalTime.NOON).setTo(LocalTime.MIDNIGHT).setIsMain(true),
            new ScheduleDay().setDay(2).setFrom(LocalTime.NOON).setTo(LocalTime.MIDNIGHT).setIsMain(true),
            new ScheduleDay().setDay(3).setFrom(LocalTime.NOON).setTo(LocalTime.MIDNIGHT).setIsMain(true)
        );

        ScheduleDto mainSchedule = new ScheduleDto().setMondayFrom(LocalTime.NOON).setMondayTo(LocalTime.MIDNIGHT)
            .setTuesdayFrom(LocalTime.NOON).setTuesdayTo(LocalTime.MIDNIGHT)
            .setWednesdayFrom(LocalTime.NOON).setWednesdayTo(LocalTime.MIDNIGHT);

        softly.assertThat(AdminScheduleConverter.toScheduleDto(scheduleDays)).isEqualTo(mainSchedule);
        softly.assertThat(AdminScheduleConverter.toSecondWaveScheduleDto(scheduleDays)).isEqualTo(new ScheduleDto());
    }

    @Test
    void toSecondWaveScheduleDto() {
        Set<ScheduleDay> scheduleDays = Set.of(
            new ScheduleDay().setDay(1).setFrom(LocalTime.NOON).setTo(LocalTime.MIDNIGHT).setIsMain(false),
            new ScheduleDay().setDay(2).setFrom(LocalTime.NOON).setTo(LocalTime.MIDNIGHT).setIsMain(false),
            new ScheduleDay().setDay(3).setFrom(LocalTime.NOON).setTo(LocalTime.MIDNIGHT).setIsMain(false)
        );

        ScheduleDto secondWaveSchedule = new ScheduleDto().setMondayFrom(LocalTime.NOON).setMondayTo(LocalTime.MIDNIGHT)
            .setTuesdayFrom(LocalTime.NOON).setTuesdayTo(LocalTime.MIDNIGHT)
            .setWednesdayFrom(LocalTime.NOON).setWednesdayTo(LocalTime.MIDNIGHT);

        softly.assertThat(AdminScheduleConverter.toScheduleDto(scheduleDays)).isEqualTo(new ScheduleDto());
        softly.assertThat(AdminScheduleConverter.toSecondWaveScheduleDto(scheduleDays)).isEqualTo(secondWaveSchedule);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void toScheduleDayResponse_shouldReturnNotEmptyResult(
        DayOfWeek dayOfWeek,
        @Nullable LocalTime timeFrom,
        @Nullable LocalTime timeTo,
        ScheduleDayResponse expected
    ) {
        // when:
        Optional<ScheduleDayResponse> actual =
            AdminScheduleConverter.toScheduleDayResponse(dayOfWeek, timeFrom, timeTo);

        // then:
        softly.assertThat(actual).isNotEmpty();
        softly.assertThat(actual.get()).isEqualTo(expected);
    }

    @Nonnull
    private static Stream<Arguments> toScheduleDayResponse_shouldReturnNotEmptyResult() {
        return Stream.of(
            Arguments.of(
                DayOfWeek.MONDAY,
                LocalTime.MIN,
                LocalTime.MAX,
                new ScheduleDayResponse(null, 1, LocalTime.MIN, LocalTime.MAX)
            ),
            Arguments.of(
                DayOfWeek.TUESDAY,
                LocalTime.MIDNIGHT,
                LocalTime.NOON,
                new ScheduleDayResponse(null, 2, LocalTime.MIDNIGHT, LocalTime.NOON)
            ),
            Arguments.of(
                DayOfWeek.WEDNESDAY,
                LocalTime.of(12, 34, 56),
                LocalTime.of(23, 45, 6),
                new ScheduleDayResponse(null, 3, LocalTime.of(12, 34, 56), LocalTime.of(23, 45, 6))
            ),
            Arguments.of(
                DayOfWeek.THURSDAY,
                LocalTime.NOON,
                LocalTime.NOON,
                new ScheduleDayResponse(null, 4, LocalTime.NOON, LocalTime.NOON)
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void toScheduleDayResponse_shouldThrowBadRequestException(
        DayOfWeek dayOfWeek,
        @Nullable LocalTime timeFrom,
        @Nullable LocalTime timeTo,
        String message
    ) {
        // expect:
        softly.assertThatThrownBy(() -> AdminScheduleConverter.toScheduleDayResponse(dayOfWeek, timeFrom, timeTo))
            .isInstanceOf(BadRequestException.class)
            .hasMessage(message);
    }

    @Nonnull
    private static Stream<Arguments> toScheduleDayResponse_shouldThrowBadRequestException() {
        return Stream.of(
            Arguments.of(
                DayOfWeek.FRIDAY,
                null,
                LocalTime.MAX,
                "400 BAD_REQUEST \"Для дня недели \"пятница\" не указано время начала\""
            ),
            Arguments.of(
                DayOfWeek.SATURDAY,
                LocalTime.MIN,
                null,
                "400 BAD_REQUEST \"Для дня недели \"суббота\" не указано время окончания\""
            ),
            Arguments.of(
                DayOfWeek.SUNDAY,
                LocalTime.NOON.plusNanos(1),
                LocalTime.NOON,
                "400 BAD_REQUEST \"Для дня недели \"воскресенье\" "
                    + "время окончания должно быть не меньше, чем время начала\""
            )
        );
    }

}
