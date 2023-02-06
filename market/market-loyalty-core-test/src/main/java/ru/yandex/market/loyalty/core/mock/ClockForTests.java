package ru.yandex.market.loyalty.core.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

public class ClockForTests extends Clock {
    private final ZoneId zoneId;
    private final AtomicReference<Instant> instantRef;
    private final Clock realClock = Clock.systemDefaultZone();
    private volatile boolean useRealClock = false;

    public ClockForTests() {
        this(ZoneId.systemDefault());
    }

    private ClockForTests(ZoneId zoneId) {
        this.zoneId = zoneId;
        this.instantRef = new AtomicReference<>(Instant.now());
    }

    public void useRealClock() {
        useRealClock = true;
    }

    @Override
    public ZoneId getZone() {
        return useRealClock ? realClock.getZone() : zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return useRealClock ? realClock.withZone(zone) : new ClockForTests(zone);
    }

    @Override
    public Instant instant() {
        return useRealClock ? realClock.instant() : instantRef.get();
    }

    public LocalDateTime dateTime() {
        return LocalDateTime.ofInstant(instant(), getZone());
    }

    public void spendTime(long toSpend, TemporalUnit temporalUnit) {
        if (useRealClock) {
            throw new IllegalStateException();
        }
        instantRef.getAndUpdate(instant -> instant.plus(toSpend, temporalUnit));
    }

    public void spendTime(TemporalAmount duration) {
        if (useRealClock) {
            throw new IllegalStateException();
        }
        instantRef.getAndUpdate(instant -> instant.plus(duration));
    }

    public void reset() {
        useRealClock = false;
        instantRef.set(Instant.now());
    }

    public void setDate(Date date) {
        if (useRealClock) {
            throw new IllegalStateException();
        }
        instantRef.set(Instant.ofEpochMilli(date.getTime()));
    }

    @Override
    public long millis() {
        return useRealClock ? realClock.millis() : instantRef.get().toEpochMilli();
    }
}
