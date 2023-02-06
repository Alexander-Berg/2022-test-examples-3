package ru.yandex.market.sc.core.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mors741
 */
class ScDateUtilsTest {

    @Test
    void getLocalDateInterval() {
        Instant start = LocalDate.now().atStartOfDay().toInstant(DateTimeUtil.DEFAULT_ZONE_ID);
        Instant end = LocalDate.now().plusDays(1).atStartOfDay().toInstant(DateTimeUtil.DEFAULT_ZONE_ID);

        LocalDateInterval localDateInterval = ScDateUtils.getLocalDateInterval(start, end);
        assertEquals(LocalDate.now(), localDateInterval.getStart());
        assertEquals(LocalDate.now(), localDateInterval.getEnd());
    }

    @Test
    void isDateWithinInterval() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime nextDayStart = today.plusDays(1).atStartOfDay();

        assertTrue(ScDateUtils.isDateWithinInterval(today, start, nextDayStart));
        assertFalse(ScDateUtils.isDateWithinInterval(today.plusDays(1), start, nextDayStart));
        assertFalse(ScDateUtils.isDateWithinInterval(today.minusDays(1), start, nextDayStart));
        assertFalse(ScDateUtils.isDateWithinInterval(today, start, start.plusHours(3)));
    }
}
