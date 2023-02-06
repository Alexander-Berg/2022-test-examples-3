package ru.yandex.market.api.matchers;

import java.math.BigDecimal;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.cashback.PaymentSystemCashback;

public class PaymentSystemCashbackMatcher {

    public static Matcher<PaymentSystemCashback> paymentSystemCashback(Matcher<PaymentSystemCashback>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<PaymentSystemCashback> promoKey(String value) {
        return ApiMatchers.map(
                PaymentSystemCashback::getPromoKey,
                "'promoKey'",
                Matchers.is(value),
                PaymentSystemCashbackMatcher::toStr
        );
    }

    public static Matcher<PaymentSystemCashback> system(String value) {
        return ApiMatchers.map(
                PaymentSystemCashback::getSystem,
                "'system'",
                Matchers.is(value),
                PaymentSystemCashbackMatcher::toStr
        );
    }

    public static Matcher<PaymentSystemCashback> cashbackPercent(Integer value) {
        return ApiMatchers.map(
                PaymentSystemCashback::getCashbackPercent,
                "'cashbackPercent'",
                Matchers.is(value),
                PaymentSystemCashbackMatcher::toStr
        );
    }

    public static Matcher<PaymentSystemCashback> amount(BigDecimal value) {
        return ApiMatchers.map(
                PaymentSystemCashback::getAmount,
                "'amount'",
                Matchers.is(value),
                PaymentSystemCashbackMatcher::toStr
        );
    }

    public static Matcher<PaymentSystemCashback> agitationPriority(Integer value) {
        return ApiMatchers.map(
                PaymentSystemCashback::getAgitationPriority,
                "'agitationPriority'",
                Matchers.is(value),
                PaymentSystemCashbackMatcher::toStr
        );
    }

    public static String toStr(PaymentSystemCashback paymentSystemCashback) {
        if (null == paymentSystemCashback) {
            return "null";
        }

        return MoreObjects.toStringHelper(paymentSystemCashback)
                .add("promoKey", paymentSystemCashback.getPromoKey())
                .add("system", paymentSystemCashback.getSystem())
                .add("cashbackPercent", paymentSystemCashback.getCashbackPercent())
                .add("amount", paymentSystemCashback.getAmount())
                .add("agitationPriority", paymentSystemCashback.getAgitationPriority())
                .toString();
    }
}
