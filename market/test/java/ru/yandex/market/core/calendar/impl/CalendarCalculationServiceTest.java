package ru.yandex.market.core.calendar.impl;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.calendar.CalendarService;
import ru.yandex.market.core.calendar.DailyCalendarService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

@DbUnitDataSet(before = "CalendarCalculationServiceTest.before.csv")
public class CalendarCalculationServiceTest extends FunctionalTest {
    private static final Instant NOV_03_2020_10_00 = Instant.parse("2020-11-03T10:00:00.000Z"); //Вторник
    private static final Instant NOV_03_2020_18_00 = Instant.parse("2020-11-03T18:00:00.000Z"); //Вторник
    private static final Instant NOV_05_2020_11_00 = Instant.parse("2020-11-05T11:00:00.000Z"); //Четверг
    private static final Instant DEC_16_2020_10_00 = Instant.parse("2020-12-16T10:00:00.000Z"); //Среда
    private static final Instant DEC_20_2020_18_00 = Instant.parse("2020-12-20T18:00:00.000Z"); //Воскресенье
    private static final Instant DEC_21_2020_18_00 = Instant.parse("2020-12-21T18:00:00.000Z"); //Понедельник
    private static final Instant DEC_23_2020_14_00 = Instant.parse("2020-12-23T14:00:00.000Z"); //Среда
    private static final Instant DEC_27_2020_17_00 = Instant.parse("2020-12-27T17:00:00.000Z"); //Воскресенье

    private CalendarCalculationService calculator;

    private static final int COUNTRY_ID = 225;
    private static final int NULL_CALENDAR_COUNTRY_ID = 226;
    private static final int UNKNOWN_COUNTRY_ID = 227;

    @Autowired
    private DailyCalendarService dailyCalendarService;

    @Autowired
    private CalendarService calendarService;

    private static final ZoneId DEFAULT_TZ = ZoneId.of("Europe/Moscow");

    @BeforeEach
    public void init() {
        calculator = new CalendarCalculationService(calendarService, dailyCalendarService);
    }

    @Test
    public void testFromWeekdayToWeekend() {
        assertEquals(2, calculator.calculateWorkingTimeDuration(
                COUNTRY_ID,
                DEFAULT_TZ,
                DEC_23_2020_14_00,
                DEC_27_2020_17_00
        ).toDays());
    }

    @Test
    public void testFromWeekendToWeekday() {
        assertEquals(2, calculator.calculateWorkingTimeDuration(
                COUNTRY_ID,
                DEFAULT_TZ,
                DEC_20_2020_18_00,
                DEC_23_2020_14_00
        ).toDays());
    }

    @Test
    public void testWeekdays() {
        assertEquals(1, calculator.calculateWorkingTimeDuration(
                COUNTRY_ID,
                DEFAULT_TZ,
                DEC_21_2020_18_00,
                DEC_23_2020_14_00
        ).toDays());
    }

    @Test
    public void testWeekdaysLong() {
        assertEquals(5, calculator.calculateWorkingTimeDuration(
                COUNTRY_ID,
                DEFAULT_TZ,
                DEC_16_2020_10_00,
                DEC_23_2020_14_00
        ).toDays());
    }

    @Test
    public void testHolidays() {
        assertEquals(1, calculator.calculateWorkingTimeDuration(
                COUNTRY_ID,
                DEFAULT_TZ,
                NOV_03_2020_10_00,
                NOV_05_2020_11_00
        ).toDays());
    }

    @Test
    public void testSameDay() {
        assertEquals(0, calculator.calculateWorkingTimeDuration(
                COUNTRY_ID,
                DEFAULT_TZ,
                NOV_03_2020_10_00,
                NOV_03_2020_18_00
        ).toDays());
    }

    @Test
    public void testSameTime() {
        assertEquals(0, calculator.calculateWorkingTimeDuration(
                COUNTRY_ID,
                DEFAULT_TZ,
                NOV_03_2020_10_00,
                NOV_03_2020_10_00
        ).toDays());
    }

    @Test
    public void testWeekendToWeekend() {
        assertEquals(5, calculator.calculateWorkingTimeDuration(
                COUNTRY_ID,
                DEFAULT_TZ,
                DEC_20_2020_18_00,
                DEC_27_2020_17_00
        ).toDays());
    }

    @Test
    public void testWrongDateOrder() {
        assertThatThrownBy(() -> {
            calculator.calculateWorkingTimeDuration(
                    COUNTRY_ID,
                    DEFAULT_TZ,
                    DEC_21_2020_18_00,
                    NOV_03_2020_10_00
            );
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testUnknownCountry() {
        assertEquals(1, calculator.calculateWorkingTimeDuration(
                UNKNOWN_COUNTRY_ID,
                DEFAULT_TZ,
                DEC_21_2020_18_00,
                DEC_23_2020_14_00
        ).toDays());
    }

    @Test
    public void testNullCalendar() {
        assertEquals(6, calculator.calculateWorkingTimeDuration(
                NULL_CALENDAR_COUNTRY_ID,
                DEFAULT_TZ,
                DEC_20_2020_18_00,
                DEC_27_2020_17_00
        ).toDays());
    }
}
