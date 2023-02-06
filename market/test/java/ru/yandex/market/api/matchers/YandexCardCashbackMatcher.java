package ru.yandex.market.api.matchers;

import java.math.BigDecimal;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.cashback.YandexCardCashback;

public class YandexCardCashbackMatcher {

    public static Matcher<YandexCardCashback> yandexCardCashback(Matcher<YandexCardCashback>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<YandexCardCashback> amount(BigDecimal value) {
        return ApiMatchers.map(
                YandexCardCashback::getAmount,
                "'amount'",
                Matchers.is(value),
                YandexCardCashbackMatcher::toStr
        );
    }

    public static Matcher<YandexCardCashback> cashbackPercent(Integer value) {
        return ApiMatchers.map(
                YandexCardCashback::getCashbackPercent,
                "'cashbackPercent'",
                Matchers.is(value),
                YandexCardCashbackMatcher::toStr
        );
    }

    public static Matcher<YandexCardCashback> promoKey(String value) {
        return ApiMatchers.map(
                YandexCardCashback::getPromoKey,
                "'promoKey'",
                Matchers.is(value),
                YandexCardCashbackMatcher::toStr
        );
    }

    public static Matcher<YandexCardCashback> maxOrderTotal(BigDecimal value) {
        return ApiMatchers.map(
                YandexCardCashback::getMaxOrderTotal,
                "'maxOrderTotal'",
                Matchers.is(value),
                YandexCardCashbackMatcher::toStr
        );
    }

    public static Matcher<YandexCardCashback> agitationPriority(Integer value) {
        return ApiMatchers.map(
                YandexCardCashback::getAgitationPriority,
                "'agitationPriority'",
                Matchers.is(value),
                YandexCardCashbackMatcher::toStr
        );
    }

    public static String toStr(YandexCardCashback yandexCardCashback) {
        if (null == yandexCardCashback) {
            return "null";
        }

        return MoreObjects.toStringHelper(yandexCardCashback)
                .add("amount", yandexCardCashback.getAmount())
                .add("promoKey", yandexCardCashback.getPromoKey())
                .add("cashbackPercent", yandexCardCashback.getCashbackPercent())
                .add("maxOrderTotal", yandexCardCashback.getMaxOrderTotal())
                .add("agitationPriority", yandexCardCashback.getAgitationPriority())
                .toString();
    }
}
