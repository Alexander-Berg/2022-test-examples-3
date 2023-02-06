package ru.yandex.market.clab;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 22.10.2018
 */
public class ControlledClock extends Clock {
    private final Clock real;
    private final ZoneId zoneId;
    private volatile Clock current;
    private volatile Duration realOffset;

    public ControlledClock(Clock real) {
        this.real = real;
        this.zoneId = real.getZone();
        this.current = real;
        this.realOffset = Duration.ZERO;
    }

    private ControlledClock(Clock real, Clock current, ZoneId zoneId, Duration realOffset) {
        this.real = real;
        this.zoneId = zoneId;
        this.current = current;
        this.realOffset = realOffset;
    }

    public synchronized void pause() {
        realOffset = null;
        current = Clock.fixed(current.instant(), zoneId);
    }

    public synchronized void pauseAndSet(LocalDateTime dateTime) {
        pause();
        current = Clock.fixed(dateTime.atZone(zoneId).toInstant(), zoneId);
    }

    public synchronized void unpause() {
        realOffset = Duration.between(real.instant(), current.instant());
        current = Clock.offset(real, realOffset);
    }

    public boolean isPaused() {
        return realOffset == null;
    }

    public synchronized void tick(Duration duration) {
        // if paused, add to fixed
        // else increase offset
        if (!isPaused()) {
            realOffset = realOffset.plus(duration);
            current = Clock.offset(real, realOffset);
        } else {
            current = Clock.fixed(current.instant().plus(duration), zoneId);
        }
    }

    public synchronized void tickMinute() {
        tick(Duration.of(1, ChronoUnit.MINUTES));
    }

    public synchronized void tickHour() {
        tick(Duration.of(1, ChronoUnit.HOURS));
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return zone;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return ControlledClock.this.withZone(zone);
            }

            @Override
            public Instant instant() {
                return ControlledClock.this.instant();
            }
        };
    }

    @Override
    public Instant instant() {
        return current.instant();
    }
}
