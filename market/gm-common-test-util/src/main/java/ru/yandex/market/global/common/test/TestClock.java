package ru.yandex.market.global.common.test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import com.google.common.annotations.VisibleForTesting;

/**
 * @author moskovkin@yandex-team.ru
 * @since 15.05.2020
 *
 * Clock started by default at "2000-01-01T00:00:00.00Z".
 * Have setTime() method what allow change application time in runtime for tests purposes.
 */
public class TestClock extends Clock {
    private static final String TEST_CLOCK_START_TIME = "2000-01-01T00:00:00.00Z";
    public static final TestClock INSTANCE = new TestClock(Instant.parse(TEST_CLOCK_START_TIME));

    private Clock delegate;

    public TestClock(Instant startTime) {
        this.delegate = Clock.systemUTC();
        setTime(startTime);
    }

    @Override
    public ZoneId getZone() {
        return delegate.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return delegate.withZone(zone);
    }

    @Override
    public Instant instant() {
        return delegate.instant();
    }

    @VisibleForTesting
    public void setTime(Instant time) {
        delegate = Clock.offset(Clock.systemUTC(), Duration.between(
                Clock.systemUTC().instant(),
                time
        ));
    }
}
