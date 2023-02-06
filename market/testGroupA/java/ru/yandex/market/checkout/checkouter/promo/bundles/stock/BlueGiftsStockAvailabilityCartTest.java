package ru.yandex.market.checkout.checkouter.promo.bundles.stock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils;
import ru.yandex.market.checkout.checkouter.util.OfferItemUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.OrderItemProvider.OrderItemBuilder;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.BUNDLE_ID;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.BUNDLE_NEW;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.BUNDLE_REMOVED;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.BUNDLE_SPLIT;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.COUNT;
import static ru.yandex.market.checkout.checkouter.cart.ItemChange.MISSING;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.providers.MultiCartProvider.single;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.yandexDelivery;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;
import static ru.yandex.market.loyalty.api.model.bundle.BundleDestroyReason.ReasonType.ERROR;

public class BlueGiftsStockAvailabilityCartTest extends AbstractWebTestBase {

    private OrderItemBuilder primaryOffer;
    private OrderItemBuilder secondaryOffer;
    private final List<SSItemAmount> offerStockAvailability = new ArrayList<>();

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
    }

    @Test
    public void shouldDestroyBundlesOnRequiredItemsMissing() {
        itemStockAvailability(primaryOffer, 1);
        itemStockAvailability(secondaryOffer, 0);

        MultiCart cart = orderCreateHelper.cart(stubRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer).promoBundle("some bundle"))
                .itemBuilder(similar(secondaryOffer).promoBundle("some bundle"))
        ), PROMO_KEY, config ->
                config.expectDestroyedPromoBundle(
                        OrderBundleBuilder.create()
                                .bundleId(md5Hex("some bundle"))
                                .destroyReason(ERROR)
                ).expectResponseItems(
                        itemResponseFor(primaryOffer)
                )));

        assertThat(firstOrder(cart).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_REMOVED))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(0)),
                        hasProperty("changes", hasSize(2)),
                        hasProperty("changes", hasItems(MISSING, BUNDLE_REMOVED))
                )
        ));
    }

    @Test
    public void shouldNotCreateBundlesOnRequiredItemsMissing() {
        itemStockAvailability(primaryOffer, 2);
        itemStockAvailability(secondaryOffer, 0);

        MultiCart cart = orderCreateHelper.cart(stubRequestFor(single(orderWithYandexDelivery()
                .someLabel()
                .someBuyer()
                .deliveryBuilder(yandexDelivery())
                .itemBuilder(similar(primaryOffer)
                        .count(2))
                .itemBuilder(secondaryOffer)
        ), PROMO_KEY, config ->
                config.expectResponseItems(
                        itemResponseFor(primaryOffer)
                                .quantity(2)
                )));

        assertThat(firstOrder(cart).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(2))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(0)),
                        hasProperty("changes", hasItem(MISSING))
                )
        ));
    }

    @Test
    public void shouldCreateBundlesOnStockMissing() {
        itemStockAvailability(primaryOffer, 1);
        itemStockAvailability(secondaryOffer, 1);

        MultiCart cart = orderCreateHelper.cart(stubRequestFor(single(orderWithYandexDelivery()
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
                                        .primaryInBundle(false)
                        )));

        assertThat(firstOrder(cart).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(2)),
                        hasProperty("changes", hasItems(COUNT, BUNDLE_NEW))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_NEW))
                )
        ));
    }

    @Test
    public void shouldSplitBundlesOnStockMissing() {
        itemStockAvailability(primaryOffer, 2);
        itemStockAvailability(secondaryOffer, 1);

        MultiCart cart = orderCreateHelper.cart(stubRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .count(2))
                .itemBuilder(similar(secondaryOffer)
                        .count(2))
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

        assertThat(firstOrder(cart).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItems(BUNDLE_SPLIT, BUNDLE_NEW))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItems(COUNT, BUNDLE_NEW))
                ),
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasItems(BUNDLE_SPLIT))
                )
        ));
    }

    @Test
    public void shouldNotBrokeBundlesOnStockMissing() {
        itemStockAvailability(primaryOffer, 1);
        itemStockAvailability(secondaryOffer, 1);

        MultiCart cart = orderCreateHelper.cart(stubRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer).promoBundle("some bundle"))
                .itemBuilder(similar(secondaryOffer).promoBundle("some bundle"))
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
                                        .primaryInBundle(false)
                        )));

        assertThat(firstOrder(cart).getItems(), hasItems(
                allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(BUNDLE_ID))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", equalTo(PROMO_BUNDLE)),
                        hasProperty("count", comparesEqualTo(1)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItems(BUNDLE_ID))
                ),
                allOf(
                        hasProperty("offerId", equalTo(GIFT_OFFER)),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("count", comparesEqualTo(0)),
                        hasProperty("changes", hasSize(1)),
                        hasProperty("changes", hasItem(MISSING))
                )
        ));
    }

    private Parameters stubRequestFor(
            MultiCart multiCart, String promo, Consumer<LoyaltyParameters> loyaltyConfigurerConsumer
    ) {
        Parameters props = BlueGiftsOrderUtils.fbyRequestFor(multiCart, promo, loyaltyConfigurerConsumer);
        props.setMockPushApi(false);

        Order order = props.getOrder();

        List<DeliveryResponse> deliveryResponses = props
                .configuration()
                .cart()
                .mocks(order.getLabel())
                .getPushApiDeliveryResponses();

        props.configuration().cart().mocks(order.getLabel()).getReportParameters().setIgnoreStocks(false);
        props.configuration().cart().mocks(order.getLabel()).setStockStorageResponse(offerStockAvailability);

        Map<String, Integer> sskuToAmount = offerStockAvailability.stream()
                .collect(Collectors.toUnmodifiableMap(i -> i.getItem().getShopSku(), SSItemAmount::getAmount));
        Map<FeedOfferId, List<OrderItem>> itemsByOffer = order.getItems().stream()
                .collect(Collectors.groupingBy(OfferItem::getFeedOfferId));


        List<OrderItem> itemsToMock = new ArrayList<>();
        for (List<OrderItem> items : itemsByOffer.values()) {
            int amount = sskuToAmount.get(items.get(0).getShopSku());

            for (OrderItem item : items) {
                OrderItem copy = OfferItemUtils.deepCopy(item);
                copy.setCount(Math.min(copy.getCount(), amount));
                itemsToMock.add(copy);

                amount -= copy.getCount();
            }
        }

        pushApiConfigurer.mockCart(
                itemsToMock,
                order.getShopId(),
                deliveryResponses,
                order.getAcceptMethod(),
                props.isMultiCart()
        );

        return props;
    }

    private void itemStockAvailability(OrderItemBuilder offer, Integer count) {
        OrderItem item = offer.build();
        offerStockAvailability.add(
                SSItemAmount.of(SSItem.of(item.getShopSku(), item.getSupplierId(), item.getWarehouseId()), count));
    }
}
