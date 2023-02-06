package ru.yandex.direct.api.v5.entity.changes.converter;

import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;

public class ChangesDateTimeConvertUtilTest {

    private String utcDateTimeStr = "2001-02-03T04:05:06Z";
    private LocalDateTime moscowDateTime = LocalDateTime.of(2001,2,3,7,5,6);

    @Test
    public void test() {
        LocalDateTime testMsk = ChangesDateTimeConvertUtil.convertUtcTimestampToMoscowDateTime(utcDateTimeStr);
        Assert.assertEquals("Не работает преобразование в MSK", testMsk, moscowDateTime);

        String testUtc = ChangesDateTimeConvertUtil.convertMoscowDateTimeToUtcTimestamp(moscowDateTime);
        Assert.assertEquals("Не работает преобразование в UTC", testUtc, utcDateTimeStr);

        String testNowUtc = ChangesDateTimeConvertUtil.createNowUtcTimestampStr();
        LocalDateTime testNowMsk = ChangesDateTimeConvertUtil.convertUtcTimestampToMoscowDateTime(testNowUtc);
        String testNowUtc2 = ChangesDateTimeConvertUtil.convertMoscowDateTimeToUtcTimestamp(testNowMsk);
        Assert.assertEquals(testNowUtc, testNowUtc2);
    }
}
