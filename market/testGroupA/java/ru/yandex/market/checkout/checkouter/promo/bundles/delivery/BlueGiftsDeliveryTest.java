package ru.yandex.market.checkout.checkouter.promo.bundles.delivery;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.fbyRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.providers.MultiCartProvider.single;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class BlueGiftsDeliveryTest extends AbstractWebTestBase {

    @Test
    public void shouldCreateParcelsForOrderWithBundle() {
        Order order = createTypicalOrderWithBundles();

        Set<Long> orderItemIds = order.getItems().stream()
                .map(OrderItem::getId)
                .collect(Collectors.toSet());

        assertThat(order.getRgb(), equalTo(Color.BLUE));
        assertThat(order.getDelivery(), notNullValue());
        assertThat(order.getDelivery().getParcels(), notNullValue());
        assertThat(order.getDelivery().getParcels(), hasSize(1));
        assertThat(order.getDelivery().getParcels(), hasItem(
                allOf(
                        hasProperty("parcelItems", hasSize(3)),
                        hasProperty("parcelItems", everyItem(
                                hasProperty("itemId", isIn(orderItemIds))
                        ))
                )
        ));
    }

    private Order createTypicalOrderWithBundles() {
        OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .label("some-id-1")
                .offer(PRIMARY_OFFER)
                .price(10000);

        OrderItemProvider.OrderItemBuilder secondaryOffer = orderItemWithSortingCenter()
                .label("some-id-2")
                .offer(GIFT_OFFER)
                .price(2000);

        return orderCreateHelper.createOrder(fbyRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .count(2))
                .itemBuilder(secondaryOffer)
        ), PROMO_KEY, config ->
                config.expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                        .expectResponseItems(
                                itemResponseFor(primaryOffer)
                                        .bundleId(PROMO_BUNDLE)
                                        .primaryInBundle(true),
                                itemResponseFor(secondaryOffer)
                                        .bundleId(PROMO_BUNDLE)
                                        .primaryInBundle(false),
                                itemResponseFor(primaryOffer)
                        )));
    }
}
