package ru.yandex.market.abo.core.shop.schedule;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 * @date 26/02/2020.
 */
class WorkingPeriodHelperTest {

    @Test
    @DisplayName("Изменение таймзоны назад без перескока через полночь")
    void changeTimeZoneBackOneDay() {
        var period = new ShopWorkingPeriod(1, LocalTime.of(10, 0), LocalTime.of(19, 0), DayOfWeek.values());
        int offset = 3;
        List<ShopWorkingPeriod> changed = WorkingPeriodHelper.changeTimeZone(period, offset);
        assertEquals(
                List.of(new ShopWorkingPeriod(1, LocalTime.of(7, 0), LocalTime.of(16, 0), DayOfWeek.values())),
                changed
        );
    }

    @Test
    @DisplayName("Изменение таймзоны вперёд без перескока через полночь")
    void changeTimeZoneForwardOneDay() {
        var period = new ShopWorkingPeriod(1, LocalTime.of(10, 0), LocalTime.of(19, 0), DayOfWeek.values());
        int offset = -3;
        List<ShopWorkingPeriod> changed = WorkingPeriodHelper.changeTimeZone(period, offset);
        assertEquals(
                List.of(new ShopWorkingPeriod(1, LocalTime.of(13, 0), LocalTime.of(22, 0), DayOfWeek.values())),
                changed
        );
    }

    @Test
    @DisplayName("Изменение таймзоны назад с перескоком через полночь и раздвоением")
    void changeTimeZoneBackThroughMidnight() {
        var period = new ShopWorkingPeriod(1, LocalTime.of(2, 0), LocalTime.of(10, 0), new DayOfWeek[]{MONDAY});
        int offset = 4;
        List<ShopWorkingPeriod> changed = WorkingPeriodHelper.changeTimeZone(period, offset);
        assertEquals(
                List.of(
                        new ShopWorkingPeriod(1, LocalTime.of(22, 0), LocalTime.of(23, 59), new DayOfWeek[]{SUNDAY}),
                        new ShopWorkingPeriod(1, LocalTime.of(0, 0), LocalTime.of(6, 0), new DayOfWeek[]{MONDAY})
                ),
                changed
        );
    }

    @Test
    @DisplayName("Изменение таймзоны вперёд с перескоком через полночь и раздвоением")
    void changeTimeZoneForwardThroughMidnight() {
        var period = new ShopWorkingPeriod(1, LocalTime.of(19, 0), LocalTime.of(23, 0), new DayOfWeek[]{SATURDAY, SUNDAY});
        int offset = -2;
        List<ShopWorkingPeriod> changed = WorkingPeriodHelper.changeTimeZone(period, offset);
        assertEquals(
                List.of(
                        new ShopWorkingPeriod(1, LocalTime.of(21, 0), LocalTime.of(23, 59), new DayOfWeek[]{SATURDAY, SUNDAY}),
                        new ShopWorkingPeriod(1, LocalTime.of(0, 0), LocalTime.of(1, 0), new DayOfWeek[]{SUNDAY, MONDAY})
                ),
                changed
        );
    }

    @Test
    @DisplayName("Изменение таймзоны назад с перескоком через полночь и полным попданием в предыдущий день")
    void changeTimeZoneBackThroughMidnightOnlyPreviousDay() {
        var period = new ShopWorkingPeriod(1, LocalTime.of(1, 0), LocalTime.of(5, 0), new DayOfWeek[]{THURSDAY});
        int offset = 7;
        List<ShopWorkingPeriod> changed = WorkingPeriodHelper.changeTimeZone(period, offset);
        assertEquals(
                List.of(new ShopWorkingPeriod(1, LocalTime.of(18, 0), LocalTime.of(22, 0), new DayOfWeek[]{WEDNESDAY})),
                changed
        );
    }

    @Test
    @DisplayName("Изменение таймзоны вперёд с перескоком через полночь и полным попданием в следующий день")
    void changeTimeZoneForwardThroughMidnightOnlyNextDay() {
        var period = new ShopWorkingPeriod(1, LocalTime.of(22, 0), LocalTime.of(23, 59), new DayOfWeek[]{FRIDAY});
        int offset = -2;
        List<ShopWorkingPeriod> changed = WorkingPeriodHelper.changeTimeZone(period, offset);
        assertEquals(
                List.of(new ShopWorkingPeriod(1, LocalTime.of(0, 0), LocalTime.of(1, 59), new DayOfWeek[]{SATURDAY})),
                changed
        );
    }

    @Test
    @DisplayName("Изменение таймзоны назад с попаданием начала точно на 23:59")
    void changeTimeZoneBackRightBeforePreviousMidnight() {
        var period = new ShopWorkingPeriod(1, LocalTime.of(2, 59), LocalTime.of(11, 0), new DayOfWeek[]{TUESDAY});
        int offset = 3;
        List<ShopWorkingPeriod> changed = WorkingPeriodHelper.changeTimeZone(period, offset);
        assertEquals(
                List.of(new ShopWorkingPeriod(1, LocalTime.of(0, 0), LocalTime.of(8, 0), new DayOfWeek[]{TUESDAY})),
                changed
        );
    }

    @Test
    @DisplayName("Изменение таймзоны назад с попаданием конца точно на полночь")
    void changeTimeZoneBackRightInPreviousMidnight() {
        var period = new ShopWorkingPeriod(1, LocalTime.of(0, 0), LocalTime.of(5, 0), new DayOfWeek[]{WEDNESDAY});
        int offset = 5;
        List<ShopWorkingPeriod> changed = WorkingPeriodHelper.changeTimeZone(period, offset);
        assertEquals(
                List.of(new ShopWorkingPeriod(1, LocalTime.of(19, 0), LocalTime.of(23, 59), new DayOfWeek[]{TUESDAY})),
                changed
        );
    }

    @Test
    @DisplayName("Изменение таймзоны вперёд с попаданием начала точно на 23:59")
    void changeTimeZoneForwardRightBeforeMidnight() {
        var period = new ShopWorkingPeriod(1, LocalTime.of(19, 59), LocalTime.of(22, 0), new DayOfWeek[]{TUESDAY});
        int offset = -4;
        List<ShopWorkingPeriod> changed = WorkingPeriodHelper.changeTimeZone(period, offset);
        assertEquals(
                List.of(new ShopWorkingPeriod(1, LocalTime.of(0, 0), LocalTime.of(2, 0), new DayOfWeek[]{WEDNESDAY})),
                changed
        );
    }

    @Test
    @DisplayName("Изменение таймзоны вперёд с попаданием конца точно на полночь")
    void changeTimeZoneForwardRightInMidnight() {
        var period = new ShopWorkingPeriod(1, LocalTime.of(18, 0), LocalTime.of(22, 0), new DayOfWeek[]{TUESDAY});
        int offset = -2;
        List<ShopWorkingPeriod> changed = WorkingPeriodHelper.changeTimeZone(period, offset);
        assertEquals(
                List.of(new ShopWorkingPeriod(1, LocalTime.of(20, 0), LocalTime.of(23, 59), new DayOfWeek[]{TUESDAY})),
                changed
        );
    }
}