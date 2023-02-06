package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.BreakIntervalV2;

public class BreakIntervalV2Matcher {
    public static Matcher<BreakIntervalV2> interval(Matcher<BreakIntervalV2> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<BreakIntervalV2> from(String from) {
        return ApiMatchers.map(
            BreakIntervalV2::getFrom,
            "'from'",
            Matchers.is(from),
            BreakIntervalV2Matcher::toStr
        );
    }


    public static Matcher<BreakIntervalV2> till(String till) {
        return ApiMatchers.map(
            BreakIntervalV2::getTill,
            "'till'",
            Matchers.is(till),
            BreakIntervalV2Matcher::toStr
        );
    }

    public static String toStr(BreakIntervalV2 interval) {
        if (null == interval) {
            return "null";
        }

        return MoreObjects.toStringHelper(BreakIntervalV2.class)
            .add("from", interval.getFrom())
            .add("till", interval.getTill())
            .toString();
    }
}
