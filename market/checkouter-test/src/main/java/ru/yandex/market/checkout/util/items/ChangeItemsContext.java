package ru.yandex.market.checkout.util.items;

import java.util.Map;
import java.util.Set;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;

public interface ChangeItemsContext {

    Map<OfferItemKey, Integer> getAccumulatedChanges();

    Set<Long> getRefundsBefore();

    Order getOrigOrder();
}
