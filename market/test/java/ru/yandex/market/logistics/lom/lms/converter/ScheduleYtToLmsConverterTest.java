package ru.yandex.market.logistics.lom.lms.converter;

import java.time.LocalTime;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.service.yt.dto.YtScheduleDays;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

@ParametersAreNonnullByDefault
@DisplayName("Конвертация расписания из моделей yt в модели lms")
class ScheduleYtToLmsConverterTest extends AbstractTest {

    private final ScheduleYtToLmsConverter scheduleYtToLmsConverter = new ScheduleYtToLmsConverter(objectMapper);

    @Test
    @DisplayName("Конвертация модели с несколькими днями в расписании")
    void convertWithMultiDays() {
        YtScheduleDays ytScheduleDays = new YtScheduleDays()
            .setId(1L)
            .setScheduleDays(
                "{\"schedule_days\":["
                    + "{\"day\":1,"
                    + "\"id\":395903,"
                    + "\"is_main\":true,"
                    + "\"time_from\":\"12:00:00.000000\","
                    + "\"time_to\":\"21:00:00.000000\""
                    + "},"
                    + "{\"day\":2,"
                    + "\"id\":395904,"
                    + "\"is_main\":false,"
                    + "\"time_from\":"
                    + "\"08:00:00.000000\","
                    + "\"time_to\":"
                    + "\"23:00:00.000000\""
                    + "}]}"
            );

        Set<ScheduleDayResponse> expectedDays = Set.of(
            new ScheduleDayResponse(395903L, 1, LocalTime.of(12, 0), LocalTime.of(21, 0), true),
            new ScheduleDayResponse(395904L, 2, LocalTime.of(8, 0), LocalTime.of(23, 0), false)
        );

        softly.assertThat(scheduleYtToLmsConverter.convert(ytScheduleDays))
            .hasSameElementsAs(expectedDays)
            .hasSize(expectedDays.size());
    }

    @Test
    @DisplayName("Конвертация с одним днем в расписании")
    void convertWithOneDay() {
        YtScheduleDays ytScheduleDays = new YtScheduleDays()
            .setId(1L)
            .setScheduleDays(
                "{\"schedule_days\":["
                    + "{\"day\":1,"
                    + "\"id\":395903,"
                    + "\"is_main\":true,"
                    + "\"time_from\":\"12:00:00.000000\","
                    + "\"time_to\":\"21:00:00.000000\""
                    + "}"
                    + "]}"
            );

        Set<ScheduleDayResponse> expectedDays = Set.of(
            new ScheduleDayResponse(395903L, 1, LocalTime.of(12, 0), LocalTime.of(21, 0), true)
        );

        softly.assertThat(scheduleYtToLmsConverter.convert(ytScheduleDays))
            .isEqualTo(expectedDays);
    }

    @Test
    @DisplayName("Конвертация с пустым списком дней в расписании")
    void convertWithoutDays() {
        YtScheduleDays ytScheduleDays = new YtScheduleDays()
            .setId(1L)
            .setScheduleDays(
                "{\"schedule_days\":[]}"
            );

        softly.assertThat(scheduleYtToLmsConverter.convert(ytScheduleDays))
            .isEmpty();
    }

    @Test
    @DisplayName("При изменении формата хранения json в yt конвертер падает")
    void failOnJsonFormatChanges() {
        YtScheduleDays dayWithIncorrectDaysJson = new YtScheduleDays()
            .setId(1L)
            .setScheduleDays(
                "{["
                    + "{\"day\":1,"
                    + "\"id\":395903,"
                    + "\"is_main\":true,"
                    + "\"time_from\":\"12:00:00.000000\","
                    + "\"time_to\":\"21:00:00.000000\""
                    + "}"
                    + "]}"
            );

        softly.assertThatCode(
            () -> scheduleYtToLmsConverter.convert(dayWithIncorrectDaysJson)
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Unexpected character");
    }

    @Test
    @DisplayName("Конвертация с null")
    void convertNullValue() {
        softly.assertThat(scheduleYtToLmsConverter.convert((YtScheduleDays) null)).isNull();
    }

    @Test
    @DisplayName("Конвертация с не заполненной строкой расписаний")
    void convertWithBlankPhones() {
        softly.assertThat(scheduleYtToLmsConverter.convert(new YtScheduleDays().setScheduleDays(null))).isEmpty();
    }
}
