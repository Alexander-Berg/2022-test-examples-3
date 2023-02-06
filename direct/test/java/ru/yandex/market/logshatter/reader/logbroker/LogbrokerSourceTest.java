package ru.yandex.market.logshatter.reader.logbroker;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 18.08.17
 */
public class LogbrokerSourceTest {
    @Test
    public void fromSourcePath() throws Exception {
        Assert.assertEquals(
            new LogbrokerSource("market-health", null),
            LogbrokerSource.fromSourcePath("market-health")
        );

        Assert.assertEquals(
            new LogbrokerSource("market-health", "other"),
            LogbrokerSource.fromSourcePath("market-health--other")
        );
    }

    @Test
    public void fromTopic() throws Exception {
        Assert.assertEquals(
            new LogbrokerSource("market-health-prestable", "push-client-log"),
            LogbrokerSource.fromTopic("rt3.man--market-health-prestable--push-client-log")
        );

        Assert.assertEquals(
            new LogbrokerSource("market-health-testing", "other"),
            LogbrokerSource.fromTopic("rt3.iva--market-health-testing--other")
        );
    }

    @Test
    public void fromPartition() throws Exception {
        Assert.assertEquals(
            new LogbrokerSource("market-health-prestable", "push-client-log"),
            LogbrokerSource.fromPartition("rt3.man--market-health-prestable--push-client-log:42")
        );
    }

    @Test
    public void testEqualsAndHashCode() throws Exception {
        LogbrokerSource source = new LogbrokerSource("market-health-prestable", "push-client-log");
        LogbrokerSource sameSource = new LogbrokerSource("market-health-prestable", "push-client-log");
        LogbrokerSource otherSource = new LogbrokerSource("market-health-stable", "push-client-log");

        Assert.assertTrue(source.equals(sameSource));
        Assert.assertEquals(source.hashCode(), sameSource.hashCode());
        Assert.assertFalse(source.equals(otherSource));
    }
}