package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.cashback.CashbackOption;
import ru.yandex.market.api.user.order.cashback.CashbackOptions;

public class CashbackOptionsMatcher {

    public static Matcher<CashbackOptions> cashbackOptions(Matcher<CashbackOptions>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<CashbackOptions> emit(Matcher<CashbackOption> matcher) {
        return ApiMatchers.map(
                CashbackOptions::getEmit,
                "'emit'",
                matcher,
                CashbackOptionsMatcher::toStr
        );
    }

    public static Matcher<CashbackOptions> spend(Matcher<CashbackOption> matcher) {
        return ApiMatchers.map(
                CashbackOptions::getSpend,
                "'spend'",
                matcher,
                CashbackOptionsMatcher::toStr
        );
    }

    public static String toStr(CashbackOptions options) {
        if (null == options) {
            return "null";
        }
        return MoreObjects.toStringHelper(options)
                .add("emit", CashbackOptionMatcher.toStr(options.getEmit()))
                .add("spend", CashbackOptionMatcher.toStr(options.getSpend()))
                .toString();
    }
}
