package ru.yandex.travel.testing.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class SettableClock extends Clock {
    private final ZoneId zoneId;
    private final AtomicReference<Instant> currentTime;

    public SettableClock() {
        this(ZoneId.of("UTC"), new AtomicReference<>(Instant.ofEpochMilli(0)));
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new SettableClock(zone, currentTime);
    }

    @Override
    public Instant instant() {
        return currentTime.get();
    }

    public void setCurrentTime(Instant newInstant) {
        log.info("Setting current test clock time to {}", newInstant);
        currentTime.set(newInstant);
    }
}
