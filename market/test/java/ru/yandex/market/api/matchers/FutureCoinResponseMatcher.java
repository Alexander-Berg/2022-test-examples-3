package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.loyalty.BaseCoinResponse;
import ru.yandex.market.api.domain.v2.loyalty.CoinRestriction;
import ru.yandex.market.api.domain.v2.loyalty.FutureCoinResponse;

/**
 * Created by fettsery on 06.09.18.
 */
public class FutureCoinResponseMatcher {
    public static Matcher<FutureCoinResponse> coinResponse(Matcher<FutureCoinResponse>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<FutureCoinResponse> title(Matcher<String> matcher) {
        return ApiMatchers.map(
            FutureCoinResponse::getTitle,
            "'title'",
            matcher,
            FutureCoinResponseMatcher::toStr
        );
    }

    public static Matcher<FutureCoinResponse> promoId(Matcher<Long> matcher) {
        return ApiMatchers.map(
            FutureCoinResponse::getPromoId,
            "'promoId'",
            matcher,
            FutureCoinResponseMatcher::toStr
        );
    }

    public static Matcher<FutureCoinResponse> coinType(Matcher<BaseCoinResponse.CoinType> matcher) {
        return ApiMatchers.map(
            FutureCoinResponse::getCoinType,
            "'coinType'",
            matcher,
            FutureCoinResponseMatcher::toStr
        );
    }

    public static Matcher<FutureCoinResponse> coinRestrictions(Matcher<Iterable<CoinRestriction>> matcher) {
        return ApiMatchers.map(
            FutureCoinResponse::getCoinRestrictions,
            "'coinRestrictions'",
            matcher,
            FutureCoinResponseMatcher::toStr
        );
    }


    public static String toStr(FutureCoinResponse coinResponse) {
        if (null == coinResponse) {
            return "null";
        }
        return MoreObjects.toStringHelper(coinResponse)
            .add("title", coinResponse.getTitle())
            .add("promoId", coinResponse.getPromoId())
            .add("coinType", coinResponse.getCoinType())
            .add("coinRestrictions", ApiMatchers.collectionToStr(coinResponse.getCoinRestrictions(), CoinRestrictionMatcher::toStr))
            .toString();
    }
}
