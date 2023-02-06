package ru.yandex.market.checkout.checkouter.promo.bundles.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyService;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider.OrderItemBuilder;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.checkout.util.loyalty.response.OrderItemResponseBuilder;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
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
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.LoyaltyTestUtils.createTestContextWithBuilders;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemBuilder;
import static ru.yandex.market.checkout.test.providers.OrderProvider.orderBuilder;
import static ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason.ReasonType.ERROR;
import static ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason.ReasonType.NEW_VERSION;

public class BlueGiftsLoyaltyServiceTest extends AbstractWebTestBase {

    private final List<FoundOfferBuilder> reportOffers = new ArrayList<>();
    @Autowired
    private LoyaltyService loyaltyService;
    private OrderItemBuilder primaryOffer;
    private OrderItemBuilder secondaryOffer;

    @BeforeEach
    public void configure() {
        primaryOffer = orderItemBuilder()
                .offer(PRIMARY_OFFER)
                .price(10000);

        secondaryOffer = orderItemBuilder()
                .offer(GIFT_OFFER)
                .price(2000);

        //report should return promo key for primary item
        reportOffers.add(FoundOfferBuilder.createFrom(primaryOffer.build())
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.GENERIC_BUNDLE.getCode()));

        reportOffers.add(FoundOfferBuilder.createFrom(secondaryOffer.build())
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.GENERIC_BUNDLE_SECONDARY.getCode()));
    }

    @Test
    public void shouldCheckPromoKeyAndConstructBundles() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(primaryOffer)
                .itemBuilder(secondaryOffer)
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999));

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(true))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("promos", contains(hasProperty("promoDefinition", allOf(
                                hasProperty("type", equalTo(GENERIC_BUNDLE)),
                                hasProperty("marketPromoId", equalTo(PROMO_KEY)),
                                hasProperty("bundleId", equalTo(PROMO_BUNDLE))
                        ))))
                )
        ))));
    }

    @Test
    public void shouldCheckPromoKeyAndDestroyBundles() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(primaryOffer
                        .promoBundle(PROMO_BUNDLE))
                .itemBuilder(secondaryOffer
                        .promoBundle(PROMO_BUNDLE))
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters().expectDestroyedPromoBundle(
                OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .destroyReason(ERROR));

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue())
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("promos", empty())
                )
        ))));
    }

    @Test
    public void shouldSplitItemsOnBundleConstruction() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(similar(primaryOffer).count(3))
                .itemBuilder(secondaryOffer)
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                .expectResponseItems(
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .bundleId(PROMO_BUNDLE)
                                .primaryInBundle(true),
                        OrderItemResponseBuilder.createFrom(secondaryOffer.build())
                                .bundleId(PROMO_BUNDLE)
                                .primaryInBundle(false),
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .quantity(2)
                );

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(2)),
                        hasProperty("changes", hasItems(BUNDLE_NEW, BUNDLE_SPLIT))
                ),
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(2)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_SPLIT))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_NEW))
                )
        ))));
    }

    @Test
    public void shouldJoinItemsOnBundleDestruction() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(similar(primaryOffer).count(2))
                .itemBuilder(similar(primaryOffer)
                        .promoBundle(PROMO_BUNDLE))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle(PROMO_BUNDLE))
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .destroyReason(NEW_VERSION))
                .expectResponseItems(
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .quantity(3),
                        OrderItemResponseBuilder.createFrom(secondaryOffer.build())
                );

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(3)),
                        hasProperty("changes", hasSize(2)),
                        hasProperty("changes", hasItems(BUNDLE_REMOVED, BUNDLE_JOIN))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_REMOVED))
                )
        ))));
    }

    @Test
    public void shouldAddChangesOnBundleConstruction() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(primaryOffer)
                .itemBuilder(secondaryOffer)
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999));

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_NEW))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_NEW))
                )
        ))));
    }

    @Test
    public void shouldAddChangesOnBundlesVariantAdd() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle("some random promo bundle"))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle("some random promo bundle"))
                .itemBuilder(similar(primaryOffer)
                        .count(2))
                .itemBuilder(similar(primaryOffer)
                        .offer("another gift offer"))
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId(md5Hex("some random promo bundle"))
                        .destroyReason(NEW_VERSION))
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(md5Hex("some version bundle id"))
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(md5Hex("another version bundle id"))
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item("another gift offer", false, 1, 1999))
                .expectResponseItems(
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .bundleId(md5Hex("some version bundle id"))
                                .primaryInBundle(true)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(secondaryOffer.build())
                                .bundleId(md5Hex("some version bundle id"))
                                .primaryInBundle(false)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .bundleId(md5Hex("another version bundle id"))
                                .primaryInBundle(true)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(secondaryOffer.build())
                                .offer("another gift offer", md5Hex("another version bundle id"))
                                .primaryInBundle(false)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .quantity(1)
                );

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_SPLIT))
                ),
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(md5Hex("some version bundle id"))),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(2)),
                        hasProperty("changes", hasItems(BUNDLE_ID, BUNDLE_SPLIT))
                ),
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(md5Hex("another version bundle id"))),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(2)),
                        hasProperty("changes", hasItems(BUNDLE_NEW, BUNDLE_SPLIT))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(md5Hex("some version bundle id"))),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_ID))
                ),
                allOf(
                        hasProperty("offerId", equalTo("another gift offer")),
                        hasProperty("bundleId", equalTo(md5Hex("another version bundle id"))),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_NEW))
                )
        ))));
    }

    @Test
    public void shouldAddChangesOnBundleDestruction() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle(PROMO_BUNDLE))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle(PROMO_BUNDLE))
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .destroyReason(ERROR));

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_REMOVED))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_REMOVED))
                )
        ))));
    }

    @Test
    public void shouldAddChangesOnBundleIdChange() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle(md5Hex("some old bundle id")))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle(md5Hex("some old bundle id")))
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId(md5Hex("some old bundle id"))
                        .destroyReason(NEW_VERSION))
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999));

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_ID))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_ID))
                )
        ))));
    }

    @Test
    public void shouldNotAddChangeOnNoneChanges() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(primaryOffer
                        .promoBundle("some random promo bundle"))
                .itemBuilder(secondaryOffer
                        .promoBundle("some random promo bundle"))
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId("some random promo bundle")
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999));

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo("some random promo bundle")),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("changes", nullValue())
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo("some random promo bundle")),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("changes", nullValue())
                )
        ))));
    }

    @Test
    public void shouldAddItemParentLinkOnBundleSplit() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(similar(primaryOffer)
                        .label("some frontend generic id - 1")
                        .count(3))
                .itemBuilder(similar(secondaryOffer)
                        .label("some frontend generic id - 2"))
                .itemBuilder(similar(secondaryOffer)
                        .offer("another gift offer")
                        .label("some frontend generic id - 3"))
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(md5Hex("another generic bundle id"))
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item("another gift offer", false, 1, 1999))
                .expectResponseItems(
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .bundleId(PROMO_BUNDLE)
                                .primaryInBundle(true)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(secondaryOffer.build())
                                .bundleId(PROMO_BUNDLE)
                                .primaryInBundle(false)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .bundleId(md5Hex("another generic bundle id"))
                                .primaryInBundle(true)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(secondaryOffer.build())
                                .offer("another gift offer", md5Hex("another generic bundle id"))
                                .primaryInBundle(false)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .quantity(1)
                );

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("label", equalTo("some frontend generic id - 1")),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("label", startsWith(LABEL_PREFIX)),
                        hasProperty("relatedItemLabel", equalTo("some frontend generic id - 1"))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("label", equalTo("some frontend generic id - 2")),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(md5Hex("another generic bundle id"))),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("label", startsWith(LABEL_PREFIX)),
                        hasProperty("relatedItemLabel", equalTo("some frontend generic id - 1"))
                ),
                allOf(
                        hasProperty("offerId", equalTo("another gift offer")),
                        hasProperty("bundleId", equalTo(md5Hex("another generic bundle id"))),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("label", equalTo("some frontend generic id - 3")),
                        hasProperty("relatedItemLabel", nullValue())
                )
        ))));
    }

    @Test
    public void shouldAddItemParentLinkOnBundleSplit2() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(similar(primaryOffer)
                        .label("some frontend generic id - 1")
                        .promoBundle("some random promo bundle"))
                .itemBuilder(similar(secondaryOffer)
                        .label("some frontend generic id - 2")
                        .promoBundle("some random promo bundle"))
                .itemBuilder(similar(primaryOffer)
                        .label("some frontend generic id - 3")
                        .count(2))
                .itemBuilder(similar(secondaryOffer)
                        .offer("another gift offer")
                        .label("some frontend generic id - 4"))
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectDestroyedPromoBundle(OrderBundleBuilder.create()
                        .bundleId(md5Hex("some random promo bundle"))
                        .destroyReason(NEW_VERSION))
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(md5Hex("another generic bundle id"))
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item("another gift offer", false, 1, 1999))
                .expectResponseItems(
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .bundleId(PROMO_BUNDLE)
                                .primaryInBundle(true)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(secondaryOffer.build())
                                .bundleId(PROMO_BUNDLE)
                                .primaryInBundle(false)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .bundleId(md5Hex("another generic bundle id"))
                                .primaryInBundle(true)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(secondaryOffer.build())
                                .offer("another gift offer", md5Hex("another generic bundle id"))
                                .primaryInBundle(false)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .quantity(1)
                );

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("label", equalTo("some frontend generic id - 3")),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("label", equalTo("some frontend generic id - 1")),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("label", equalTo("some frontend generic id - 2")),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(md5Hex("another generic bundle id"))),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("label", startsWith(LABEL_PREFIX)),
                        hasProperty("relatedItemLabel", equalTo("some frontend generic id - 3"))
                ),
                allOf(
                        hasProperty("offerId", equalTo("another gift offer")),
                        hasProperty("bundleId", equalTo(md5Hex("another generic bundle id"))),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("label", equalTo("some frontend generic id - 4")),
                        hasProperty("relatedItemLabel", nullValue())
                )
        ))));
    }

    @Test
    public void shouldAddItemParentLinkOnBundleSplit3() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(similar(primaryOffer)
                        .promoBundle("some random promo bundle")
                        .label("some frontend generic id - 1"))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle("some random promo bundle")
                        .label("some frontend generic id - 2"))
                .itemBuilder(similar(primaryOffer)
                        .label("some frontend generic id - 3")
                        .count(2))
                .itemBuilder(similar(secondaryOffer)
                        .offer("another gift offer")
                        .label("some frontend generic id - 4"))
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(md5Hex("some random promo bundle"))
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                .expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(md5Hex("another generic bundle id"))
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item("another gift offer", false, 1, 1999))
                .expectResponseItems(
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .bundleId(md5Hex("some random promo bundle"))
                                .primaryInBundle(true)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(secondaryOffer.build())
                                .bundleId(md5Hex("some random promo bundle"))
                                .primaryInBundle(false)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .bundleId(md5Hex("another generic bundle id"))
                                .primaryInBundle(true)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(secondaryOffer.build())
                                .offer("another gift offer", md5Hex("another generic bundle id"))
                                .primaryInBundle(false)
                                .quantity(1),
                        OrderItemResponseBuilder.createFrom(primaryOffer.build())
                                .quantity(1)
                );

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(hasProperty("items", hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("label", equalTo("some frontend generic id - 3")),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(md5Hex("some random promo bundle"))),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("label", equalTo("some frontend generic id - 1")),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(md5Hex("some random promo bundle"))),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("label", equalTo("some frontend generic id - 2")),
                        hasProperty("relatedItemLabel", nullValue())
                ),
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(md5Hex("another generic bundle id"))),
                        hasProperty("primaryInBundle", is(true)),
                        hasProperty("label", startsWith(LABEL_PREFIX)),
                        hasProperty("relatedItemLabel", equalTo("some frontend generic id - 3"))
                ),
                allOf(
                        hasProperty("offerId", equalTo("another gift offer")),
                        hasProperty("bundleId", equalTo(md5Hex("another generic bundle id"))),
                        hasProperty("primaryInBundle", is(false)),
                        hasProperty("label", equalTo("some frontend generic id - 4")),
                        hasProperty("relatedItemLabel", nullValue())
                )
        ))));
    }

    @Test
    public void shouldAddItemParentLinkOnBundleDestroy() {
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(similar(primaryOffer)
                        .label("some frontend generic id - 1")
                        .count(2))
                .itemBuilder(similar(primaryOffer)
                        .promoBundle(PROMO_BUNDLE)
                        .label("some frontend generic id - 2"))
                .itemBuilder(similar(secondaryOffer)
                        .promoBundle(PROMO_BUNDLE)
                        .label("some frontend generic id - 3"))
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters().expectDestroyedPromoBundle(
                OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .destroyReason(NEW_VERSION));

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContextWithBuilders(cart, reportOffers));

        assertThat(cart.getCarts(), hasItem(allOf(
                hasProperty("removedByRegroupingItems", hasItem("some frontend generic id - 2")),
                hasProperty("items", hasItems(
                        allOf(
                                hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                                hasProperty("bundleId", nullValue()),
                                hasProperty("label", equalTo("some frontend generic id - 1")),
                                hasProperty("relatedItemLabel", nullValue())
                        ),
                        allOf(
                                hasProperty("offerId", equalTo(GIFT_OFFER)),
                                hasProperty("bundleId", nullValue()),
                                hasProperty("label", equalTo("some frontend generic id - 3")),
                                hasProperty("relatedItemLabel", nullValue())
                        )
                ))
        )));
    }

    private OrderItemBuilder similar(OrderItemBuilder item) {
        return item.clone();
    }
}
