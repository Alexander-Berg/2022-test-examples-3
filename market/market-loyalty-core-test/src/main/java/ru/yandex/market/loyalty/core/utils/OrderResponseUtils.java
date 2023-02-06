package ru.yandex.market.loyalty.core.utils;

import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.loyalty.api.model.AbstractOrder;
import ru.yandex.market.loyalty.api.model.OrderItemResponse;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.core.model.order.ItemKey;

import java.util.Map;
import java.util.function.Predicate;

public class OrderResponseUtils {

    public static OrderWithBundlesResponse firstOrderOf(MultiCartWithBundlesDiscountResponse response) {
        return response.getOrders().iterator().next();
    }

    public static <T extends OrderItemResponse> T itemResponseOf(AbstractOrder<T> order, Predicate<T> predicate) {
        return order.getItems().stream()
                .filter(predicate)
                .findFirst().orElse(null);
    }

    public static <T extends OrderItemResponse> Predicate<T> withKeyOf(long feedId, String offerId) {
        return item -> item.getFeedId().equals(feedId) &&
                offerId.equals(item.getOfferId());
    }

    public static Map<ItemKey, OrderItemResponse> itemsAsMap(AbstractOrder<OrderItemResponse> orderResponse) {
        return orderResponse.getItems().stream()
                .map(item -> Pair.of(ItemKey.fromOrderItem(item, ItemKey.SINGLE_CART_ID, null, null), item))
                .collect(CoreCollectionUtils.toMap());
    }
}
