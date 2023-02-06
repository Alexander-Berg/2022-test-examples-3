package ru.yandex.market.mbo_clab_billing.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.Assert;
import org.junit.Test;

import static ru.yandex.market.mbo_clab_billing.util.TimeZoneUtils.findPeriod;

public class TimeZoneUtilsTest {


    private Instant utcInst(String dateTime) {
        return LocalDateTime.parse(dateTime).toInstant(ZoneOffset.UTC);
    }

    private Instant moscowInst(String startMoscow) {
        Instant startInstant = utcInst(startMoscow);
        ZoneOffset offset = TimeZoneUtils.MOSCOW.getRules().getOffset(startInstant);
        return startInstant.minusSeconds(offset.getTotalSeconds());
    }

    private Instant[] exp(String startMoscow, String endMoscow) {
        return new Instant[]{moscowInst(startMoscow), moscowInst(endMoscow)};
    }

    private void assertResult(String inputUtc, String startMoscow, String endMoscow) {
        Assert.assertArrayEquals(exp(startMoscow, endMoscow), findPeriod(utcInst(inputUtc)));
    }

    @Test
    public void convertInstantToCorrectPeriodInMoscowZone() {
        // first half
        assertResult("2021-04-01T16:00:00", "2021-04-01T00:00:00", "2021-04-16T00:00:00");
        assertResult("2021-04-05T16:00:00", "2021-04-01T00:00:00", "2021-04-16T00:00:00");
        assertResult("2021-04-15T16:00:00", "2021-04-01T00:00:00", "2021-04-16T00:00:00");
        // second half
        assertResult("2021-04-16T16:00:00", "2021-04-16T00:00:00", "2021-05-01T00:00:00");
        assertResult("2021-04-23T16:00:00", "2021-04-16T00:00:00", "2021-05-01T00:00:00");
        assertResult("2021-04-30T16:00:00", "2021-04-16T00:00:00", "2021-05-01T00:00:00");
        // close to midnight check
        assertResult("2021-03-31T23:00:00", "2021-04-01T00:00:00", "2021-04-16T00:00:00");
        assertResult("2021-04-15T23:00:00", "2021-04-16T00:00:00", "2021-05-01T00:00:00");
    }

    @Test
    public void convertToLocalDateInDifferentZones() {
        Assert.assertEquals(LocalDate.parse("2021-04-04"),
                TimeZoneUtils.dateFromInstant(moscowInst("2021-04-05T00:00:00"), false));
        Assert.assertEquals(LocalDate.parse("2021-04-05"),
                TimeZoneUtils.dateFromInstant(moscowInst("2021-04-05T00:00:00"), true));
        Assert.assertEquals(LocalDate.parse("2021-04-04"),
                TimeZoneUtils.dateFromInstant(moscowInst("2021-04-05T02:00:00"), false));
        Assert.assertEquals(LocalDate.parse("2021-04-05"),
                TimeZoneUtils.dateFromInstant(moscowInst("2021-04-05T02:00:00"), true));

        Assert.assertEquals(LocalDate.parse("2021-04-05"),
                TimeZoneUtils.dateFromInstant(moscowInst("2021-04-05T16:00:00"), false));
        Assert.assertEquals(LocalDate.parse("2021-04-05"),
                TimeZoneUtils.dateFromInstant(moscowInst("2021-04-05T16:00:00"), true));
        Assert.assertEquals(LocalDate.parse("2021-04-05"),
                TimeZoneUtils.dateFromInstant(moscowInst("2021-04-05T23:00:00"), false));
        Assert.assertEquals(LocalDate.parse("2021-04-05"),
                TimeZoneUtils.dateFromInstant(moscowInst("2021-04-05T23:00:00"), true));

    }
}
