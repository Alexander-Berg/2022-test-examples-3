package ru.yandex.market.crm.triggers.services;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.junit.Test;

import ru.yandex.market.crm.domain.report.Outlet;
import ru.yandex.market.crm.triggers.utils.OutletScheduleParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OutletScheduleParserTest {
    @Test
    public void testOneDayPeriod() {
        String raw = """
                <WorkingTime>
                                <WorkingDaysFrom>1</WorkingDaysFrom>
                                <WorkingDaysTill>1</WorkingDaysTill>
                                <WorkingHoursFrom>10:00</WorkingHoursFrom>
                                <WorkingHoursTill>20:00</WorkingHoursTill>
                                <Break>
                                    <HoursFrom>12:00</HoursFrom>
                                    <HoursTill>12:30</HoursTill>
                                </Break>
                                <Break>
                                    <HoursFrom>18:00</HoursFrom>
                                    <HoursTill>18:30</HoursTill>
                                </Break>
                </WorkingTime>
                """;

        var parsed = OutletScheduleParser.parse(raw);

        assertEquals(1, parsed.size());
        var expectedBrakes = Arrays.asList(
                new OutletScheduleParser.Break("12:00", "12:30"),
                new OutletScheduleParser.Break("18:00", "18:30")
        );
        var expected = new OutletScheduleParser.WorkingTime(1, "10:00", 1, "20:00", expectedBrakes);
        assertEqualDayTimeRange(expected, parsed.get(0));
    }

    @Test
    public void testHumanStringParse() {
        String raw =
                """
                        <WorkingTime>
                                        <WorkingDaysFrom>1</WorkingDaysFrom>
                                        <WorkingDaysTill>1</WorkingDaysTill>
                                        <WorkingHoursFrom>10:00</WorkingHoursFrom>
                                        <WorkingHoursTill>20:00</WorkingHoursTill>
                                        <Break>
                                            <HoursFrom>18:00</HoursFrom>
                                            <HoursTill>18:30</HoursTill>
                                        </Break>
                        </WorkingTime>
                        <WorkingTime>
                                        <WorkingDaysFrom>2</WorkingDaysFrom>
                                        <WorkingDaysTill>2</WorkingDaysTill>
                                        <WorkingHoursFrom>10:00</WorkingHoursFrom>
                                        <WorkingHoursTill>20:00</WorkingHoursTill>
                                        <Break>
                                            <HoursFrom>18:00</HoursFrom>
                                            <HoursTill>18:30</HoursTill>
                                        </Break>
                        </WorkingTime>
                        <WorkingTime>
                                        <WorkingDaysFrom>3</WorkingDaysFrom>
                                        <WorkingDaysTill>3</WorkingDaysTill>
                                        <WorkingHoursFrom>10:00</WorkingHoursFrom>
                                        <WorkingHoursTill>20:00</WorkingHoursTill>
                                        <Break>
                                            <HoursFrom>15:00</HoursFrom>
                                            <HoursTill>15:30</HoursTill>
                                        </Break>
                        </WorkingTime>
                        <WorkingTime>
                                        <WorkingDaysFrom>4</WorkingDaysFrom>
                                        <WorkingDaysTill>4</WorkingDaysTill>
                                        <WorkingHoursFrom>10:00</WorkingHoursFrom>
                                        <WorkingHoursTill>18:00</WorkingHoursTill>
                                        <Break>
                                            <HoursFrom>15:00</HoursFrom>
                                            <HoursTill>15:30</HoursTill>
                                        </Break>
                        </WorkingTime>
                        <WorkingTime>
                                        <WorkingDaysFrom>5</WorkingDaysFrom>
                                        <WorkingDaysTill>5</WorkingDaysTill>
                                        <WorkingHoursFrom>10:00</WorkingHoursFrom>
                                        <WorkingHoursTill>18:00</WorkingHoursTill>
                                        <Break>
                                            <HoursFrom>15:00</HoursFrom>
                                            <HoursTill>15:30</HoursTill>
                                        </Break>
                                        <Break>
                                            <HoursFrom>18:00</HoursFrom>
                                            <HoursTill>18:30</HoursTill>
                                        </Break>
                        </WorkingTime>
                        """;

        String schedule = OutletScheduleParser.toHumanForm(raw);
        var expected = "пн. — вт. с 10:00 до 20:00, перерыв с 18:00 до 18:30;" +
                " ср. с 10:00 до 20:00, перерыв с 15:00 до 15:30;" +
                " чт. с 10:00 до 18:00, перерыв с 15:00 до 15:30;" +
                " пт. с 10:00 до 18:00, перерыв с 15:00 до 15:30, с 18:00 до 18:30";
        assertEquals(expected, schedule);
    }

    @Test
    public void testWrongOrderRanges() {
        String raw =
                """
                        <WorkingTime>
                                        <WorkingDaysFrom>2</WorkingDaysFrom>
                                        <WorkingDaysTill>2</WorkingDaysTill>
                                        <WorkingHoursFrom>10:00</WorkingHoursFrom>
                                        <WorkingHoursTill>20:00</WorkingHoursTill>
                                        <Break>
                                            <HoursFrom>10:00</HoursFrom>
                                            <HoursTill>12:30</HoursTill>
                                        </Break>
                                        <Break>
                                            <HoursFrom>18:00</HoursFrom>
                                            <HoursTill>18:30</HoursTill>
                                        </Break>
                        </WorkingTime>
                        <WorkingTime>
                                        <WorkingDaysFrom>1</WorkingDaysFrom>
                                        <WorkingDaysTill>1</WorkingDaysTill>
                                        <WorkingHoursFrom>10:00</WorkingHoursFrom>
                                        <WorkingHoursTill>20:00</WorkingHoursTill>
                                        <Break>
                                            <HoursFrom>18:00</HoursFrom>
                                            <HoursTill>18:30</HoursTill>
                                        </Break>
                                        <Break>
                                            <HoursFrom>10:00</HoursFrom>
                                            <HoursTill>12:30</HoursTill>
                                        </Break>
                        </WorkingTime>
                        """;

        String schedule = OutletScheduleParser.toHumanForm(raw);
        var expected = "пн. — вт. с 10:00 до 20:00, перерыв с 10:00 до 12:30, с 18:00 до 18:30";
        assertEquals(expected, schedule);
    }

    @Test
    public void testCrossedOrderRanges() {
        String raw =
                """
                        <WorkingTime>
                                        <WorkingDaysFrom>1</WorkingDaysFrom>
                                        <WorkingDaysTill>3</WorkingDaysTill>
                                        <WorkingHoursFrom>10:00</WorkingHoursFrom>
                                        <WorkingHoursTill>20:00</WorkingHoursTill>
                                        <Break>
                                            <HoursFrom>10:00</HoursFrom>
                                            <HoursTill>12:30</HoursTill>
                                        </Break>
                        </WorkingTime>
                        <WorkingTime>
                                        <WorkingDaysFrom>2</WorkingDaysFrom>
                                        <WorkingDaysTill>4</WorkingDaysTill>
                                        <WorkingHoursFrom>10:00</WorkingHoursFrom>
                                        <WorkingHoursTill>20:00</WorkingHoursTill>
                                        <Break>
                                            <HoursFrom>10:00</HoursFrom>
                                            <HoursTill>12:30</HoursTill>
                                        </Break>
                        </WorkingTime>
                        """;

        String schedule = OutletScheduleParser.toHumanForm(raw);
        var expected = "пн. — чт. с 10:00 до 20:00, перерыв с 10:00 до 12:30";
        assertEquals(expected, schedule);
    }

    @Test
    public void testEmptySchedule() {
        String raw = "";
        String schedule = OutletScheduleParser.toHumanForm(raw);
        assertEquals(Strings.EMPTY, schedule);
    }

    @Test
    public void testOutletSchedule() {
        var outlet = new Outlet();
        outlet.setWorkingTime(List.of(
                workingTime(1, 1, "08:00", "20:00"),
                workingTime(2, 2, "08:00", "20:00"),
                workingTime(3, 3, "08:00", "20:00"),
                workingTime(4, 4, "08:00", "20:00"),
                workingTime(5, 5, "08:00", "18:00"),
                workingTime(5, 5, "10:00", "16:00")
        ));

        String schedule = OutletScheduleParser.toHumanForm(outlet);
        assertEquals("пн. — чт. с 08:00 до 20:00; пт. с 08:00 до 18:00; пт. с 10:00 до 16:00", schedule);
    }

    private Outlet.WorkingTime workingTime(Integer daysFrom, Integer daysTo, String hoursFrom, String hoursTo) {
        var workingTime = new Outlet.WorkingTime();
        workingTime.setDaysFrom(daysFrom);
        workingTime.setDaysTo(daysTo);
        workingTime.setHoursFrom(hoursFrom);
        workingTime.setHoursTo(hoursTo);

        return workingTime;
    }

    @Test
    public void testOutletEmptySchedule() {
        String schedule = OutletScheduleParser.toHumanForm(new Outlet());
        assertEquals(Strings.EMPTY, schedule);

        var outlet = new Outlet();
        outlet.setWorkingTime(List.of());

        schedule = OutletScheduleParser.toHumanForm(outlet);
        assertEquals(Strings.EMPTY, schedule);
    }

    private void assertEqualDayTimeRange(OutletScheduleParser.WorkingTime expected,
                                         OutletScheduleParser.WorkingTime actual) {
        assertEquals(expected.getWorkingDaysFrom(), actual.getWorkingDaysFrom());
        assertEquals(expected.getWorkingDaysTill(), actual.getWorkingDaysTill());
        assertEquals(expected.getWorkingHoursFrom(), actual.getWorkingHoursFrom());
        assertEquals(expected.getWorkingHoursTill(), actual.getWorkingHoursTill());
        assertEqualBrakes(expected.getBreaksList(), actual.getBreaksList());
    }

    private void assertEqualBrakes(List<OutletScheduleParser.Break> expected, List<OutletScheduleParser.Break> actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertEquals(expected.size(), actual.size());
        for (var i = 0; i < expected.size(); i++) {
            var expectedBrake = expected.get(i);
            var actualBrake = actual.get(i);
            assertEquals(expectedBrake.getHoursFrom(), actualBrake.getHoursFrom());
            assertEquals(expectedBrake.getHoursTill(), actualBrake.getHoursTill());
        }
    }
}
