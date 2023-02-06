package ru.yandex.market.api.matchers;

import java.math.BigDecimal;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.cashback.Cashback;
import ru.yandex.market.api.user.order.cashback.CashbackOptionProfile;
import ru.yandex.market.api.user.order.cashback.CashbackOptions;
import ru.yandex.market.api.user.order.cashback.CashbackType;
import ru.yandex.market.api.user.order.cashback.GrowingCashback;
import ru.yandex.market.api.user.order.cashback.PaymentSystemCashback;
import ru.yandex.market.api.user.order.cashback.WelcomeCashback;

public class CashbackMatcher {

    public static Matcher<Cashback> cashback(Matcher<Cashback>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<Cashback> balance(BigDecimal value) {
        return ApiMatchers.map(
                Cashback::getCashbackBalance,
                "'cashbackBalance'",
                Matchers.is(value),
                CashbackMatcher::toStr
        );
    }

    public static Matcher<Cashback> selectedCashbackOption(CashbackType value) {
        return ApiMatchers.map(
                Cashback::getSelectedCashbackOption,
                "'cashbackBalance'",
                Matchers.is(value),
                CashbackMatcher::toStr
        );
    }

    public static Matcher<Cashback> cashbackProfiles(Matcher<Iterable<CashbackOptionProfile>> matcher) {
        return ApiMatchers.map(
                Cashback::getCashbackOptionsProfiles,
                "'cashbackOptionsProfiles'",
                matcher,
                CashbackMatcher::toStr
        );
    }

    public static Matcher<Cashback> applicableCashback(Matcher<CashbackOptions> matcher) {
        return ApiMatchers.map(
                Cashback::getApplicableOptions,
                "'applicableCashback'",
                matcher,
                CashbackMatcher::toStr
        );
    }

    public static Matcher<Cashback> welcomeCashback(Matcher<WelcomeCashback> matcher) {
        return ApiMatchers.map(
                Cashback::getWelcomeCashback,
                "'welcomeCashback'",
                matcher,
                CashbackMatcher::toStr
        );
    }

    public static Matcher<Cashback> paymentSystemCashback(Matcher<PaymentSystemCashback> matcher) {
        return ApiMatchers.map(
                Cashback::getPaymentSystemCashback,
                "'paymentSystemCashback'",
                matcher,
                CashbackMatcher::toStr
        );
    }

    public static Matcher<Cashback> growingCashback(Matcher<GrowingCashback> matcher) {
        return ApiMatchers.map(
                Cashback::getGrowingCashback,
                "'growingCashback'",
                matcher,
                CashbackMatcher::toStr
        );
    }

    public static String toStr(Cashback cashback) {
        if (null == cashback) {
            return "null";
        }
        return MoreObjects.toStringHelper(cashback)
                .add("cashbackBalance", cashback.getCashbackBalance())
                .add("selectedCashbackOption", cashback.getSelectedCashbackOption())
                .add("cashbackOptionsProfiles", ApiMatchers.collectionToStr(cashback.getCashbackOptionsProfiles(),
                        CashbackOptionProfileMathcher::toStr))
                .add("welcomeCashback", cashback.getWelcomeCashback())
                .add("growingCashback", cashback.getGrowingCashback())
                .add("paymentSystemCashback", cashback.getPaymentSystemCashback())
                .toString();
    }
}
