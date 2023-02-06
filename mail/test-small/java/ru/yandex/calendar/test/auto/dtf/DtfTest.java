package ru.yandex.calendar.test.auto.dtf;

import lombok.val;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import ru.yandex.calendar.util.dates.DateTimeFormatter;
import ru.yandex.misc.time.TimeUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class DtfTest extends AbstractDtfTestCase {
    @Test
    public void periodTs1() {
        val chrono = tzId2ChronoDtfMap.get(TimeUtils.EUROPE_MOSCOW_TIME_ZONE.getID());
        val startDt       = new DateTime(2009, 12, 30,  9,  8,  7, 654, chrono);
        val endPeriodTsStr  = "P3DT1H10M-";
        val expectedEndDt = new DateTime(2010, 01, 02, 10, 18,  0,   0, chrono);
        val actualEndTs  = DateTimeFormatter.toNullableExtTimestampUnsafe(startDt.toInstant(), endPeriodTsStr, chrono.getZone());
        assertThat(actualEndTs.getMillis()).isEqualTo(expectedEndDt.getMillis());
    }
}
