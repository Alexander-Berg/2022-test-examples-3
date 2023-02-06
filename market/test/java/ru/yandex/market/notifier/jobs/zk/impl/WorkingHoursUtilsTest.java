package ru.yandex.market.notifier.jobs.zk.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static ru.yandex.market.notifier.jobs.zk.impl.WorkingHoursUtils.courierCallTime;

public class WorkingHoursUtilsTest {

    private static final String TODAY_SOON = "Доставка состоится сегодня 14.12 в течение 4 часов. Курьер позвонит вам за час до доставки.";
    private static final String TODAY_AFTER_TEN = "Доставка состоится сегодня 15.12 с 10:00 до 14:00. Курьер позвонит вам за час до доставки.";
    private static final String TOMORROW_AFTER_TEN = "Доставка состоится завтра 15.12 с 10:00 до 14:00. Курьер позвонит вам за час до доставки.";
    private static final String OTHER_DAY_AFTER_TEN = "Доставка состоится 16.12 с 10:00 до 14:00. Курьер позвонит вам за час до доставки.";

    @Test
    public void withinWorkingHours_deliveryToday_soon() {
        assertEquals(TODAY_SOON, getCourierCallTime("2016-12-14T15:00:00", "2016-12-14"));
    }

    @Test
    public void beforeWorkingHours_deliveryToday_todayAfterTen() {
        assertEquals(TODAY_AFTER_TEN, getCourierCallTime("2016-12-15T00:05:00", "2016-12-15"));
    }

    @Test
    public void afterWorkingHours_deliveryToday_soon() {
        assertEquals(TODAY_SOON, getCourierCallTime("2016-12-14T18:05:00", "2016-12-14"));
    }

    @Test
    public void afterWorkingHours_deliveryTomorrow_tomorrowAfterTen() {
        assertEquals(TOMORROW_AFTER_TEN, getCourierCallTime("2016-12-14T18:00:00", "2016-12-15"));
    }

    @Test
    public void withingWorkingHours_deliveryTomorrow_tomorrowAfterTen() {
        assertEquals(TOMORROW_AFTER_TEN, getCourierCallTime("2016-12-14T15:00:00", "2016-12-15"));
    }

    @Test
    public void beforeWorkingHours_deliveryTomorrow_tomorrowAfterTen() {
        assertEquals(TOMORROW_AFTER_TEN, getCourierCallTime("2016-12-14T08:00:00", "2016-12-15"));
    }

    @Test
    public void afterWorkingHours_deliveryOtherDay_otherDayAfterTen() {
        assertEquals(OTHER_DAY_AFTER_TEN, getCourierCallTime("2016-12-14T18:00:00", "2016-12-16"));
    }



    private static String getCourierCallTime(String eventDate, String deliveryDate) {
        return courierCallTime(LocalDateTime.parse(eventDate), LocalDate.parse(deliveryDate));
    }
}
