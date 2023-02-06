package ru.yandex.market.logshatter.reader;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.logshatter.utils.QueueLimitsMapper;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 25.04.18
 */
public class QueuesLimitsTest {
    @Test
    public void checksById() {
        QueuesLimits limits = new QueuesLimits("market-health-.+:1,^marketstat$:2,blabla--testing:3");
        Assert.assertEquals(3, limits.getLimits().size());

        QueuesLimits.QueueLimit limit = QueueLimitsMapper.toMap(limits.getLimits()).get("market-health-.+");
        Assert.assertEquals(1024L * 1024L, limit.getLimitBytes());
        Assert.assertTrue(limit.check("market-health-testing"));
        Assert.assertFalse(limit.check("a-market-health-testing"));

        limit = QueueLimitsMapper.toMap(limits.getLimits()).get("^marketstat$");
        Assert.assertEquals(2L * 1024L * 1024L, limit.getLimitBytes());
        Assert.assertTrue(limit.check("marketstat"));
        Assert.assertFalse(limit.check("marketstat$a"));

        limit = QueueLimitsMapper.toMap(limits.getLimits()).get("blabla--testing");
        Assert.assertEquals(3L * 1024L * 1024L, limit.getLimitBytes());
        Assert.assertTrue(limit.check("blabla--testing"));
        Assert.assertFalse(limit.check("blabla--stable"));

    }
}
