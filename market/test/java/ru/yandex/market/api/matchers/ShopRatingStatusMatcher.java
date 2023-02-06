package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.ShopRatingInfo;

public class ShopRatingStatusMatcher {
    public static Matcher<ShopRatingInfo.Status> status(Matcher<ShopRatingInfo.Status> ... statuses) {
        return Matchers.allOf(statuses);
    }

    public static Matcher<ShopRatingInfo.Status> id(String id) {
        return ApiMatchers.map(
            ShopRatingInfo.Status::getId,
            "'id'",
            Matchers.is(id),
            ShopRatingStatusMatcher::toStr
        );
    }

    public static Matcher<ShopRatingInfo.Status> name(String name) {
        return ApiMatchers.map(
            ShopRatingInfo.Status::getName,
            "'name'",
            Matchers.is(name),
            ShopRatingStatusMatcher::toStr
        );
    }

    public static String toStr(ShopRatingInfo.Status status) {
        if (null == status) {
            return "null";
        }
        return MoreObjects.toStringHelper(status)
            .add("id", status.getId())
            .add("name", status.getName())
            .toString();
    }
}
