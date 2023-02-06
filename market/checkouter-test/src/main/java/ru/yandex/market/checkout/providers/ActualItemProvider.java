package ru.yandex.market.checkout.providers;

import java.util.Random;

import ru.yandex.market.checkout.checkouter.actual.ActualItem;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

public abstract class ActualItemProvider {

    private static final Random RANDOM = new Random(1337L);

    private ActualItemProvider() {
        throw new UnsupportedOperationException();
    }

    public static ActualItem buildActualItem() {
        ActualItem actualItem = new ActualItem();
        actualItem.setFeedId(OrderItemProvider.FEED_ID);
        actualItem.setOfferId(String.valueOf(Math.abs(RANDOM.nextLong())));
        actualItem.setShopId(OrderProvider.SHOP_ID);
        actualItem.setBuyerRegionId(213L);
        actualItem.setRgb(Color.BLUE);
        return actualItem;
    }

    public static ActualItem buildActualItem(OrderItem item, long shopId, long buyerRegionId, Color color) {
        ActualItem actualItem = new ActualItem();
        actualItem.setFeedId(item.getFeedId());
        actualItem.setOfferId(item.getOfferId());
        actualItem.setShopId(shopId);
        actualItem.setBuyerRegionId(buyerRegionId);
        actualItem.setRgb(color);
        return actualItem;
    }
}
