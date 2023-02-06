package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.checkout.BreakInterval;
import ru.yandex.market.api.user.order.checkout.WeekSchedule;

import java.time.DayOfWeek;

public class WeekScheduleMatcher {
    public static Matcher<WeekSchedule> schedule(Matcher<WeekSchedule> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<WeekSchedule> beginDow(DayOfWeek beginDow) {
        return ApiMatchers.map(
            WeekSchedule::getBeginDayOfWeek,
            "'beginDow'",
            Matchers.is(beginDow),
            WeekScheduleMatcher::toStr
        );
    }

    public static Matcher<WeekSchedule> endDow(DayOfWeek endDow) {
        return ApiMatchers.map(
            WeekSchedule::getEndDayOfWeek,
            "'endDow'",
            Matchers.is(endDow),
            WeekScheduleMatcher::toStr
        );
    }

    public static Matcher<WeekSchedule> beginMod(int beginMod) {
        return ApiMatchers.map(
            WeekSchedule::getBeginMinuteOfDay,
            "'beginMod'",
            Matchers.is(beginMod),
            WeekScheduleMatcher::toStr
        );
    }

    public static Matcher<WeekSchedule> endMod(int endMod) {
        return ApiMatchers.map(
            WeekSchedule::getEndMinuteOfDay,
            "'endMod'",
            Matchers.is(endMod),
            WeekScheduleMatcher::toStr
        );
    }

    public static Matcher<WeekSchedule> breaks(Matcher<Iterable<? extends BreakInterval>> intervals) {
        return ApiMatchers.map(
            WeekSchedule::getBreaks,
            "'breaks'",
            intervals,
            WeekScheduleMatcher::toStr
        );
    }

    public static String toStr(WeekSchedule schedule) {
        if (null == schedule) {
            return "null";
        }

        return MoreObjects.toStringHelper(WeekSchedule.class)
            .add("beginDow", schedule.getBeginDayOfWeek())
            .add("endDow", schedule.getEndDayOfWeek())
            .add("beginMod", schedule.getBeginMinuteOfDay())
            .add("endMod", schedule.getEndMinuteOfDay())
            .add("breaks", ApiMatchers.collectionToStr(schedule.getBreaks(), BreakIntervalMatcher::toStr))
            .toString();
    }
}
