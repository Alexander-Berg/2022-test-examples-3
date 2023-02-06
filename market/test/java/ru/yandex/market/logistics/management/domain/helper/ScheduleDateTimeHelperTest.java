package ru.yandex.market.logistics.management.domain.helper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.dto.utils.ScheduleDateTimeHelper;
import ru.yandex.market.logistics.management.domain.entity.Schedule;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDateTimeResponse;

public class ScheduleDateTimeHelperTest extends AbstractTest {
    private static final Set<LocalDate> HOLIDAYS = new HashSet<>() {
        {
            add(LocalDate.of(2019, 11, 9));
            add(LocalDate.of(2019, 11, 10));
            add(LocalDate.of(2019, 11, 16));
            add(LocalDate.of(2019, 11, 17));
        }
    };

    private static final LocalTime TIME_FROM = LocalTime.of(8, 0);
    private static final LocalTime TIME_TO = LocalTime.of(18, 0);
    private static final LocalDate DATE_FROM = LocalDate.of(2019, 11, 4);
    private static final LocalDate DATE_FROM_WHOLE_MONTH = LocalDate.of(2020, 5, 22);
    private static final LocalDate DATE_TO = LocalDate.of(2019, 11, 19);
    private static final LocalDate DATE_TO_WHOLE_MONTH = LocalDate.of(2020, 6, 22);

    @Test
    void mergeScheduleAndHolidays() {
        softly.assertThat(getResponse())
            .as("Schedule date time response should match converter result")
            .containsExactlyElementsOf(ScheduleDateTimeHelper.mergeScheduleAndHolidays(
                new Schedule().setScheduleDays(getScheduleDays(5)),
                HOLIDAYS,
                DATE_FROM,
                DATE_TO)
            );
    }

    @Test
    void mergeScheduleAndHolidaysWholeMonthWithoutHolidays() {
        softly.assertThat(getMonthResponse())
            .containsExactlyElementsOf(ScheduleDateTimeHelper.mergeScheduleAndHolidays(
                new Schedule().setScheduleDays(getScheduleDays(7)),
                Set.of(),
                DATE_FROM_WHOLE_MONTH,
                DATE_TO_WHOLE_MONTH)
            );
    }

    private static Set<ScheduleDay> getScheduleDays(int daysOfWeek) {
        return IntStream.range(1, daysOfWeek + 1)
            .mapToObj(day -> new ScheduleDay()
                .setDay(day)
                .setFrom(TIME_FROM)
                .setTo(TIME_TO)
            )
            .collect(Collectors.toSet());
    }

    private static List<ScheduleDateTimeResponse> getResponse() {
        return IntStream.concat(
            IntStream.concat(
                IntStream.range(4, 9),
                IntStream.range(11, 16)),
            IntStream.range(18, 19)
        ).mapToObj(
            day -> ScheduleDateTimeResponse.newBuilder()
                .date(LocalDate.of(2019, 11, day))
                .from(TIME_FROM)
                .to(TIME_TO)
                .build()
        ).collect(Collectors.toList());
    }

    private static List<ScheduleDateTimeResponse> getMonthResponse() {
        List<ScheduleDateTimeResponse> daysInMay = IntStream.range(22, 32)
            .mapToObj(day -> ScheduleDateTimeResponse.newBuilder()
                .date(LocalDate.of(2020, 5, day))
                .from(TIME_FROM)
                .to(TIME_TO)
                .build()
            )
            .collect(Collectors.toList());

        List<ScheduleDateTimeResponse> daysInJune = IntStream.range(1, 22)
            .mapToObj(day -> ScheduleDateTimeResponse.newBuilder()
                .date(LocalDate.of(2020, 6, day))
                .from(TIME_FROM)
                .to(TIME_TO)
                .build()
            ).collect(Collectors.toList());

        List<ScheduleDateTimeResponse> response = new ArrayList<>();
        response.addAll(daysInMay);
        response.addAll(daysInJune);
        return response;
    }
}
