package ru.yandex.market.checkout.checkouter.promo.bundles.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentBooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider.OrderItemBuilder;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.common.report.model.FoundOffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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

public class BlueGiftsCheckoutControllerCartTest extends AbstractWebTestBase {

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

        MultiCart multiCart = orderCreateHelper.cart(fbyRequestFor(cart, reportOffers, config ->
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

        assertThat(multiCart.getCarts().get(0).getItems(), hasItems(
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
    public void shouldBeNoChangesOnValidBundles() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle(PROMO_BUNDLE))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle(PROMO_BUNDLE))
        );

        MultiCart multiCart = orderCreateHelper.cart(fbyRequestFor(cart, reportOffers, config ->
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

        assertThat(multiCart, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", nullValue()));
        assertThat(firstOrder(multiCart).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("label", is("some-id-1")),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("relatedItemLabel", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", nullValue())
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("label", is("some-id-2")),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("relatedItemLabel", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", nullValue()),
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
    public void shouldConstructBundleWithoutBundleId() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(primaryOffer)
                .itemBuilder(secondaryOffer)
        );

        MultiCart multiCart = orderCreateHelper.cart(fbyRequestFor(cart, reportOffers, config ->
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

        assertThat(multiCart, hasProperty("valid", is(false)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", hasItem(
                hasProperty("code", equalTo("PROMO_BUNDLE_CART_CHANGE"))
        )));
        assertThat(firstOrder(multiCart).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("label", is("some-id-1")),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("relatedItemLabel", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItem(BUNDLE_NEW))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("label", is("some-id-2")),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("relatedItemLabel", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItem(BUNDLE_NEW)),
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
    public void shouldConstructBundleWithRandomBundleId() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle("frontend random promo bundle"))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle("frontend random promo bundle"))
        );

        MultiCart multiCart = orderCreateHelper.cart(fbyRequestFor(cart, reportOffers, config ->
                config.expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId("frontend random promo bundle")
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

        assertThat(multiCart, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("label", is("some-id-1")),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("relatedItemLabel", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItem(BUNDLE_ID))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("label", is("some-id-2")),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("relatedItemLabel", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItem(BUNDLE_ID)),
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
    public void shouldSplitItemsOnBundleComplexityViolation() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .count(2)
                        .promoBundle("frontend random promo bundle"))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle("frontend random promo bundle"))
        );

        MultiCart multiCart = orderCreateHelper.cart(fbyRequestFor(cart, reportOffers, config ->
                config.expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId("frontend random promo bundle")
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

        assertThat(multiCart, hasProperty("valid", is(false)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", hasItem(
                hasProperty("code", equalTo("PROMO_BUNDLE_CART_CHANGE"))
        )));
        assertThat(firstOrder(multiCart).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("label", is("some-id-1")),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("relatedItemLabel", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItems(BUNDLE_ID, BUNDLE_SPLIT))
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", is(PROMO_BUNDLE)),
                        hasProperty("label", is("some-id-2")),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("relatedItemLabel", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItem(BUNDLE_ID)),
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
                        hasProperty("label", startsWith(LABEL_PREFIX)),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("relatedItemLabel", is("some-id-1")),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItems(BUNDLE_SPLIT))
                )
        ));
    }

    @Test
    public void shouldDestroyExpiredBundle() {
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

        MultiCart multiCart = orderCreateHelper.cart(fbyRequestFor(cart, reportOffers, config ->
                config.expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId("frontend random promo bundle")
                        .destroyReason(ERROR))
                        .expectResponseItems(
                                itemResponseFor(primaryOffer)
                                        .quantity(2),
                                itemResponseFor(secondaryOffer)
                                        .quantity(4)
                        )));

        assertThat(multiCart, hasProperty("valid", is(false)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", hasItem(
                hasProperty("code", equalTo("PROMO_BUNDLE_CART_CHANGE"))
        )));
        assertThat(firstOrder(multiCart), allOf(
                hasProperty("removedByRegroupingItems", hasItem("some-id-2")),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("offerId", is(PRIMARY_OFFER)),
                                hasProperty("bundleId", nullValue()),
                                hasProperty("label", is("some-id-1")),
                                hasProperty("primaryInBundle", nullValue()),
                                hasProperty("relatedItemLabel", nullValue()),
                                hasProperty("count", comparesEqualTo(2)),
                                hasProperty("changes", hasItem(BUNDLE_REMOVED))
                        ),
                        allOf(
                                hasProperty("offerId", is(GIFT_OFFER)),
                                hasProperty("bundleId", nullValue()),
                                hasProperty("label", is("some-id-3")),
                                hasProperty("primaryInBundle", nullValue()),
                                hasProperty("relatedItemLabel", nullValue()),
                                hasProperty("count", comparesEqualTo(4)),
                                hasProperty("changes", hasItems(BUNDLE_REMOVED, BUNDLE_JOIN))
                        )
                ))
        ));
    }

    @Test
    public void shouldCreateBundlesWithVariants() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .count(4))
                .itemBuilder(similar(secondaryOffer)
                        .count(2))
                .itemBuilder(similar(secondaryOffer)
                        .label("some-id-3")
                        .offer("another gift offer")
                        .count(2))
        );

        reportOffers.add(offerOf(similar(secondaryOffer)
                .offer("another gift offer")
        ).build());

        MultiCart multiCart = orderCreateHelper.cart(fbyRequestFor(cart, reportOffers, config ->
                config
                        .expectPromoBundle(OrderBundleBuilder.create()
                                .bundleId("first variant bundle")
                                .promo(PROMO_KEY)
                                .item(similar(primaryOffer).primaryInBundle(true), 1)
                                .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999)
                        )
                        .expectPromoBundle(OrderBundleBuilder.create()
                                .bundleId("second variant bundle")
                                .promo(PROMO_KEY)
                                .item(similar(primaryOffer).primaryInBundle(true), 1)
                                .item("another gift offer", false, 1, 1999)
                        )
                        .expectResponseItems(
                                itemResponseFor(primaryOffer)
                                        .bundleId("first variant bundle")
                                        .primaryInBundle(true)
                                        .quantity(2),
                                itemResponseFor(secondaryOffer)
                                        .bundleId("first variant bundle")
                                        .primaryInBundle(false)
                                        .quantity(2),
                                itemResponseFor(primaryOffer)
                                        .bundleId("second variant bundle")
                                        .primaryInBundle(true)
                                        .quantity(2),
                                itemResponseFor(secondaryOffer)
                                        .primaryInBundle(false)
                                        .offer("another gift offer", "second variant bundle")
                                        .quantity(2)
                        )));

        assertThat(multiCart, hasProperty("valid", is(false)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", hasItem(
                hasProperty("code", equalTo("PROMO_BUNDLE_CART_CHANGE"))
        )));
        assertThat(firstOrder(multiCart), allOf(
                hasProperty("removedByRegroupingItems", empty()),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("offerId", is(PRIMARY_OFFER)),
                                hasProperty("bundleId", is("first variant bundle")),
                                hasProperty("label", is("some-id-1")),
                                hasProperty("primaryInBundle", is(true)),
                                hasProperty("relatedItemLabel", nullValue()),
                                hasProperty("count", comparesEqualTo(2)),
                                hasProperty("changes", hasItems(BUNDLE_SPLIT, BUNDLE_NEW))
                        ),
                        allOf(
                                hasProperty("offerId", is(GIFT_OFFER)),
                                hasProperty("bundleId", is("first variant bundle")),
                                hasProperty("label", is("some-id-2")),
                                hasProperty("primaryInBundle", is(false)),
                                hasProperty("relatedItemLabel", nullValue()),
                                hasProperty("count", comparesEqualTo(2)),
                                hasProperty("changes", hasItem(BUNDLE_NEW)),
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
                                hasProperty("bundleId", is("second variant bundle")),
                                hasProperty("label", startsWith(LABEL_PREFIX)),
                                hasProperty("primaryInBundle", is(true)),
                                hasProperty("relatedItemLabel", is("some-id-1")),
                                hasProperty("count", comparesEqualTo(2)),
                                hasProperty("changes", hasItems(BUNDLE_SPLIT, BUNDLE_NEW))
                        ),
                        allOf(
                                hasProperty("offerId", is("another gift offer")),
                                hasProperty("bundleId", is("second variant bundle")),
                                hasProperty("label", is("some-id-3")),
                                hasProperty("primaryInBundle", is(false)),
                                hasProperty("relatedItemLabel", nullValue()),
                                hasProperty("count", comparesEqualTo(2)),
                                hasProperty("changes", hasItem(BUNDLE_NEW)),
                                hasProperty("promos", hasItem(allOf(
                                        hasProperty("promoDefinition", allOf(
                                                hasProperty("type", is(GENERIC_BUNDLE)),
                                                hasProperty("marketPromoId", is(PROMO_KEY))
                                        )),
                                        hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(1999)))
                                )))
                        )
                ))
        ));
    }

    @Test
    public void shouldDestroyExpiredBundleWithVariants() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle("first variant bundle")
                        .count(2))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle("first variant bundle")
                        .count(2))
                .itemBuilder(similar(primaryOffer)
                        .label("some-id-3")
                        .promoBundle("second variant bundle"))
                .itemBuilder(similar(secondaryOffer)
                        .label("some-id-4")
                        .offer("another gift offer")
                        .promoBundle("second variant bundle"))
                .itemBuilder(similar(secondaryOffer)
                        .label("some-id-5")
                        .offer("another gift offer"))
        );

        reportOffers.add(offerOf(similar(secondaryOffer)
                .offer("another gift offer")
        ).build());

        MultiCart multiCart = orderCreateHelper.cart(fbyRequestFor(cart, reportOffers, config ->
                config.expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId("first variant bundle")
                        .promo(PROMO_KEY)
                        .destroyReason(ERROR))
                        .expectPromoBundle(OrderBundleBuilder.create()
                                .bundleId("second variant bundle")
                                .promo(PROMO_KEY)
                                .item(similar(primaryOffer).primaryInBundle(true), 1)
                                .item("another gift offer", false, 1, 1999)
                        )
                        .expectResponseItems(
                                itemResponseFor(primaryOffer)
                                        .bundleId("second variant bundle")
                                        .primaryInBundle(true)
                                        .quantity(2),
                                itemResponseFor(secondaryOffer)
                                        .offer("another gift offer", "second variant bundle")
                                        .primaryInBundle(false)
                                        .quantity(2),
                                itemResponseFor(primaryOffer),
                                itemResponseFor(secondaryOffer)
                                        .quantity(2)
                        )));

        assertThat(multiCart, hasProperty("valid", is(false)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", hasItem(
                hasProperty("code", is("PROMO_BUNDLE_CART_CHANGE"))
        )));

        assertThat(firstOrder(multiCart), allOf(
                hasProperty("removedByRegroupingItems", hasItem("some-id-5")),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("offerId", is(PRIMARY_OFFER)),
                                hasProperty("bundleId", is("second variant bundle")),
                                hasProperty("label", is("some-id-3")),
                                hasProperty("primaryInBundle", is(true)),
                                hasProperty("relatedItemLabel", nullValue()),
                                hasProperty("count", comparesEqualTo(2)),
                                hasProperty("changes", hasItem(BUNDLE_JOIN))
                        ),
                        allOf(
                                hasProperty("offerId", is("another gift offer")),
                                hasProperty("bundleId", is("second variant bundle")),
                                hasProperty("label", is("some-id-4")),
                                hasProperty("primaryInBundle", is(false)),
                                hasProperty("relatedItemLabel", nullValue()),
                                hasProperty("count", comparesEqualTo(2)),
                                hasProperty("changes", hasItem(BUNDLE_JOIN)),
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
                                hasProperty("label", is("some-id-1")),
                                hasProperty("primaryInBundle", nullValue()),
                                hasProperty("relatedItemLabel", nullValue()),
                                hasProperty("count", comparesEqualTo(1)),
                                hasProperty("changes", hasItems(BUNDLE_REMOVED, BUNDLE_SPLIT))
                        ),
                        allOf(
                                hasProperty("offerId", is(GIFT_OFFER)),
                                hasProperty("bundleId", nullValue()),
                                hasProperty("label", is("some-id-2")),
                                hasProperty("primaryInBundle", nullValue()),
                                hasProperty("relatedItemLabel", nullValue()),
                                hasProperty("count", comparesEqualTo(2)),
                                hasProperty("changes", hasItems(BUNDLE_REMOVED))
                        )
                ))
        ));
    }

    @Test
    public void shouldNotSendDestroyChangesWithIdChange() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle("first variant bundle"))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle("first variant bundle"))
                .itemBuilder(similar(primaryOffer)
                        .promoBundle("second variant bundle"))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle("second variant bundle"))
        );

        Order order = firstOrder(orderCreateHelper.cart(fbyRequestFor(cart, reportOffers, config ->
                config
                        .expectDestroyedPromoBundle(OrderBundleBuilder.create()
                                .bundleId("first variant bundle")
                                .promo(PROMO_KEY)
                                .destroyReason(NEW_VERSION))
                        .expectDestroyedPromoBundle(OrderBundleBuilder.create()
                                .bundleId("second variant bundle")
                                .promo(PROMO_KEY)
                                .destroyReason(NEW_VERSION))
                        .expectPromoBundle(OrderBundleBuilder.create()
                                .bundleId("new variant bundle")
                                .promo(PROMO_KEY)
                                .item(similar(primaryOffer).primaryInBundle(true), 1)
                                .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                        .expectResponseItems(
                                itemResponseFor(primaryOffer)
                                        .bundleId("new variant bundle")
                                        .primaryInBundle(true)
                                        .quantity(2),
                                itemResponseFor(secondaryOffer)
                                        .bundleId("new variant bundle")
                                        .primaryInBundle(false)
                                        .quantity(2)
                        ))));

        assertThat(order.getItems(), hasSize(2));
        assertThat(order.getItems(), hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("changes", not(hasItem(BUNDLE_REMOVED)))
                ),
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("changes", not(hasItem(BUNDLE_REMOVED)))
                )
        ));
    }

    @Test
    public void shouldDestroyBundleWhenLoyaltyNotAvailable() {
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle(PROMO_BUNDLE))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle(PROMO_BUNDLE))
        );
        Parameters parameters = fbyRequestFor(cart, reportOffers, config ->
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
                        ));

        checkouterFeatureWriter.writeValue(PermanentBooleanFeatureType.SKIP_DISCOUNT_CALCULATION_ENABLED, true);
        parameters.setSkipDiscountCalculation(true);
        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", is(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("label", is("some-id-1")),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", is(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("label", is("some-id-2")),
                        hasProperty("primaryInBundle", nullValue()),
                        hasProperty("relatedItemLabel", nullValue())
                )))
        );
    }
}
