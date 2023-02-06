package ru.yandex.direct.core.entity.vcard;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.vcard.VcardWorktime.DailySchedule;

import static org.junit.Assert.assertEquals;

@ParametersAreNonnullByDefault
public class VcardWorktimeDailyScheduleTest {
    private static final DailySchedule TEST_SCHEDULE = DailySchedule.fromEncodedString("0#3#10#30#18#15");

    @Test
    public void fromEncodedStringDaySinceTest() {
        assertEquals("получен правильный день начала", 0, TEST_SCHEDULE.getDaySince());
    }

    @Test
    public void fromEncodedStringDayTillTest() {
        assertEquals("получен правильный день окончания", 3, TEST_SCHEDULE.getDayTill());
    }

    @Test
    public void fromEncodedStringHourSinceTest() {
        assertEquals("получен правильный час начала", 10, TEST_SCHEDULE.getHourSince());
    }

    @Test
    public void fromEncodedStringMuniteSinceTest() {
        assertEquals("получена правильная минута начала", 30, TEST_SCHEDULE.getMinuteSince());
    }

    @Test
    public void fromEncodedStringHourTillTest() {
        assertEquals("получен правильный час окончания", 18, TEST_SCHEDULE.getHourTill());
    }

    @Test
    public void fromEncodedStringMinuteTillTest() {
        assertEquals("получена правильная минута окончания", 15, TEST_SCHEDULE.getMinuteTill());
    }

    @Test
    public void leadingZeroesTest() {
        DailySchedule leadingZeroes = DailySchedule.fromEncodedString("00#01#02#03#04#05");
        DailySchedule noLeadingZeroes = DailySchedule.fromEncodedString("0#1#2#3#4#5");
        assertEquals("расписания с ведущими нулями и без ведущих нулей эквивалентны", leadingZeroes, noLeadingZeroes);
    }
}
