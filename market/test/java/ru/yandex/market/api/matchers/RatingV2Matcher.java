package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.RatingV2;
import ru.yandex.market.api.domain.v2.ShopRatingInfo;

import java.math.BigDecimal;

public class RatingV2Matcher {
    public static <T extends RatingV2> Matcher<T> rating(Matcher<T> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static <T extends RatingV2> Matcher<T> value(BigDecimal rating) {
        return ApiMatchers.map(
            T::getRating,
            "'rating'",
            Matchers.is(rating),
            RatingV2Matcher::toStr
        );
    }

    public static <T extends RatingV2> Matcher<T> count(int count) {
        return ApiMatchers.map(
            T::getCount,
            "'count'",
            Matchers.is(count),
            RatingV2Matcher::toStr
        );
    }

    public static Matcher<ShopRatingInfo> status(Matcher<ShopRatingInfo.Status> ... statuses) {
        return ApiMatchers.map(
            ShopRatingInfo::getStatus,
            "'status'",
            Matchers.allOf(statuses),
            RatingV2Matcher::toStr
        );
    }

    public static <T extends RatingV2> String toStr(T rating) {
        if (null == rating) {
            return "null";
        }

        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(rating)
            .add("value", rating.getRating())
            .add("count", rating.getCount());

        if (rating instanceof ShopRatingInfo) {
            ShopRatingInfo.Status status = ((ShopRatingInfo) rating).getStatus();
            helper.add("status", ShopRatingStatusMatcher.toStr(status));
        }

        return helper.toString();
    }
}
