package ru.yandex.market.pricelabs.tms.cache;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.TimeSource;
import ru.yandex.market.pricelabs.misc.TimingUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractSimpleCacheTest {

    @Test
    void testCache() {
        var simpleCache = new SimpleCache(TimingUtils.timeSource(), 600);
        var now = TimingUtils.timeSource().getMillis();

        assertEquals(now, simpleCache.getCache());

        TimingUtils.addTime(600 * 1000);

        assertEquals(now, simpleCache.getCache());

        TimingUtils.addTime(1);

        var newNow = TimingUtils.timeSource().getMillis();

        assertEquals(newNow, simpleCache.getCache());

        TimingUtils.addTime(1);

        var newNowForce = TimingUtils.timeSource().getMillis();

        assertEquals(newNowForce, simpleCache.getCache(true));

    }

    private class SimpleCache extends AbstractSimpleCache<Long> {

        SimpleCache(TimeSource timeSource, int refreshPeriodSeconds) {
            super(timeSource, refreshPeriodSeconds);
        }

        @Override
        protected Long getCachedData() {
            return TimingUtils.timeSource().getMillis();
        }
    }
}
