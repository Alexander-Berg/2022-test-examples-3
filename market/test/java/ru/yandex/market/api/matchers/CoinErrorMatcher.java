package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.CoinError;

/**
 * Created by fettsery on 06.09.18.
 */
public class CoinErrorMatcher {
    public static Matcher<CoinError> coinError(Matcher<CoinError> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<CoinError> coinId(Matcher<Long> matcher) {
        return ApiMatchers.map(
            CoinError::getCoinId,
            "'coinId'",
            matcher,
            CoinErrorMatcher::toStr
        );
    }

    public static Matcher<CoinError> code(Matcher<String> matcher) {
        return ApiMatchers.map(
            CoinError::getCode,
            "'code'",
            matcher,
            CoinErrorMatcher::toStr
        );
    }

    public static Matcher<CoinError> message(Matcher<String> matcher) {
        return ApiMatchers.map(
            CoinError::getMessage,
            "'message'",
            matcher,
            CoinErrorMatcher::toStr
        );
    }

    public static String toStr(CoinError coinError) {
        if (null == coinError) {
            return "null";
        }
        return MoreObjects.toStringHelper(coinError)
            .add("coinId", coinError.getCoinId())
            .add("code", coinError.getCode())
            .add("message", coinError.getMessage())
            .toString();
    }
}
