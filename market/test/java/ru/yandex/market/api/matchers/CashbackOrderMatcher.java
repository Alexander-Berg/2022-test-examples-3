package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.cashback.CashbackOptions;
import ru.yandex.market.api.user.order.cashback.OrderCashback;
import ru.yandex.market.api.user.order.cashback.OrderItemCashback;

public class CashbackOrderMatcher {

    public static Matcher<OrderCashback> orderCashback(Matcher<OrderCashback>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<OrderCashback> cartId(String value) {
        return ApiMatchers.map(
                OrderCashback::getCartId,
                "'cartId'",
                Matchers.is(value),
                CashbackOrderMatcher::toStr
        );
    }

    public static Matcher<OrderCashback> orderId(Long value) {
        return ApiMatchers.map(
                OrderCashback::getOrderId,
                "'orderId'",
                Matchers.is(value),
                CashbackOrderMatcher::toStr
        );
    }

    public static Matcher<OrderCashback> orderCashback(Matcher<CashbackOptions> matcher) {
        return ApiMatchers.map(
                OrderCashback::getCashback,
                "'cashback'",
                matcher,
                CashbackOrderMatcher::toStr
        );
    }

    public static Matcher<OrderCashback> orderItems(Matcher<Iterable<OrderItemCashback>> matcher) {
        return ApiMatchers.map(
                OrderCashback::getOrderItems,
                "'orderItems'",
                matcher,
                CashbackOrderMatcher::toStr
        );
    }

    public static String toStr(OrderCashback orderCashback) {
        if (null == orderCashback) {
            return "null";
        }
        return MoreObjects.toStringHelper(orderCashback)
                .add("cartId", orderCashback.getCartId())
                .add("orderId", orderCashback.getOrderId())
                .add("cashback", CashbackOptionsMatcher.toStr(orderCashback.getCashback()))
                .add("orderItems", orderCashback.getCartId())
                .add("orderItems", ApiMatchers.collectionToStr(orderCashback.getOrderItems(),
                        CashbackOrderItemMatcher::toStr))

                .toString();
    }
}
