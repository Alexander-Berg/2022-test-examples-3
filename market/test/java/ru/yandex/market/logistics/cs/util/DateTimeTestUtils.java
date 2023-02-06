package ru.yandex.market.logistics.cs.util;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;

import lombok.ToString;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DateTimeTestUtils {
    private static final MicrosecondLocalDateTimeComparator MICROCECONDS_COMPARATOR =
        new MicrosecondLocalDateTimeComparator();

    public Comparator<LocalDateTime> microsecondsComparator() {
        return MICROCECONDS_COMPARATOR;
    }

    private LocalDateTime roundToMicroseconds(LocalDateTime localDateTime) {
        long seconds = localDateTime.toEpochSecond(ZoneOffset.UTC);
        long nanos = localDateTime.getNano();
        return Instant.ofEpochSecond(seconds, Math.round(nanos / 1000000d) * 1000000)
            .atZone(ZoneOffset.UTC)
            .toLocalDateTime();
    }

    @ToString
    private static class MicrosecondLocalDateTimeComparator implements Comparator<LocalDateTime>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(LocalDateTime o1, LocalDateTime o2) {
            return roundToMicroseconds(o1).compareTo(roundToMicroseconds(o2));
        }
    }
}
