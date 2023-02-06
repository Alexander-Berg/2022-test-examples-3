package ru.yandex.market.api.matchers;

import java.math.BigDecimal;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.cashback.WelcomeCashback;

public class WelcomeCashbackMatcher {

    public static Matcher<WelcomeCashback> welcomeCashback(Matcher<WelcomeCashback>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<WelcomeCashback> promoKey(String value) {
        return ApiMatchers.map(
                WelcomeCashback::getPromoKey,
                "'promoKey'",
                Matchers.is(value),
                WelcomeCashbackMatcher::toStr
        );
    }

    public static Matcher<WelcomeCashback> remainingMultiCartTotal(BigDecimal value) {
        return ApiMatchers.map(
                WelcomeCashback::getRemainingMultiCartTotal,
                "'remainingMultiCartTotal'",
                Matchers.is(value),
                WelcomeCashbackMatcher::toStr
        );
    }

    public static Matcher<WelcomeCashback> minMultiCartTotal(BigDecimal value) {
        return ApiMatchers.map(
                WelcomeCashback::getMinMultiCartTotal,
                "'minMultiCartTotal'",
                Matchers.is(value),
                WelcomeCashbackMatcher::toStr
        );
    }

    public static Matcher<WelcomeCashback> amount(BigDecimal value) {
        return ApiMatchers.map(
                WelcomeCashback::getAmount,
                "'amount'",
                Matchers.is(value),
                WelcomeCashbackMatcher::toStr
        );
    }

    public static Matcher<WelcomeCashback> agitationPriority(Integer value) {
        return ApiMatchers.map(
                WelcomeCashback::getAgitationPriority,
                "'agitationPriority'",
                Matchers.is(value),
                WelcomeCashbackMatcher::toStr
        );
    }

    public static String toStr(WelcomeCashback welcomeCashback) {
        if (null == welcomeCashback) {
            return "null";
        }

        return MoreObjects.toStringHelper(welcomeCashback)
                .add("promoKey", welcomeCashback.getPromoKey())
                .add("remainingMultiCartTotal", welcomeCashback.getRemainingMultiCartTotal())
                .add("minMultiCartTotal", welcomeCashback.getMinMultiCartTotal())
                .add("amount", welcomeCashback.getAmount())
                .add("agitationPriority", welcomeCashback.getAgitationPriority())
                .toString();
    }
}
