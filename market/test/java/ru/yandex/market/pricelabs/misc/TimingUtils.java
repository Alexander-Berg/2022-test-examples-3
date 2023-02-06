package ru.yandex.market.pricelabs.misc;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class TimingUtils {
    private static final AtomicLong NOW = new AtomicLong();
    private static final TimeSource TIME_SOURCE = new TimeSource(NOW::get);

    static {
        resetTime();
    }

    private TimingUtils() {
        //
    }

    public static void addTime(long timestampInMillisDiff) {
        NOW.set(NOW.get() + timestampInMillisDiff);
    }

    public static void setTime(Instant instant) {
        NOW.set(instant.toEpochMilli());
    }

    public static void resetTime() {
        NOW.set(System.currentTimeMillis());
    }

    public static TimeSource timeSource() {
        return TIME_SOURCE;
    }

    public static Instant getInstant() {
        return TIME_SOURCE.getInstant();
    }

}
