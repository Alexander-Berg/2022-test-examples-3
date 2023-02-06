package ru.yandex.market.wms.api.service.referenceitems.push.utils;

import java.time.Instant;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.wms.api.service.referenceitems.utils.UpdatedDateTimeUtil;

public class UpdatedDateTimeUtilTest {
    @Test
    void checkSimpleUnitId() {
        DateTime result = UpdatedDateTimeUtil.calc(
                Instant.parse("2020-09-16T13:14:02.233Z"),
                Instant.parse("2020-09-16T16:14:02.233Z"),
                Arrays.asList(Instant.parse("2020-09-16T15:14:02.233Z"), Instant.parse("2020-09-16T18:14:02.233Z")));

        DateTime expected = new DateTime("2020-09-16T18:14:02.233Z");

        Assert.assertEquals(result, expected);
    }
}
