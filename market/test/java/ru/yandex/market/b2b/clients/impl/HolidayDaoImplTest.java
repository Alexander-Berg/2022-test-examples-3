package ru.yandex.market.b2b.clients.impl;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HolidayDaoImplTest extends AbstractFunctionalTest {
    private final HolidayDaoImpl dao;

    private final JdbcTemplate template;

    @Autowired
    public HolidayDaoImplTest(HolidayDaoImpl dao, JdbcTemplate template) {
        this.dao = dao;
        this.template = template;
    }

    @BeforeEach
    public void clearHolidaysTable() {
        template.update("DELETE FROM holiday WHERE \"id\" != 1");
        template.update("UPDATE holiday SET \"nonWorkDate\" = '1970-01-01' WHERE \"id\" = 1");
    }

    public static Set<LocalDate> may2022Holidays() {
        Set<LocalDate> may2022 = new HashSet<>();
        may2022.add(LocalDate.of(2022, 5, 1));
        may2022.add(LocalDate.of(2022, 5, 2));
        may2022.add(LocalDate.of(2022, 5, 3));
        may2022.add(LocalDate.of(2022, 5, 7));
        may2022.add(LocalDate.of(2022, 5, 8));
        may2022.add(LocalDate.of(2022, 5, 9));
        may2022.add(LocalDate.of(2022, 5, 10));
        may2022.add(LocalDate.of(2022, 5, 14));
        may2022.add(LocalDate.of(2022, 5, 15));
        may2022.add(LocalDate.of(2022, 5, 21));
        may2022.add(LocalDate.of(2022, 5, 22));
        may2022.add(LocalDate.of(2022, 5, 28));
        may2022.add(LocalDate.of(2022, 5, 29));
        return may2022;
    }

    @Test
    public void isUpdateTodayCheck() {
        assertFalse(dao.isTodayUpdated(LocalDate.of(2022, 5, 17)));
        assertTrue(dao.isTodayUpdated(LocalDate.of(2022, 5, 17)));
        assertTrue(dao.isTodayUpdated(LocalDate.of(2022, 5, 16)));
        assertFalse(dao.isTodayUpdated(LocalDate.of(2022, 5, 18)));
    }

    @Test
    public void updateAndGet() {
        LocalDate from = LocalDate.of(2022, 5, 1);
        LocalDate to = LocalDate.of(2022, 5, 31);

        Set<LocalDate> dates = dao.getHolidays(from, to, 225);
        assertTrue(dates.isEmpty());

        Set<LocalDate> may2022 = may2022Holidays();
        dao.updateHolidays(from, to, 225, may2022);

        dates = dao.getHolidays(from, to, 225);
        assertEquals(13, dates.size());

        may2022.remove(LocalDate.of(2022, 5, 29));
        may2022.add(LocalDate.of(2022, 5, 27));
        may2022.add(LocalDate.of(2022, 5, 26));

        dao.updateHolidays(from, to, 225, may2022);

        dates = dao.getHolidays(from, to, 225);
        assertEquals(14, dates.size());
        assertTrue(dates.contains(LocalDate.of(2022, 5, 27)));
        assertFalse(dates.contains(LocalDate.of(2022, 5, 29)));

        dates = dao.getHolidays(from, LocalDate.of(2022, 5, 8), 225);
        assertEquals(5, dates.size());

        dao.updateHolidays(from, to, 225, new HashSet<>());
        dates = dao.getHolidays(from, LocalDate.of(2022, 5, 8), 225);
        assertTrue(dates.isEmpty());
    }

    @Test
    public void updateTodayCheckOnHoliday() {
        LocalDate from = LocalDate.of(2022, 5, 1);
        LocalDate to = LocalDate.of(2022, 5, 31);

        Set<LocalDate> may2022 = new HashSet<>(may2022Holidays());
        dao.updateHolidays(from, to, 225, may2022);

        for (int i = 1; i <= 6; i++) {
            may2022.remove(LocalDate.of(2022, 5, i));
        }

        for (int i = 7; i <= 9; i++) {
            assertFalse(dao.isTodayUpdated(LocalDate.of(2022, 5, i)));
            dao.updateHolidays(LocalDate.of(2022, 5, i), to, 225, may2022);
            may2022.remove(LocalDate.of(2022, 5, i));
        }

        assertTrue(dao.isTodayUpdated(LocalDate.of(2022, 5, 9)));

        Set<LocalDate> dates = dao.getHolidays(LocalDate.of(2022, 5, 9), to, 225);
        assertTrue(dates.contains(LocalDate.of(2022, 5, 9)));
    }
}
