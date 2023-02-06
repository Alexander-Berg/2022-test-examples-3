package ru.yandex.market.logistics.management.util;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class TestableClock extends Clock {
    private final Clock system = Clock.systemDefaultZone();
    private volatile Clock fixed = null;

    @Override
    public ZoneId getZone() {
        if (fixed == null) {
            return system.getZone();
        }
        return fixed.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        if (fixed == null) {
            return system.withZone(zone);
        }
        return fixed.withZone(zone);
    }

    @Override
    public Instant instant() {
        if (fixed == null) {
            return system.instant();
        }
        return fixed.instant();
    }

    /**
     * Устанавливает фиксированное время на часах. Не нужно вызывать из продакшен кода.
     */
    @VisibleForTesting
    public void setFixed(Instant instant, ZoneId zoneId) {
        this.fixed = Clock.fixed(instant, zoneId);
    }

    /**
     * Возвращает часы в нормальное состояние. Не нужно вызывать из продакшен кода.
     */
    @VisibleForTesting
    public void clearFixed() {
        this.fixed = null;
    }
}
