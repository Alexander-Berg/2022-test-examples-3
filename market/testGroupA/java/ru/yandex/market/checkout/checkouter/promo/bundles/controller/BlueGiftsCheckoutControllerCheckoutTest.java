package ru.yandex.market.checkout.checkouter.promo.bundles.controller;

import java.math.BigDecimal;
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
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.common.report.model.FoundOffer;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.BUNDLE_ID;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.BUNDLE_JOIN;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.BUNDLE_NEW;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.BUNDLE_REMOVED;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.BUNDLE_SPLIT;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.GENERIC_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.BundleItemsJoiner.LABEL_PREFIX;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.fbyRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.offerItemKey;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.offerOf;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;
import static ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason.ReasonType.ERROR;
import static ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason.ReasonType.NEW_VERSION;

public class BlueGiftsCheckoutControllerCheckoutTest extends AbstractWebTestBase {

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
    public void shouldCreateOrderWithOrderedItems() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(orderItemWithSortingCenter()
                        .offer("offer 1")
                        .price(123))
                .itemBuilder(similar(primaryOffer)
                        .promoBundle(PROMO_BUNDLE))
                .itemBuilder(orderItemWithSortingCenter()
                        .offer("offer 2")
                        .price(123))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle(PROMO_BUNDLE))
                .itemBuilder(orderItemWithSortingCenter()
                        .offer("offer 3")
                        .price(123))
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(fbyRequestFor(cart, reportOffers, config ->
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
                                        .primaryInBundle(false)
                        )));

        assertThat(multiOrder.getCarts().get(0).getItems(), hasItems(
                hasProperty("offerItemKey", is(offerItemKey("offer 1"))),
                allOf(
                        hasProperty("offerItemKey", is(offerItemKey(PRIMARY_OFFER, PROMO_BUNDLE))),
                        hasProperty("primaryInBundle", is(true))
                ),
                allOf(
                        hasProperty("offerItemKey", is(offerItemKey(GIFT_OFFER, PROMO_BUNDLE))),
                        hasProperty("primaryInBundle", is(false))
                ),
                hasProperty("offerItemKey", is(offerItemKey("offer 2"))),
                hasProperty("offerItemKey", is(offerItemKey("offer 3")))
        ));
    }

    @Test
    public void shouldCheckoutOnValidBundles() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle(PROMO_BUNDLE))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle(PROMO_BUNDLE))
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(fbyRequestFor(cart, reportOffers, config ->
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
                                        .primaryInBundle(false)
                        )));

        assertThat(multiOrder, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiOrder).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("changes", nullValue()),
                        hasProperty("label", is("some-id-1"))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("changes", nullValue()),
                        hasProperty("label", is("some-id-2")),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(GENERIC_BUNDLE)),
                                        hasProperty("marketPromoId", is(PROMO_KEY))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1999)))
                        )))
                )
        ));
    }

    @Test
    public void shouldCheckoutOnBundleIdChange() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle("some another id"))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle("some another id"))
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(fbyRequestFor(cart, reportOffers, config ->
                config.expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId("some another id")
                        .destroyReason(NEW_VERSION))
                        .expectPromoBundle(OrderBundleBuilder.create()
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
                                        .primaryInBundle(false)
                        )));

        assertThat(multiOrder, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiOrder).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("changes", nullValue()),
                        hasProperty("label", is("some-id-1"))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("changes", nullValue()),
                        hasProperty("label", is("some-id-2")),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(GENERIC_BUNDLE)),
                                        hasProperty("marketPromoId", is(PROMO_KEY))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1999)))
                        )))
                )
        ));
    }

    @Test
    public void shouldNotCheckoutOnNewBundle() throws Exception {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(primaryOffer)
                .itemBuilder(secondaryOffer)
        );

        MultiOrder multiOrder = checkout(fbyRequestFor(cart, reportOffers, config ->
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
                                        .primaryInBundle(false)
                        )));

        assertThat(multiOrder, hasProperty("valid", is(false)));
        assertThat(firstOrder(multiOrder), hasProperty("validationErrors", hasItem(
                hasProperty("code", equalTo("PROMO_BUNDLE_CART_CHANGE"))
        )));
        assertThat(firstOrder(multiOrder).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("changes", hasItems(BUNDLE_NEW)),
                        hasProperty("label", is("some-id-1")),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("changes", hasItems(BUNDLE_NEW)),
                        hasProperty("label", is("some-id-2")),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("relatedItemLabel", nullValue()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(GENERIC_BUNDLE)),
                                        hasProperty("marketPromoId", is(PROMO_KEY))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1999)))
                        )))
                )
        ));
    }

    @Test
    public void shouldNotCheckoutOnBundleRemove() throws Exception {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle(PROMO_BUNDLE))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle(PROMO_BUNDLE))
        );

        MultiOrder multiOrder = checkout(fbyRequestFor(cart, reportOffers, config ->
                config.expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .destroyReason(ERROR)
                )));

        assertThat(multiOrder, hasProperty("valid", is(false)));
        assertThat(firstOrder(multiOrder), hasProperty("validationErrors", hasItem(
                hasProperty("code", equalTo("PROMO_BUNDLE_CART_CHANGE"))
        )));
        assertThat(firstOrder(multiOrder).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", hasItems(BUNDLE_REMOVED)),
                        hasProperty("label", is("some-id-1")),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", hasItem(BUNDLE_REMOVED)),
                        hasProperty("label", is("some-id-2")),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("relatedItemLabel", nullValue()),
                        hasProperty("promos", empty())
                )
        ));
    }

    @Test
    public void shouldNotCheckoutOnBundleSplit() throws Exception {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle("frontend random promo bundle")
                        .count(2))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle("frontend random promo bundle"))
        );

        MultiOrder multiOrder = checkout(fbyRequestFor(cart, reportOffers, config ->
                config.expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId(md5Hex("frontend random promo bundle"))
                        .destroyReason(NEW_VERSION))
                        .expectPromoBundle(OrderBundleBuilder.create()
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

        assertThat(multiOrder, hasProperty("valid", is(false)));
        assertThat(firstOrder(multiOrder), hasProperty("validationErrors", hasItem(
                hasProperty("code", equalTo("PROMO_BUNDLE_CART_CHANGE"))
        )));

        assertThat(firstOrder(multiOrder).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItems(BUNDLE_ID, BUNDLE_SPLIT)),
                        hasProperty("label", is("some-id-1")),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItems(BUNDLE_ID)),
                        hasProperty("label", is("some-id-2")),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("relatedItemLabel", nullValue()),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", allOf(
                                        hasProperty("type", is(GENERIC_BUNDLE)),
                                        hasProperty("marketPromoId", is(PROMO_KEY))
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1999)))
                        )))
                ),
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItems(BUNDLE_SPLIT)),
                        hasProperty("label", startsWith(LABEL_PREFIX)),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("relatedItemLabel", is("some-id-1"))
                )
        ));
    }

    @Test
    public void shouldDestroyExpiredBundle() throws Exception {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle("frontend random promo bundle")
                        .count(2))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle("frontend random promo bundle")
                        .count(2))
                .itemBuilder(similar(secondaryOffer)
                        .label("some-id-3")
                        .count(2))
        );

        MultiOrder multiOrder = checkout(fbyRequestFor(cart, reportOffers, config ->
                config.expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId(md5Hex("frontend random promo bundle"))
                        .destroyReason(ERROR))
                        .expectResponseItems(
                                itemResponseFor(primaryOffer)
                                        .quantity(2),
                                itemResponseFor(secondaryOffer)
                                        .quantity(4)
                        )));

        assertThat(multiOrder, hasProperty("valid", is(false)));
        assertThat(firstOrder(multiOrder), hasProperty("validationErrors", hasItem(
                hasProperty("code", equalTo("PROMO_BUNDLE_CART_CHANGE"))
        )));
        assertThat(firstOrder(multiOrder), allOf(
                hasProperty("removedByRegroupingItems", hasItem("some-id-2")),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("offerId", is(PRIMARY_OFFER)),
                                hasProperty("bundleId", nullValue()),
                                hasProperty("count", comparesEqualTo(2)),
                                hasProperty("changes", hasItem(BUNDLE_REMOVED)),
                                hasProperty("label", is("some-id-1")),
                                hasProperty("primaryInBundle", nullValue()),
                                hasProperty("relatedItemLabel", nullValue())
                        ),
                        allOf(
                                hasProperty("offerId", is(GIFT_OFFER)),
                                hasProperty("bundleId", nullValue()),
                                hasProperty("count", comparesEqualTo(4)),
                                hasProperty("changes", hasItems(BUNDLE_REMOVED, BUNDLE_JOIN)),
                                hasProperty("label", is("some-id-3")),
                                hasProperty("primaryInBundle", nullValue()),
                                hasProperty("relatedItemLabel", nullValue())
                        )
                ))
        ));
    }

    private MultiOrder checkout(Parameters parameters) throws Exception {
        MultiCart patched = orderCreateHelper.cart(parameters);

        firstOrder(patched).setItems(parameters.getOrder().getItems());

        return orderCreateHelper.checkout(patched, parameters);
    }
}
