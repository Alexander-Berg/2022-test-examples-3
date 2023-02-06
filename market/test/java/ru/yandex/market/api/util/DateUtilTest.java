package ru.yandex.market.api.util;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

import ru.yandex.market.api.integration.UnitTestBase;

/**
 * Created by fettsery on 13.06.18.
 */
public class DateUtilTest extends UnitTestBase {
    @Test
    public void shouldBeInBounds() {
        LocalDateTime from = LocalDateTime.parse("2018-06-06T10:00:00");
        LocalDateTime to = LocalDateTime.parse("2018-06-08T10:00:00");
        LocalDateTime date = LocalDateTime.parse("2018-06-07T10:00:00");
        Assert.assertTrue(DateUtil.isInBounds(from, to, date));
    }

    @Test
    public void shouldNotBeInBounds() {
        LocalDateTime from = LocalDateTime.parse("2018-06-06T10:00:00");
        LocalDateTime to = LocalDateTime.parse("2018-06-08T10:00:00");
        LocalDateTime date = LocalDateTime.parse("2018-06-10T10:00:00");
        Assert.assertFalse(DateUtil.isInBounds(from, to, date));
    }

    @Test
    public void shouldBeInLeftBound() {
        LocalDateTime to = LocalDateTime.parse("2018-06-08T10:00:00");
        LocalDateTime date = LocalDateTime.parse("2018-05-10T10:00:00");
        Assert.assertTrue(DateUtil.isInBounds(null, to, date));
    }

    @Test
    public void shouldBeInRightBound() {
        LocalDateTime from = LocalDateTime.parse("2018-06-08T10:00:00");
        LocalDateTime date = LocalDateTime.parse("2018-07-10T10:00:00");
        Assert.assertTrue(DateUtil.isInBounds(from, null, date));
    }

    @Test
    public void shouldBeInNoBounds() {
        LocalDateTime date = LocalDateTime.parse("2018-07-10T10:00:00");
        Assert.assertTrue(DateUtil.isInBounds(null, null, date));
    }
}
