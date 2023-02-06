package ru.yandex.direct.test.utils.matcher;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class LocalDateTimeMatcher extends TypeSafeMatcher<LocalDateTime> {

    private boolean exact;
    private boolean inclusive;
    private LocalDateTime from;
    private LocalDateTime to;

    private LocalDateTimeMatcher(boolean exact, boolean inclusive, LocalDateTime from, LocalDateTime to) {
        this.exact = exact;
        this.inclusive = inclusive;
        this.from = from;
        this.to = to;
    }

    public static LocalDateTimeMatcher approximatelyNow() {
        LocalDateTime now = LocalDateTime.now();
        return new LocalDateTimeMatcher(false, true, now.minusMinutes(2L), now.plusMinutes(2L));
    }

    public static Comparator<LocalDateTime> approximatelyTimeComparator(int equalIntervalInSeconds) {
        return (time1, time2) -> {
            if (Math.abs(Duration.between(time1, time2).toSeconds()) < equalIntervalInSeconds) {
                return 0;
            } else {
                return (int) Duration.between(time1, time2).toSeconds();
            }
        };
    }

    public static LocalDateTimeMatcher approximatelyNow(ZoneId zone) {
        LocalDateTime now = LocalDateTime.now(zone);
        return new LocalDateTimeMatcher(false, true, now.minusMinutes(2L), now.plusMinutes(2L));
    }

    public static LocalDateTimeMatcher approximately(LocalDateTime dateTime) {
        return new LocalDateTimeMatcher(false, true, dateTime.minusMinutes(2L), dateTime.plusMinutes(2L));
    }

    public static LocalDateTimeMatcher isAfter(LocalDateTime localDateTime) {
        return new LocalDateTimeMatcher(false, false, localDateTime, LocalDateTime.MAX);
    }

    @Override
    protected boolean matchesSafely(LocalDateTime localDateTime) {
        if (exact) {
            return localDateTime.isEqual(from);
        } else {
            if (inclusive) {
                return (localDateTime.isAfter(from) && localDateTime.isBefore(to)) ||
                        localDateTime.isEqual(from) || localDateTime.isEqual(to);
            } else {
                return localDateTime.isAfter(from) && localDateTime.isBefore(to);
            }
        }
    }

    @Override
    public void describeTo(Description description) {
        if (exact) {
            description
                    .appendText("local date-time is equal to ")
                    .appendText(from.toString());
        } else {
            description.appendText("local date-time is after ");

            if (inclusive) {
                description.appendText("(inclusive) ");
            }

            description
                    .appendText(" than ")
                    .appendText(from.toString())
                    .appendText(" and before ");

            if (inclusive) {
                description.appendText("(inclusive) ");
            }

            description
                    .appendText("than ")
                    .appendText(to.toString());
        }
    }
}
