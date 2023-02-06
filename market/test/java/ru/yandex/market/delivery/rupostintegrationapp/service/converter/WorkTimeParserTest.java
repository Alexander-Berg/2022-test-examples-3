package ru.yandex.market.delivery.rupostintegrationapp.service.converter;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.entities.common.TimeInterval;
import ru.yandex.market.delivery.entities.common.WorkTime;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;

class WorkTimeParserTest extends BaseTest {

    static Stream<Arguments> getParameters() {
        return Stream.of(
            Arguments.of(null, Collections.emptyList()),
            Arguments.of("", Collections.emptyList()),
            Arguments.of("09:00-17:00", Collections.emptyList()),
            Arguments.of("09:00-17:00 перерыв 12:30-13:00 перерыв[2] 16:30-17:00", Collections.emptyList()),
            Arguments.of("пн 09:00-17:00", stub1()),
            Arguments.of("пн: 09:00-17:00", stub1()),
            Arguments.of("пн 09:00-17:00, хз 09:00-17:00", stub1()),
            Arguments.of("пн 09:00-17:00, 09:00-17:00", stub1()),
            Arguments.of("пн,вт 09:00-17:00", stub2()),
            Arguments.of("пн,вт 09:00-17:00, сб 12:00-15:00", stub3()),
            Arguments.of("пн,вт: 09:00-17:00, сб 12:00-15:00", stub3()),
            Arguments.of("пн-пт 09:00-17:00", stub4()),
            Arguments.of("пн-пт 09:00-17:00, сб 12:00-15:00", stub5()),
            Arguments.of(
                "пн 09:00-17:00 перерыв 13:00-14:00, " +
                    "вт 08:00-17:00 перерыв 13:00-14:00, " +
                    "ср-пт 08:00-17:00 перерыв 14:00-15:00, " +
                    "сб 09:00-18:00 перерыв 13:00-14:00",
                stub6()
            ),
            Arguments.of(
                "пн-пт 10:00-21:00 перерыв 12:30-13:00 перерыв[2] 16:30-17:00, " +
                    "сб 08:00-17:00 перерыв 14:00-15:00, " +
                    "вс 08:00-17:00 перерыв 14:00-15:00",
                stub7()
            ),
            Arguments.of("пн-вс 00:00-24:00", stub8())
        );
    }

    private static List<WorkTime> stub1() {
        WorkTime workTime = new WorkTime();
        workTime.setDay(DayOfWeek.MONDAY.getValue());
        workTime.setPeriods(Collections.singletonList(new TimeInterval("09:00/17:00")));

        return Collections.singletonList(workTime);
    }

    private static List<WorkTime> stub2() {
        WorkTime workTime = new WorkTime();
        workTime.setDay(DayOfWeek.MONDAY.getValue());
        workTime.setPeriods(Collections.singletonList(new TimeInterval("09:00/17:00")));

        WorkTime workTime2 = new WorkTime();
        workTime2.setDay(DayOfWeek.TUESDAY.getValue());
        workTime2.setPeriods(Collections.singletonList(new TimeInterval("09:00/17:00")));

        return Arrays.asList(workTime, workTime2);
    }

    private static List<WorkTime> stub3() {
        WorkTime workTime = buildWorkTime(1);
        WorkTime workTime2 = buildWorkTime(2);

        WorkTime workTime3 = new WorkTime();
        workTime3.setDay(DayOfWeek.SATURDAY.getValue());
        workTime3.setPeriods(Collections.singletonList(new TimeInterval("12:00/15:00")));

        return Arrays.asList(workTime, workTime2, workTime3);
    }

    private static List<WorkTime> stub4() {
        return IntStream
            .range(1, 6)
            .mapToObj(WorkTimeParserTest::buildWorkTime)
            .collect(Collectors.toList());
    }

