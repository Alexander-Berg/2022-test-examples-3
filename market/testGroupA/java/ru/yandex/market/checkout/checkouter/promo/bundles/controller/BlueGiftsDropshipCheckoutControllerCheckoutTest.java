package ru.yandex.market.checkout.checkouter.promo.bundles.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider.OrderItemBuilder;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.common.report.model.FoundOffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.dropshipRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.offerItemKey;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.offerOf;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class BlueGiftsDropshipCheckoutControllerCheckoutTest extends AbstractWebTestBase {

    private OrderItemBuilder primaryOffer;
    private OrderItemBuilder secondaryOffer;
    private final List<FoundOffer> reportOffers = new ArrayList<>();

    @BeforeEach
    public void configure() {
        primaryOffer = orderItemWithSortingCenter()
                .label("some-id-1")
                .offer(PRIMARY_OFFER)
                .price(10000);

        secondaryOffer = orderItemWithSortingCenter()
                .label("some-id-2")
                .offer(GIFT_OFFER)
                .price(2000);

        //report should return promo key for primary item
        reportOffers.add(offerOf(primaryOffer)
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.GENERIC_BUNDLE.getCode())
                .build());
        reportOffers.add(offerOf(secondaryOffer)
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.GENERIC_BUNDLE_SECONDARY.getCode())
                .build());
    }

    @Test
    public void shouldCreateDropshipOrderWithBundles() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle(PROMO_BUNDLE))
                .itemBuilder(similar(primaryOffer))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle(PROMO_BUNDLE))
        );

        Parameters parameters = dropshipRequestFor(cart, reportOffers,
                ShopSettingsHelper::getDefaultMeta, config ->
                        config.expectPromoBundle(OrderBundleBuilder.create()
                                .bundleId(PROMO_BUNDLE)
                                .promo(PROMO_KEY)
                                .item(similar(primaryOffer).primaryInBundle(true), 1)
                                .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                                .expectResponseItems(
                                        itemResponseFor(primaryOffer)
                                                .bundleId(PROMO_BUNDLE)
                                                .primaryInBundle(true),
                                        itemResponseFor(primaryOffer),
                                        itemResponseFor(secondaryOffer)
                                                .bundleId(PROMO_BUNDLE)
                                                .primaryInBundle(false)
                                ));

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);


        assertThat(multiOrder.getOrders().get(0).getItems(), hasItems(
                allOf(
                        hasProperty("offerItemKey", is(offerItemKey(PRIMARY_OFFER, PROMO_BUNDLE))),
                        hasProperty("count", comparesEqualTo(1))
                ),
                allOf(
                        hasProperty("offerItemKey", is(offerItemKey(PRIMARY_OFFER))),
                        hasProperty("count", comparesEqualTo(1))
                ),
                allOf(
                        hasProperty("offerItemKey", is(offerItemKey(GIFT_OFFER, PROMO_BUNDLE))),
                        hasProperty("count", comparesEqualTo(1))
                )
        ));
    }
}
