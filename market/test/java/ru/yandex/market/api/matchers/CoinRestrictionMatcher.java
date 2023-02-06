package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.loyalty.CoinRestriction;

/**
 * Created by fettsery on 06.09.18.
 */
public class CoinRestrictionMatcher {
    public static Matcher<CoinRestriction> coinRestriction(Matcher<CoinRestriction> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<CoinRestriction> categoryId(Matcher<Long> matcher) {
        return ApiMatchers.map(
            CoinRestriction::getCategoryId,
            "'categoryId'",
            matcher,
            CoinRestrictionMatcher::toStr
        );
    }

    public static Matcher<CoinRestriction> skuId(Matcher<String> matcher) {
        return ApiMatchers.map(
            CoinRestriction::getSkuId,
            "'skuId'",
            matcher,
            CoinRestrictionMatcher::toStr
        );
    }

    public static Matcher<CoinRestriction> restrictionType(Matcher<String> matcher) {
        return ApiMatchers.map(
            CoinRestriction::getRestrictionType,
            "'restrictionType'",
            matcher,
            CoinRestrictionMatcher::toStr
        );
    }

    public static String toStr(CoinRestriction coinRestriction) {
        if (null == coinRestriction) {
            return "null";
        }
        return MoreObjects.toStringHelper(coinRestriction)
            .add("categoryId", coinRestriction.getCategoryId())
            .add("skuId", coinRestriction.getSkuId())
            .add("restrictionType", coinRestriction.getRestrictionType())
            .toString();
    }
}
