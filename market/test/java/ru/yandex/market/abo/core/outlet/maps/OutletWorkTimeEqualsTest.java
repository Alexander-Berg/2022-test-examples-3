package ru.yandex.market.abo.core.outlet.maps;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.outlet.maps.model.MapsOutlet;
import ru.yandex.market.abo.core.outlet.maps.model.MapsScheduleDay;
import ru.yandex.market.abo.core.outlet.maps.model.MapsScheduleLine;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.outlet.maps.model.OneDaySchedule.MINUTES_DELTA;
import static ru.yandex.market.core.schedule.ScheduleLine.DayOfWeek.MONDAY;
import static ru.yandex.market.core.schedule.ScheduleLine.DayOfWeek.SUNDAY;

/**
 * @author artemmz
 * @date 26.04.18.
 */
class OutletWorkTimeEqualsTest {
    private static final int TEN_O_CLOCK = (int) TimeUnit.HOURS.toMinutes(10);
    private static final int ELEVEN_O_CLOCK = (int) TimeUnit.HOURS.toMinutes(11);
    private static final int EIGHT_O_CLOCK = (int) TimeUnit.HOURS.toMinutes(20);
    private static final int SIX_O_CLOCK = (int) TimeUnit.HOURS.toMinutes(18);
    private static final int ONE_O_CLOCK = (int) TimeUnit.HOURS.toMinutes(13);
    private static final int TWO_O_CLOCK = (int) TimeUnit.HOURS.toMinutes(14);

    private static final int SEVEN_HOUR_DAY = (int) TimeUnit.HOURS.toMinutes(7);
    private static final int TEN_HOUR_DAY = (int) TimeUnit.HOURS.toMinutes(10);

    private OutletParamsComparator paramsComparator = new OutletParamsComparator();

    @Mock
    private OutletInfo mbiOutlet;
    @Mock
    private MapsOutlet mapsOutlet;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * MBI (пн-сб с 10 до 20, вс с 11 до 18):
     * <schedule id="24916790">
     * <line startDay="1" startMinute="600" minutes="600" days="5"/>
     * <line startDay="7" startMinute="660" minutes="420" days="0"/>
     * </schedule>
     * Справочник (то же расписание, с перерывом на обед в вс, который игнорируем):
     * <pre>
     * 	[
     *     {
     *         "day": "saturday",
     *         "time_minutes_begin": 600,
     *         "time_minutes_end": 1200
     *     },
     *     {
     *         "day": "sunday",
     *         "time_minutes_begin": 660,
     *         "time_minutes_end": 780
     *     },
     *     {
     *         "day": "sunday",
     *         "time_minutes_begin": 840,
     *         "time_minutes_end": 1080
     *     },
     *     {
     *         "day": "weekdays",
     *         "time_minutes_begin": 600,
     *         "time_minutes_end": 1200
     *     }
     * ]
     * </pre>
     */
    @Test
    void workingTimeEquals() {
        when(mbiOutlet.getSchedule()).thenReturn(new Schedule(0, List.of(
                new ScheduleLine(MONDAY, 5, TEN_O_CLOCK, TEN_HOUR_DAY),
                new ScheduleLine(SUNDAY, 0, ELEVEN_O_CLOCK, SEVEN_HOUR_DAY)
        )));
        when(mapsOutlet.getWorkIntervals()).thenReturn(List.of(
                new MapsScheduleLine(MapsScheduleDay.SATURDAY, TEN_O_CLOCK, EIGHT_O_CLOCK),
                new MapsScheduleLine(MapsScheduleDay.SUNDAY, ELEVEN_O_CLOCK, ONE_O_CLOCK),
                new MapsScheduleLine(MapsScheduleDay.SUNDAY, TWO_O_CLOCK, SIX_O_CLOCK),
                new MapsScheduleLine(MapsScheduleDay.WEEKDAYS, TEN_O_CLOCK, EIGHT_O_CLOCK)
        ));
        assertTrue(paramsComparator.workingTimesEqual(mbiOutlet, mapsOutlet));
    }

    @Test
    void workingTimeNotEquals() {
        when(mbiOutlet.getSchedule()).thenReturn(new Schedule(0, List.of(
                new ScheduleLine(MONDAY, 5, TEN_O_CLOCK, TEN_HOUR_DAY),
                new ScheduleLine(SUNDAY, 0, ELEVEN_O_CLOCK, SEVEN_HOUR_DAY)
        )));
        when(mapsOutlet.getWorkIntervals()).thenReturn(List.of(
                new MapsScheduleLine(MapsScheduleDay.SATURDAY, TEN_O_CLOCK, EIGHT_O_CLOCK + 2 * MINUTES_DELTA),
                new MapsScheduleLine(MapsScheduleDay.SUNDAY, ELEVEN_O_CLOCK, ONE_O_CLOCK),
                new MapsScheduleLine(MapsScheduleDay.SUNDAY, TWO_O_CLOCK, SIX_O_CLOCK),
                new MapsScheduleLine(MapsScheduleDay.WEEKDAYS, TEN_O_CLOCK, EIGHT_O_CLOCK)
        ));
        assertFalse(paramsComparator.workingTimesEqual(mbiOutlet, mapsOutlet));
    }

    @Test
    void diffLessThenDelta() {
        when(mbiOutlet.getSchedule()).thenReturn(new Schedule(0, List.of(
                new ScheduleLine(MONDAY, 5, TEN_O_CLOCK, TEN_HOUR_DAY),
                new ScheduleLine(SUNDAY, 0, ELEVEN_O_CLOCK, SEVEN_HOUR_DAY)
        )));
        when(mapsOutlet.getWorkIntervals()).thenReturn(List.of(
                new MapsScheduleLine(MapsScheduleDay.SATURDAY, TEN_O_CLOCK, EIGHT_O_CLOCK + MINUTES_DELTA / 2),
                new MapsScheduleLine(MapsScheduleDay.SUNDAY, ELEVEN_O_CLOCK, ONE_O_CLOCK),
                new MapsScheduleLine(MapsScheduleDay.SUNDAY, TWO_O_CLOCK, SIX_O_CLOCK),
                new MapsScheduleLine(MapsScheduleDay.WEEKDAYS, TEN_O_CLOCK, EIGHT_O_CLOCK)
        ));
        assertTrue(paramsComparator.workingTimesEqual(mbiOutlet, mapsOutlet));
    }
}
