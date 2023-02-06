package ru.yandex.market.loyalty.core.utils;

import ru.yandex.market.loyalty.api.model.bundle.OrderBundleItem;

public class OrderBundleItemBuilder implements Builder<OrderBundleItem> {
    private String offerId;
    private Long feedId;
    private Integer countInBundle = 1;
    private boolean primaryInBundle = false;

    public static BuildCustomizer<OrderBundleItem, OrderBundleItemBuilder> key(long feedId, String offerId) {
        return b -> {
            b.feedId = feedId;
            b.offerId = offerId;
        };
    }

    public static BuildCustomizer<OrderBundleItem, OrderBundleItemBuilder> countInBundle(int countInBundle) {
        return b -> b.countInBundle = countInBundle;
    }

    public static BuildCustomizer<OrderBundleItem, OrderBundleItemBuilder> primary() {
        return b -> b.primaryInBundle = true;
    }

    @Override
    public OrderBundleItem build() {
        return new OrderBundleItem(
                offerId,
                feedId,
                countInBundle,
                primaryInBundle
        );
    }
}
