package ru.yandex.market.checkout.util.items;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.test.providers.OrderItemProvider.OrderItemBuilder;
import ru.yandex.market.checkout.util.loyalty.response.OrderItemResponseBuilder;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static ru.yandex.market.checkout.util.loyalty.response.OrderItemResponseBuilder.createFrom;

public final class OrderItemUtils {

    private OrderItemUtils() {
    }

    @Nonnull
    public static OrderItemBuilder similar(@Nonnull OrderItemBuilder item) {
        return item.clone();
    }

    @Nonnull
    public static FoundOfferBuilder offerOf(@Nonnull OrderItemBuilder itemBuilder) {
        return FoundOfferBuilder.createFrom(itemBuilder.build());
    }

    @Nonnull
    public static FoundOfferBuilder offerOf(@Nonnull OrderItem item) {
        return FoundOfferBuilder.createFrom(item);
    }

    @Nonnull
    public static OrderItemResponseBuilder itemResponseFor(@Nonnull OrderItemBuilder itemBuilder) {
        return itemResponseFor(itemBuilder.build());
    }

    @Nonnull
    public static OrderItemResponseBuilder itemResponseFor(@Nonnull OrderItem item) {
        return createFrom(item).quantity(1);
    }
}