    private static List<WorkTime> stub5() {
        List<WorkTime> workTimes = IntStream
            .range(1, 6)
            .mapToObj(WorkTimeParserTest::buildWorkTime)
            .collect(Collectors.toList());

        WorkTime workTime = new WorkTime();
        workTime.setDay(DayOfWeek.SATURDAY.getValue());
        workTime.setPeriods(Collections.singletonList(new TimeInterval("12:00/15:00")));

        workTimes.add(workTime);

        return workTimes;
    }

    private static List<WorkTime> stub6() {

        List<WorkTime> workTimes = new ArrayList<>();

        WorkTime workTime = new WorkTime();
        workTime.setDay(DayOfWeek.MONDAY.getValue());
        workTime.setPeriods(
            Arrays.asList(
                new TimeInterval("09:00/13:00"),
                new TimeInterval("14:00/17:00")
            )
        );

        WorkTime workTime2 = new WorkTime();
        workTime2.setDay(DayOfWeek.TUESDAY.getValue());
        workTime2.setPeriods(
            Arrays.asList(
                new TimeInterval("08:00/13:00"),
                new TimeInterval("14:00/17:00")
            )
        );

        List<WorkTime> mediumDays = IntStream
            .range(3, 6)
            .mapToObj(WorkTimeParserTest::buildWorkTimeWithBreak)
            .collect(Collectors.toList());

        WorkTime workTime3 = new WorkTime();
        workTime3.setDay(DayOfWeek.SATURDAY.getValue());
        workTime3.setPeriods(
            Arrays.asList(
                new TimeInterval("09:00/13:00"),
                new TimeInterval("14:00/18:00")
            )
        );

        workTimes.add(workTime);
        workTimes.add(workTime2);
        workTimes.addAll(mediumDays);
        workTimes.add(workTime3);

        return workTimes;
    }

    private static List<WorkTime> stub7() {
        List<WorkTime> workTimes = new ArrayList<>();

        List<WorkTime> days = IntStream
            .range(1, 6)
            .mapToObj(WorkTimeParserTest::buildWorkTimeWithDoubleBreak)
            .collect(Collectors.toList());

        WorkTime saturday = buildWorkTimeWithBreak(6);
        WorkTime sunday = buildWorkTimeWithBreak(7);

        workTimes.addAll(days);
        workTimes.add(saturday);
        workTimes.add(sunday);

        return workTimes;
    }

    private static List<WorkTime> stub8() {
        return IntStream
            .range(1, 8)
            .mapToObj(WorkTimeParserTest::buildNonStopWorkTime)
            .collect(Collectors.toList());
    }

    private static WorkTime buildNonStopWorkTime(int i) {
        WorkTime workTime = buildWorkTime(i);
        workTime.setPeriods(
            Collections.singletonList(
                new TimeInterval("00:00/24:00")
            )
        );
        return workTime;
    }

    private static WorkTime buildWorkTimeWithDoubleBreak(int i) {
        WorkTime workTime = buildWorkTime(i);

        workTime.setPeriods(
            Arrays.asList(
                new TimeInterval("10:00/12:30"),
                new TimeInterval("13:00/16:30"),
                new TimeInterval("17:00/21:00")
            )
        );
        return workTime;
    }

    private static WorkTime buildWorkTime(int i) {
        WorkTime workTime = new WorkTime();
        workTime.setDay(i);
        workTime.setPeriods(Collections.singletonList(new TimeInterval("09:00/17:00")));
        return workTime;
    }

    private static WorkTime buildWorkTimeWithBreak(int i) {
        WorkTime workTime = buildWorkTime(i);
        workTime.setPeriods(
            Arrays.asList(
                new TimeInterval("08:00/14:00"),
                new TimeInterval("15:00/17:00")
            )
        );
        return workTime;
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void parse(String incomingString, List<WorkTime> workTimes) throws Exception {
        softly.assertThat(new WorkTimeParser().parse(incomingString))
            .as("Asserting the parsed work times is valid")
            .isEqualTo(workTimes);
    }
}
