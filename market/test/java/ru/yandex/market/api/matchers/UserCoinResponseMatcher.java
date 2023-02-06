package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.loyalty.CoinCreationReason;
import ru.yandex.market.api.domain.v2.loyalty.CoinRestriction;
import ru.yandex.market.api.domain.v2.loyalty.UserCoinResponse;

/**
 * Created by fettsery on 06.09.18.
 */
public class UserCoinResponseMatcher {
    public static Matcher<UserCoinResponse> coinResponse(Matcher<UserCoinResponse>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<UserCoinResponse> id(Matcher<Long> matcher) {
        return ApiMatchers.map(
            UserCoinResponse::getId,
            "'id'",
            matcher,
            UserCoinResponseMatcher::toStr
        );
    }

    public static Matcher<UserCoinResponse> title(Matcher<String> matcher) {
        return ApiMatchers.map(
            UserCoinResponse::getTitle,
            "'title'",
            matcher,
            UserCoinResponseMatcher::toStr
        );
    }

    public static Matcher<UserCoinResponse> creationDate(Matcher<String> matcher) {
        return ApiMatchers.map(
            UserCoinResponse::getCreationDate,
            "'creationDate'",
            matcher,
            UserCoinResponseMatcher::toStr
        );
    }

    public static Matcher<UserCoinResponse> endDate(Matcher<String> matcher) {
        return ApiMatchers.map(
            UserCoinResponse::getEndDate,
            "'endDate'",
            matcher,
            UserCoinResponseMatcher::toStr
        );
    }

    public static Matcher<UserCoinResponse> coinRestrictions(Matcher<Iterable<CoinRestriction>> matcher) {
        return ApiMatchers.map(
            UserCoinResponse::getCoinRestrictions,
            "'coinRestrictions'",
            matcher,
            UserCoinResponseMatcher::toStr
        );
    }

    public static Matcher<UserCoinResponse> reason(Matcher<CoinCreationReason> matcher) {
        return ApiMatchers.map(
            UserCoinResponse::getReason,
            "'reason'",
            matcher,
            UserCoinResponseMatcher::toStr
        );
    }

    public static Matcher<UserCoinResponse> reasonParam(Matcher<String> matcher) {
        return ApiMatchers.map(
            UserCoinResponse::getReasonParam,
            "'reasonParam'",
            matcher,
            UserCoinResponseMatcher::toStr
        );
    }

    public static Matcher<UserCoinResponse> reasonOrderIds(Matcher<Iterable<? extends String>> matcher) {
        return ApiMatchers.map(
                UserCoinResponse::getReasonOrderIds,
                "'reasonOrderIds'",
                matcher,
                UserCoinResponseMatcher::toStr
        );
    }

    public static String toStr(UserCoinResponse coinResponse) {
        if (null == coinResponse) {
            return "null";
        }
        return MoreObjects.toStringHelper(coinResponse)
            .add("id", coinResponse.getId())
            .add("title", coinResponse.getTitle())
            .add("creationDate", coinResponse.getCreationDate())
            .add("endDate", coinResponse.getEndDate())
            .add("coinRestrictions", ApiMatchers.collectionToStr(coinResponse.getCoinRestrictions(), CoinRestrictionMatcher::toStr))
            .add("reason", coinResponse.getReason())
            .add("reasonParam", coinResponse.getReasonParam())
            .add("reasonOrderIds", coinResponse.getReasonOrderIds())
            .toString();
    }
}
