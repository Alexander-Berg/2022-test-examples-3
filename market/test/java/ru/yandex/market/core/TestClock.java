package ru.yandex.market.core;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Clock для тестов.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 * @see ru.yandex.common.util.date.TestableClock
 */
public class TestClock extends Clock {

    private final ZoneId zone;

    public TestClock() {
        this(ZoneId.systemDefault());
    }

    TestClock(final ZoneId zone) {
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        if (zone.equals(this.zone)) {
            return this;
        }
        return new TestClock(zone);
    }

    @Override
    public long millis() {
        return System.currentTimeMillis();
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(millis());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TestClock testClock = (TestClock) o;
        return Objects.equals(zone, testClock.zone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zone);
    }

    @Override
    public String toString() {
        return "TestClock[" + zone + "]";
    }
}
