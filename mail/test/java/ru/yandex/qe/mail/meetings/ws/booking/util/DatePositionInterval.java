package ru.yandex.qe.mail.meetings.ws.booking.util;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.Interval;

import ru.yandex.qe.mail.meetings.booking.util.IntervalUtils;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Intervalable;

import static ru.yandex.qe.mail.meetings.ws.booking.util.DatePosition.dp;
import static ru.yandex.qe.mail.meetings.ws.booking.util.DatePosition.week;


public final class DatePositionInterval implements Intervalable {
    public final DatePosition left;
    public final DatePosition right;

    public static DatePositionInterval of(DatePosition left, DatePosition right) {
        return new DatePositionInterval(left, right);
    }

    public static DatePositionInterval fromInterval(Interval interval) {
        return of(
                DatePosition.fromDate(interval.getStart().toDate()),
                DatePosition.fromDate(interval.getEnd().toDate())
        );
    }

    public static DatePositionInterval dpInterval(int daysOffset, String range) {
        var parts = range.split("-");
        assert parts.length == 2;
        var leftParts = parts[0].strip().split(":");
        var rightParts = parts[1].strip().split(":");
        return of(
                dp(daysOffset, Integer.parseInt(leftParts[0]), Integer.parseInt(leftParts[1])),
                dp(daysOffset, Integer.parseInt(rightParts[0]), Integer.parseInt(rightParts[1]))
        );
    }

    public static List<Interval> weekWithGaps(DatePositionInterval... freeIntervals) {
        var term = week();

        return IntervalUtils.makeBusy(
                term,
                Arrays.stream(freeIntervals)
                        .map(g -> new Interval(
                                g.left.toMillis(),
                                g.right.toMillis())
                        ).collect(Collectors.toUnmodifiableList())
        );
    }

    private DatePositionInterval(DatePosition left, DatePosition right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return left.toString() + "-" + right.toString();
    }

    @Override
    public Date getStart() {
        return left.toDate();
    }

    @Override
    public Date getEnd() {
        return right.toDate();
    }
}
