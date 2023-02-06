package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.cashback.CashbackOptions;
import ru.yandex.market.api.user.order.cashback.OrderItemCashback;

public class CashbackOrderItemMatcher {

    public static Matcher<OrderItemCashback> orderItemCashback(Matcher<OrderItemCashback>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<OrderItemCashback> feedId(Long value) {
        return ApiMatchers.map(
                OrderItemCashback::getFeedId,
                "'feedId'",
                Matchers.is(value),
                CashbackOrderItemMatcher::toStr
        );
    }

    public static Matcher<OrderItemCashback> offerId(String value) {
        return ApiMatchers.map(
                OrderItemCashback::getOfferId,
                "'offerId'",
                Matchers.is(value),
                CashbackOrderItemMatcher::toStr
        );
    }

    public static Matcher<OrderItemCashback> cartId(String value) {
        return ApiMatchers.map(
                OrderItemCashback::getCartId,
                "'cartId'",
                Matchers.is(value),
                CashbackOrderItemMatcher::toStr
        );
    }

    public static Matcher<OrderItemCashback> bundleId(String value) {
        return ApiMatchers.map(
                OrderItemCashback::getBundleId,
                "'bundleId'",
                Matchers.is(value),
                CashbackOrderItemMatcher::toStr
        );
    }

    public static Matcher<OrderItemCashback> itemCashback(Matcher<CashbackOptions> matcher) {
        return ApiMatchers.map(
                OrderItemCashback::getCashback,
                "'cashback'",
                matcher,
                CashbackOrderItemMatcher::toStr
        );
    }

    public static String toStr(OrderItemCashback itemCashback) {
        if (null == itemCashback) {
            return "null";
        }

        return MoreObjects.toStringHelper(itemCashback)
                .add("feedId", itemCashback.getFeedId())
                .add("offerId", itemCashback.getOfferId())
                .add("cashback", CashbackOptionsMatcher.toStr(itemCashback.getCashback()))
                .add("cartId", itemCashback.getCartId())
                .add("bundleId", itemCashback.getBundleId())
                .toString();
    }
}
