package ru.yandex.market.billing.overdraft.delay;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.calendar.CalendarService;
import ru.yandex.market.core.calendar.DailyCalendarService;
import ru.yandex.market.core.util.DateTimes;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link OverdraftDelayService}
 *
 * @author vbudnev
 */
@DbUnitDataSet(before = "db/OverdraftDelayServiceTest.before.csv")
class OverdraftDelayServiceTest extends FunctionalTest {

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private DailyCalendarService dailyCalendarService;

    private OverdraftDelayService overdraftDelayService;

    @BeforeEach
    void beforeEach() {
        overdraftDelayService = new OverdraftDelayService(
                dailyCalendarService,
                calendarService,
                LocalDate.of(2019, 6, 14)
        );
    }

    @DisplayName("Получение рабочего дня со сдвигом для localDate")
    @Test
    void test_getWorkingDayOffset_localDate() {
        assertEquals(
                LocalDate.of(2019, 6, 22),
                overdraftDelayService.getWorkingDayOffset(LocalDate.of(2019, 6, 14))
        );

        assertEquals(
                LocalDate.of(2019, 6, 25),
                overdraftDelayService.getWorkingDayOffset(LocalDate.of(2019, 6, 20))
        );

        assertEquals(
                LocalDate.of(2019, 7, 4),
                overdraftDelayService.getWorkingDayOffset(LocalDate.of(2019, 7, 1))
        );
    }

    @DisplayName("Получение рабочего дня-времени со сдвигом для instant")
    @Test
    void test_getWorkingDayOffset_instant() {
        assertEquals(
                DateTimes.toInstant(2019, 7, 4, 1, 1, 1),
                overdraftDelayService.getWorkingDayOffset(DateTimes.toInstant(2019, 7, 1, 1, 1, 1))
        );
    }

    @DisplayName("Ошибка если не удалось найти")
    @Test
    void test_getWorkingDayOffset_error() {

        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> overdraftDelayService.getWorkingDayOffset(LocalDate.of(2019, 8, 1))
        );
        assertThat(ex.getMessage(), is("Cant find working day for 2019-08-01 with offset 3"));
    }
}