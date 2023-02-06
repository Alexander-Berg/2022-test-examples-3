package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.OpenHoursV2;
import ru.yandex.market.api.domain.v2.OutletV2;
import ru.yandex.market.api.offer.Phone;

public class OutletMatcher {
    public static Matcher<OutletV2> outlet(Matcher<OutletV2> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<OutletV2> id(String id) {
        return ApiMatchers.map(
            OutletV2::getId,
            "'id'",
            Matchers.is(id),
            OutletMatcher::toStr
        );
    }


    public static Matcher<OutletV2> name(String name) {
        return ApiMatchers.map(
            OutletV2::getName,
            "'name'",
            Matchers.is(name),
            OutletMatcher::toStr
        );
    }


    public static Matcher<OutletV2> type(String type) {
        return ApiMatchers.map(
            OutletV2::getType,
            "'type'",
            Matchers.is(type),
            OutletMatcher::toStr
        );
    }


    public static Matcher<OutletV2> shopId(long shopId) {
        return ApiMatchers.map(
            OutletV2::getShopId,
            "'shopId'",
            Matchers.is(shopId),
            OutletMatcher::toStr
        );
    }

    public static Matcher<OutletV2> phones(Matcher<Iterable<Phone>> phones) {
        return ApiMatchers.map(
            OutletV2::getPhones,
            "'phones'",
            phones,
            OutletMatcher::toStr
        );
    }

    public static Matcher<OutletV2> schedule(Matcher<Iterable<OpenHoursV2>> schedule) {
        return ApiMatchers.map(
            OutletV2::getSchedule,
            "'schedule'",
            schedule,
            OutletMatcher::toStr
        );
    }

    public static String toStr(OutletV2 outlet) {
        if (null == outlet) {
            return "null";
        }
        return MoreObjects.toStringHelper(OutletV2.class)
            .add("id", outlet.getId())
            .add("name", outlet.getName())
            .add("type", outlet.getType())
            .add("shopId", outlet.getShopId())
            .add("phones", ApiMatchers.collectionToStr(outlet.getPhones(), PhoneMatcher::toStr))
            .add("schedule", ApiMatchers.collectionToStr(outlet.getSchedule(), OpenHoursMatcher::toStr))
            .toString();
    }
}
