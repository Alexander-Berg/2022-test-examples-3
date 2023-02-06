package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.BreakIntervalV2;
import ru.yandex.market.api.domain.v2.OpenHoursV2;

public class OpenHoursMatcher {
    public static Matcher<OpenHoursV2> openHours(Matcher<OpenHoursV2>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<OpenHoursV2> daysFrom(String daysFrom) {
        return ApiMatchers.map(
            OpenHoursV2::getDaysFrom,
            "'daysFrom'",
            Matchers.is(daysFrom),
            OpenHoursMatcher::toStr
        );
    }


    public static Matcher<OpenHoursV2> daysTill(String daysTill) {
        return ApiMatchers.map(
            OpenHoursV2::getDaysTill,
            "'daysTill'",
            Matchers.is(daysTill),
            OpenHoursMatcher::toStr
        );
    }

    public static Matcher<OpenHoursV2> from(String from) {
        return ApiMatchers.map(
            OpenHoursV2::getFrom,
            "'from'",
            Matchers.is(from),
            OpenHoursMatcher::toStr
        );
    }


    public static Matcher<OpenHoursV2> till(String till) {
        return ApiMatchers.map(
            OpenHoursV2::getTill,
            "'till'",
            Matchers.is(till),
            OpenHoursMatcher::toStr
        );
    }


    public static Matcher<OpenHoursV2> breaks(Matcher<Iterable<BreakIntervalV2>> interval) {
        return ApiMatchers.map(
            OpenHoursV2::getBreaks,
            "'breaks'",
            interval,
            OpenHoursMatcher::toStr
        );
    }


    public static String toStr(OpenHoursV2 openHours) {
        if (null == openHours) {
            return "null";
        }
        return MoreObjects.toStringHelper(OpenHoursV2.class)
            .add("daysFrom", openHours.getDaysFrom())
            .add("daysTill", openHours.getDaysTill())
            .add("from", openHours.getFrom())
            .add("till", openHours.getTill())
            .add("breaks", ApiMatchers.collectionToStr(openHours.getBreaks(), BreakIntervalV2Matcher::toStr))
            .toString();
    }
}
