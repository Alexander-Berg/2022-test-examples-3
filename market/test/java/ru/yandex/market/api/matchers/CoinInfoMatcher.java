package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.loyalty.UserCoinResponse;
import ru.yandex.market.api.user.order.CoinError;
import ru.yandex.market.api.user.order.CoinInfo;

/**
 * Created by fettsery on 06.09.18.
 */
public class CoinInfoMatcher {
    public static Matcher<CoinInfo> coinInfo(Matcher<CoinInfo>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<CoinInfo> unusedCoinIds(Matcher<Iterable<Long>> matcher) {
        return ApiMatchers.map(
            CoinInfo::getUnusedCoinIds,
            "'unusedCoinIds'",
            matcher,
            CoinInfoMatcher::toStr
        );
    }

    public static Matcher<CoinInfo> coinErrors(Matcher<Iterable<CoinError>> matcher) {
        return ApiMatchers.map(
            CoinInfo::getCoinErrors,
            "'coinErrors'",
            matcher,
            CoinInfoMatcher::toStr
        );
    }

    public static Matcher<CoinInfo> allCoins(Matcher<Iterable<UserCoinResponse>> matcher) {
        return ApiMatchers.map(
            CoinInfo::getAllCoins,
            "'allCoins'",
            matcher,
            CoinInfoMatcher::toStr
        );
    }


    public static String toStr(CoinInfo coinInfo) {
        if (null == coinInfo) {
            return "null";
        }
        return MoreObjects.toStringHelper(coinInfo)
            .add("unusedCoinIds", ApiMatchers.collectionToStr(coinInfo.getUnusedCoinIds(), Object::toString))
            .add("coinErrors", ApiMatchers.collectionToStr(coinInfo.getCoinErrors(), CoinErrorMatcher::toStr))
            .add("allCoins", ApiMatchers.collectionToStr(coinInfo.getAllCoins(), UserCoinResponseMatcher::toStr))
            .toString();
    }
}
