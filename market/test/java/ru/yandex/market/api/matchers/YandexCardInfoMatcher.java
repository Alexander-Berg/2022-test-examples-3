package ru.yandex.market.api.matchers;

import java.math.BigDecimal;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.cashback.YandexCardInfo;

public class YandexCardInfoMatcher {

    public static Matcher<YandexCardInfo> yandexCardInfo(Matcher<YandexCardInfo>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<YandexCardInfo> yandexCardPaymentAllowed(Boolean value) {
        return ApiMatchers.map(
                YandexCardInfo::isYandexCardPaymentAllowed,
                "'isYandexCardPaymentAllowed'",
                Matchers.is(value),
                YandexCardInfoMatcher::toStr
        );
    }

    public static Matcher<YandexCardInfo> limit(BigDecimal value) {
        return ApiMatchers.map(
                YandexCardInfo::getLimit,
                "'limit'",
                Matchers.is(value),
                YandexCardInfoMatcher::toStr
        );
    }

    public static String toStr(YandexCardInfo yandexCardInfo) {
        if (null == yandexCardInfo) {
            return "null";
        }

        return MoreObjects.toStringHelper(yandexCardInfo)
                .add("yandexCardPaymentAllowed", yandexCardInfo.isYandexCardPaymentAllowed())
                .add("limit", yandexCardInfo.getLimit())
                .toString();
    }
}
