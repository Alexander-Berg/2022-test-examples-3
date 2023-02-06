package ru.yandex.market.api.matchers;

import java.math.BigDecimal;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.cashback.GrowingCashback;

public class GrowingCashbackMatcher {

    public static Matcher<GrowingCashback> growingCashback(Matcher<GrowingCashback>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<GrowingCashback> promoKey(String value) {
        return ApiMatchers.map(
                GrowingCashback::getPromoKey,
                "'promoKey'",
                Matchers.is(value),
                GrowingCashbackMatcher::toStr
        );
    }

    public static Matcher<GrowingCashback> remainingMultiCartTotal(BigDecimal value) {
        return ApiMatchers.map(
                GrowingCashback::getRemainingMultiCartTotal,
                "'remainingMultiCartTotal'",
                Matchers.is(value),
                GrowingCashbackMatcher::toStr
        );
    }

    public static Matcher<GrowingCashback> minMultiCartTotal(BigDecimal value) {
        return ApiMatchers.map(
                GrowingCashback::getMinMultiCartTotal,
                "'minMultiCartTotal'",
                Matchers.is(value),
                GrowingCashbackMatcher::toStr
        );
    }

    public static Matcher<GrowingCashback> amount(BigDecimal value) {
        return ApiMatchers.map(
                GrowingCashback::getAmount,
                "'amount'",
                Matchers.is(value),
                GrowingCashbackMatcher::toStr
        );
    }

    public static Matcher<GrowingCashback> agitationPriority(Integer value) {
        return ApiMatchers.map(
                GrowingCashback::getAgitationPriority,
                "agitationPriority",
                Matchers.is(value),
                GrowingCashbackMatcher::toStr
        );
    }

    public static String toStr(GrowingCashback growingCashback) {
        if (null == growingCashback) {
            return "null";
        }

        return MoreObjects.toStringHelper(growingCashback)
                .add("promoKey", growingCashback.getPromoKey())
                .add("remainingMultiCartTotal", growingCashback.getRemainingMultiCartTotal())
                .add("minMultiCartTotal", growingCashback.getMinMultiCartTotal())
                .add("amount", growingCashback.getAmount())
                .add("agitationPriority", growingCashback.getAgitationPriority())
                .toString();
    }
}
