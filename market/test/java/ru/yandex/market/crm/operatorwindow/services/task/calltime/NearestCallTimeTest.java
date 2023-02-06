package ru.yandex.market.crm.operatorwindow.services.task.calltime;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.operatorwindow.util.DateParsers;

public class NearestCallTimeTest {

    private static final ZoneId TZ_MSK = ZoneId.of("Europe/Moscow"); // UTC+3

    private static final LocalDate DAY_2018_11_22 = DateParsers.parseIsoDate("2018-11-22");
    private static final OffsetDateTime DT1 = makeDateTime(DAY_2018_11_22, "10:30");
    private static final OffsetDateTime DT2 = makeDateTime(DAY_2018_11_22, "12:00");

    private static OffsetDateTime makeDateTime(LocalDate date, String time) {
        return date
                .atTime(DateParsers.parseIsoTime(time))
                .atZone(TZ_MSK).toOffsetDateTime();
    }

    @Test
    public void nowIsBeforeFrom() {
        OffsetDateTime now = makeDateTime(DAY_2018_11_22, "09:01");
        NearestCallTime callTime = new NearestCallTime(DT1, DT2, now);
        Assertions.assertFalse(callTime.isCallAllowedNow());
        Assertions.assertTrue(callTime.nextAllowedCallTime().isEqual(DT1));
    }

    @Test
    public void nowIsFrom() {
        NearestCallTime callTime = new NearestCallTime(DT1, DT2, DT1);
        Assertions.assertTrue(callTime.isCallAllowedNow());
        Assertions.assertTrue(callTime.nextAllowedCallTime().isEqual(DT1));
    }

    @Test
    public void nowIsInInterval() {
        OffsetDateTime now = makeDateTime(DAY_2018_11_22, "10:31");
        NearestCallTime callTime = new NearestCallTime(DT1, DT2, now);
        Assertions.assertTrue(callTime.isCallAllowedNow());
        Assertions.assertTrue(callTime.nextAllowedCallTime().isEqual(now));
    }

    @Test
    public void nowIsTo() {
        NearestCallTime callTime = new NearestCallTime(DT1, DT2, DT2);
        Assertions.assertTrue(callTime.isCallAllowedNow());
        Assertions.assertTrue(callTime.nextAllowedCallTime().isEqual(DT2));
    }

    // CustomerCallTimeCalculator cannot produce this case. Test it for uniformity.
    @Test
    public void nowIsAfterTo() {
        OffsetDateTime now = makeDateTime(DAY_2018_11_22, "12:01");
        NearestCallTime callTime = new NearestCallTime(DT1, DT2, now);
        Assertions.assertFalse(callTime.isCallAllowedNow());
        Assertions.assertTrue(callTime.nextAllowedCallTime().isEqual(now));
    }

}
