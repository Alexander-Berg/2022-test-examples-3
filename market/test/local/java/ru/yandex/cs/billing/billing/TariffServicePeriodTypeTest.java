package ru.yandex.cs.billing.billing;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.yandex.cs.billing.billing.TariffServicePeriodType.PERIOD_TYPE_DAY;
import static ru.yandex.cs.billing.billing.TariffServicePeriodType.PERIOD_TYPE_MONTH;

public class TariffServicePeriodTypeTest {

    @Test
    public void testGetNextPeriodStartAfter()  {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime localDateTime = LocalDateTime.parse("2018-04-26 17:34:56.789", dateTimeFormatter);

        LocalDateTime nextPeriodStartAfterForDay = PERIOD_TYPE_DAY.getNextPeriodStartAfter(localDateTime);
        LocalDateTime nextPeriodStartAfterForMonth = PERIOD_TYPE_MONTH.getNextPeriodStartAfter(localDateTime);

        Assert.assertEquals("2018-04-27 00:00:00.000", nextPeriodStartAfterForDay.format(dateTimeFormatter));
        Assert.assertEquals("2018-05-01 00:00:00.000", nextPeriodStartAfterForMonth.format(dateTimeFormatter));
    }

}
