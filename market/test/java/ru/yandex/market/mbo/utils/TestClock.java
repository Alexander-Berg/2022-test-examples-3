package ru.yandex.market.mbo.utils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * @author yuramalinov
 * @created 15.05.19
 */
public class TestClock extends Clock {
    private final long start;
    private final ZoneId zoneId = ZoneId.systemDefault();
    private long tick = 0;

    public TestClock() {
        this(Instant.now().toEpochMilli());
    }

    public TestClock(long start) {
        this.start = start;
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(start + TimeUnit.SECONDS.toMillis(tick++));
    }
}
