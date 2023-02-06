package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.cashback.CashbackOptionPrecondition;
import ru.yandex.market.api.user.order.cashback.CashbackOptionProfile;
import ru.yandex.market.api.user.order.cashback.CashbackOptions;
import ru.yandex.market.api.user.order.cashback.CashbackType;
import ru.yandex.market.api.user.order.cashback.OrderCashback;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;

public class CashbackOptionProfileMathcher {

    public static Matcher<CashbackOptionProfile> cashbackProfile(Matcher<CashbackOptionProfile>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<CashbackOptionProfile> types(Matcher<Iterable<CashbackType>> matcher) {
        return ApiMatchers.map(
                CashbackOptionProfile::getCashbackTypes,
                "'cashbackTypes'",
                matcher,
                CashbackOptionProfileMathcher::toStr
        );
    }

    public static Matcher<CashbackOptionProfile> preconditions(Matcher<Iterable<CashbackOptionPrecondition>> matcher) {
        return ApiMatchers.map(
                CashbackOptionProfile::getCashbackOptionsPrecondition,
                "'cashbackOptionsPreconditions'",
                matcher,
                CashbackOptionProfileMathcher::toStr
        );
    }

    public static Matcher<CashbackOptionProfile> paymentTypes(Matcher<Iterable<PaymentType>> matcher) {
        return ApiMatchers.map(
                CashbackOptionProfile::getPaymentTypes,
                "'paymentTypes'",
                matcher,
                CashbackOptionProfileMathcher::toStr
        );
    }

    public static Matcher<CashbackOptionProfile> deliveryTypes(Matcher<Iterable<DeliveryType>> matcher) {
        return ApiMatchers.map(
                CashbackOptionProfile::getDeliveryTypes,
                "'deliveryTypes'",
                matcher,
                CashbackOptionProfileMathcher::toStr
        );
    }

    public static Matcher<CashbackOptionProfile> profileCashback(Matcher<CashbackOptions> matcher) {
        return ApiMatchers.map(
                CashbackOptionProfile::getCashback,
                "'cashback'",
                matcher,
                CashbackOptionProfileMathcher::toStr
        );
    }

    public static Matcher<CashbackOptionProfile> orders(Matcher<Iterable<OrderCashback>> matcher) {
        return ApiMatchers.map(
                CashbackOptionProfile::getOrders,
                "'orders'",
                matcher,
                CashbackOptionProfileMathcher::toStr
        );
    }

    public static String toStr(CashbackOptionProfile profile) {
        if (null == profile) {
            return "null";
        }
        return MoreObjects.toStringHelper(profile)
                .add("cashbackTypes", ApiMatchers.collectionToStr(profile.getCashbackTypes(), CashbackType::toString))
                .add("cashbackOptionsPreconditions",
                        ApiMatchers.collectionToStr(profile.getCashbackOptionsPrecondition(),
                                CashbackOptionPrecondition::toString))
                .add("paymentTypes", ApiMatchers.collectionToStr(profile.getPaymentTypes(), PaymentType::toString))
                .add("cashback", CashbackOptionsMatcher.toStr(profile.getCashback()))
                .add("orders", ApiMatchers.collectionToStr(profile.getOrders(), CashbackOrderMatcher::toStr))
                .toString();
    }
}
