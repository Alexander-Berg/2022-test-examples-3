package ru.yandex.market.partner.phone.calendar;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.calendar.WeeklySchedule;
import ru.yandex.market.core.ds.phone.PhoneVisibilityCalendar;
import ru.yandex.market.core.ds.phone.PhoneVisibilityCalendarService;
import ru.yandex.market.core.ds.phone.TotalDurationException;
import ru.yandex.market.partner.test.context.FunctionalTest;

@DbUnitDataSet(before = "PhoneVisibilityCalendarServiceTest.before.csv")
class PhoneVisibilityCalendarServiceTest extends FunctionalTest {

    private static final long UID = 123L;
    private static final int MOSCOW_TIMEZONE = 1;
    private static final int SHOP_1_ID = 101;

    @Autowired
    private PhoneVisibilityCalendarService phoneVisibilityCalendarService;

    @Test
    void shouldReturnDefaultCalendar() {
        PhoneVisibilityCalendar calendar = phoneVisibilityCalendarService.getShopCalendar(SHOP_1_ID);
        Assertions.assertNotNull(calendar);
        Assertions.assertTrue(calendar.isDefault());
    }

    @Test
    void shouldCreateNewCalendar() {
        // given
        WeeklySchedule weeklySchedule = new WeeklySchedule(DayOfWeek.MONDAY, 9, 0, DayOfWeek.SUNDAY, 18, 0, null);
        PhoneVisibilityCalendar calendar = new PhoneVisibilityCalendar(SHOP_1_ID, Arrays.asList(weeklySchedule), MOSCOW_TIMEZONE);

        // when
        int calendarId = phoneVisibilityCalendarService.persist(UID, calendar);
        PhoneVisibilityCalendar persistedCalendar = phoneVisibilityCalendarService.getShopCalendar(SHOP_1_ID);

        // then
        Assertions.assertNotNull(persistedCalendar, "Calendar must not be null");
        Assertions.assertFalse(persistedCalendar.isDefault(), "Calendar must not be default");
        Assertions.assertEquals(persistedCalendar.getId(), calendarId, "Expected calendar ID is different");
        Assertions.assertEquals(calendar.getSchedules(), Arrays.asList(weeklySchedule));
    }

    @Test
    void shouldUpdateOldCalendar() {
        // given
        WeeklySchedule weeklySchedule = new WeeklySchedule(DayOfWeek.MONDAY, 9, 0, DayOfWeek.SUNDAY, 18, 0, null);
        PhoneVisibilityCalendar calendar = new PhoneVisibilityCalendar(SHOP_1_ID, Arrays.asList(weeklySchedule), MOSCOW_TIMEZONE);

        WeeklySchedule newWeeklySchedule = new WeeklySchedule(DayOfWeek.MONDAY, 10, 0, DayOfWeek.FRIDAY, 20, 0, null);
        PhoneVisibilityCalendar newCalendar = new PhoneVisibilityCalendar(SHOP_1_ID, Arrays.asList(newWeeklySchedule), MOSCOW_TIMEZONE);

        int calendarId = phoneVisibilityCalendarService.persist(UID, calendar);
        PhoneVisibilityCalendar persistedCalendar = phoneVisibilityCalendarService.getShopCalendar(SHOP_1_ID);

        Assertions.assertNotNull(persistedCalendar, "Calendar must not be null");
        Assertions.assertFalse(persistedCalendar.isDefault(), "Calendar must not be default");
        Assertions.assertEquals(persistedCalendar.getId(), calendarId, "Expected calendar ID is different");
        Assertions.assertEquals(calendar.getSchedules(), Arrays.asList(weeklySchedule));

        // when
        int newCalendarId = phoneVisibilityCalendarService.persist(UID, newCalendar);
        newCalendar = phoneVisibilityCalendarService.getShopCalendar(SHOP_1_ID);

        // then
        Assertions.assertEquals(calendarId, newCalendarId);
        Assertions.assertEquals(1, newCalendar.getSchedules().size(), "Schedules must not be empty");
        Assertions.assertNotSame(newWeeklySchedule, newCalendar.getSchedules().get(0));
        Assertions.assertEquals(newWeeklySchedule, newCalendar.getSchedules().get(0), "Schedules must be equal");
    }

    @Test
    void shouldThrowConstraintViolationExceptionException() {
        // given
        PhoneVisibilityCalendar calendar = new PhoneVisibilityCalendar(SHOP_1_ID, Collections.emptyList(), MOSCOW_TIMEZONE);

        // when
        try {
            phoneVisibilityCalendarService.persist(UID, calendar);

            // then
            Assertions.fail("Violation constraint did not work");
        } catch (TotalDurationException e) {
            Assertions.assertEquals("Few duration: 0", e.getMessage());
        }
    }
}
