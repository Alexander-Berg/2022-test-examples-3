package ru.yandex.market.loyalty.core.utils;

import ru.yandex.market.loyalty.api.model.bundle.OrderBundle;
import ru.yandex.market.loyalty.api.model.bundle.OrderBundleItem;

import java.util.HashSet;
import java.util.Set;

import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.customize;

public class OrderBundleBuilder implements Builder<OrderBundle> {
    private String bundleId;
    private String distributorPromoId;
    private String promoKey;
    private Set<OrderBundleItem> items = new HashSet<>();
    private Long quantity = 1L;
    private boolean restrictReturn = true;

    public static BuildCustomizer<OrderBundle, OrderBundleBuilder> bundleId(String bundleId) {
        return b -> b.bundleId = bundleId;
    }

    public static BuildCustomizer<OrderBundle, OrderBundleBuilder> promoKey(String promoKey) {
        return b -> b.promoKey = promoKey;
    }

    public static BuildCustomizer<OrderBundle, OrderBundleBuilder> distributorPromoId(String distributorPromoId) {
        return b -> b.distributorPromoId = distributorPromoId;
    }

    public static BuildCustomizer<OrderBundle, OrderBundleBuilder> quantity(long quantity) {
        return b -> b.quantity = quantity;
    }

    public static BuildCustomizer<OrderBundle, OrderBundleBuilder> restrictReturn() {
        return b -> b.restrictReturn = true;
    }

    @SafeVarargs
    public static BuildCustomizer<OrderBundle, OrderBundleBuilder> item(
            BuildCustomizer<OrderBundleItem, OrderBundleItemBuilder>... customizers
    ) {
        return b -> b.items.add(customize(OrderBundleItemBuilder::new, customizers).build());
    }

    @Override
    public OrderBundle build() {
        return new OrderBundle(
                bundleId,
                promoKey,
                distributorPromoId,
                quantity,
                items,
                restrictReturn
        );
    }
}
