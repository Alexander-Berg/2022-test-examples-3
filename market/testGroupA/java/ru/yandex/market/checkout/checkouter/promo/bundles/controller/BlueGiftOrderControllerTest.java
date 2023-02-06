package ru.yandex.market.checkout.checkouter.promo.bundles.controller;

import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.fbyRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.offerItemKey;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.providers.MultiCartProvider.single;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class BlueGiftOrderControllerTest extends AbstractWebTestBase {

    @Test
    public void shouldReturnPrimaryInBundleFields() {
        Order savedOrder = createTypicalOrderWithBundles();

        Order order = client.getOrder(savedOrder.getId(), ClientRole.SYSTEM, 0L);

        assertThat(order.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(true))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(false))
                )
        ));
    }

    @Test
    public void shouldReturnReturnRestrictionInPromoFields() {
        Order savedOrder = createTypicalOrderWithBundles(OrderBundleBuilder::restrictReturn);

        Order order = client.getOrder(savedOrder.getId(), ClientRole.SYSTEM, 0L);

        assertThat(order.getPromos(), hasItem(hasProperty("promoDefinition", allOf(
                hasProperty("bundleId", is(PROMO_BUNDLE)),
                hasProperty("type", is(PromoType.GENERIC_BUNDLE)),
                hasProperty("marketPromoId", is(PROMO_KEY)),
                hasProperty("bundleReturnRestrict", is(true))
        ))));
    }

    @Test
    public void shouldReturnOrderedItemsWithGetResponse() {
        Order savedOrder = createTypicalOrderWithBundles(OrderBundleBuilder::restrictReturn);

        Order order = client.getOrder(savedOrder.getId(), ClientRole.SYSTEM, 0L);

        assertThat(order.getItems(), hasItems(
                hasProperty("offerItemKey", is(offerItemKey("offer 1"))),
                hasProperty("offerItemKey", is(offerItemKey(PRIMARY_OFFER, PROMO_BUNDLE))),
                hasProperty("offerItemKey", is(offerItemKey(GIFT_OFFER, PROMO_BUNDLE))),
                hasProperty("offerItemKey", is(offerItemKey(PRIMARY_OFFER))),
                hasProperty("offerItemKey", is(offerItemKey("offer 3"))),
                hasProperty("offerItemKey", is(offerItemKey("offer 4"))),
                hasProperty("offerItemKey", is(offerItemKey("offer 6")))
        ));
    }

    private Order createTypicalOrderWithBundles() {
        return createTypicalOrderWithBundles(b -> {
        });
    }

    private Order createTypicalOrderWithBundles(Consumer<OrderBundleBuilder> bundleBuilderCustomizer) {
        final OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .offer(PRIMARY_OFFER)
                .price(10000);

        final OrderItemProvider.OrderItemBuilder secondaryOffer = orderItemWithSortingCenter()
                .offer(GIFT_OFFER)
                .price(2000);

        final OrderBundleBuilder bundleBuilder = OrderBundleBuilder.create()
                .bundleId(PROMO_BUNDLE)
                .promo(PROMO_KEY)
                .item(similar(primaryOffer).primaryInBundle(true), 1)
                .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999);

        bundleBuilderCustomizer.accept(bundleBuilder);

        return orderCreateHelper.createOrder(fbyRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(orderItemWithSortingCenter()
                        .offer("offer 1")
                        .price(123))
                .itemBuilder(similar(primaryOffer)
                        .count(2))
                .itemBuilder(orderItemWithSortingCenter()
                        .offer("offer 3")
                        .price(123))
                .itemBuilder(orderItemWithSortingCenter()
                        .offer("offer 4")
                        .price(123))
                .itemBuilder(similar(secondaryOffer))
                .itemBuilder(orderItemWithSortingCenter()
                        .offer("offer 6")
                        .price(123))
        ), Arrays.asList(
                FoundOfferBuilder.createFrom(primaryOffer.build())
                        .promoKey(PROMO_KEY)
                        .promoType(ReportPromoType.GENERIC_BUNDLE.getCode())
                        .build(),
                FoundOfferBuilder.createFrom(secondaryOffer.build())
                        .promoKey(PROMO_KEY)
                        .promoType(ReportPromoType.GENERIC_BUNDLE_SECONDARY.getCode())
                        .build()
        ), config ->
                config.expectPromoBundle(bundleBuilder)
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
