package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.checkout.BreakInterval;


public class BreakIntervalMatcher {
    public static Matcher<BreakInterval> intervals(Matcher<BreakInterval>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<BreakInterval> begin(String begin) {
        return ApiMatchers.map(
            BreakInterval::getBegin,
            "'begin'",
            Matchers.is(begin),
            BreakIntervalMatcher::toStr
        );
    }

    public static Matcher<BreakInterval> end(String end) {
        return ApiMatchers.map(
            BreakInterval::getEnd,
            "'end'",
            Matchers.is(end),
            BreakIntervalMatcher::toStr
        );
    }

    public static String toStr(BreakInterval interval) {
        if (null == interval) {
            return "null";
        }
        return MoreObjects.toStringHelper(BreakInterval.class)
            .add("begin", interval.getBegin())
            .add("end", interval.getEnd())
            .toString();
    }
}
