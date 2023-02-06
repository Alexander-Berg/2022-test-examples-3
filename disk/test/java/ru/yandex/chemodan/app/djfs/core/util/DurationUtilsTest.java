package ru.yandex.chemodan.app.djfs.core.util;

import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class DurationUtilsTest {
    @Test
    public void formatAsSeconds() {
        Assert.equals("0.000", DurationUtils.formatAsSecondsWithMillis(Duration.ZERO));
        Assert.equals("1.000", DurationUtils.formatAsSecondsWithMillis(Duration.standardSeconds(1)));
        Assert.equals("60.000", DurationUtils.formatAsSecondsWithMillis(Duration.standardSeconds(60)));
        Assert.equals("3600.000", DurationUtils.formatAsSecondsWithMillis(Duration.standardHours(1)));
        Assert.equals("1.100", DurationUtils.formatAsSecondsWithMillis(Duration.millis(1100)));
        Assert.equals("123.456", DurationUtils.formatAsSecondsWithMillis(Duration.millis(123456)));
    }
}
