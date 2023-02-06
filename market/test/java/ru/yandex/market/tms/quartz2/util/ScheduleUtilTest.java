package ru.yandex.market.tms.quartz2.util;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScheduleUtilTest {

    @Test
    public void shouldFire() {
        Calendar firedTime = Calendar.getInstance();
        firedTime.set(2016, 4, 18, 13, 47); // yyyy mm dd hh mi
        Date countDate = firedTime.getTime();

        assertTrue(ScheduleUtil.shouldFire("0 0/5 * * * ?", countDate, 5, TimeUnit.MINUTES));
        assertFalse(ScheduleUtil.shouldFire("0 0/5 * * * ?", countDate, 2, TimeUnit.MINUTES));
        assertFalse(ScheduleUtil.shouldFire("0 0/30 * * * ?", countDate, 5, TimeUnit.MINUTES));

        assertTrue(ScheduleUtil.shouldFire("0 0 1/1 * * ?", countDate, 1, TimeUnit.HOURS));
        assertFalse(ScheduleUtil.shouldFire("0 0 0 1 1 ? 2000", countDate, 5, TimeUnit.DAYS));
        assertFalse(ScheduleUtil.shouldFire("0 0 0 1 1 ? 2030", countDate, 2, TimeUnit.NANOSECONDS));
    }

}
