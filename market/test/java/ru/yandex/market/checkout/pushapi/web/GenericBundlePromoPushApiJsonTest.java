package ru.yandex.market.checkout.pushapi.web;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartItem;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartParameters;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderAcceptHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderParameters;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Features;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider.OrderItemBuilder;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.shopapi.ShopApiConfigurer;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.FEED_ID;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.similar;

public class GenericBundlePromoPushApiJsonTest extends AbstractWebTestBase {

    public static final String PRIMARY_OFFER = "some primary offer";
    public static final String GIFT_OFFER = "some gift offer";
    public static final String OFFER_WITH_PROMOCODE = "some offer with promocode";
    public static final String PROMO_BUNDLE = "some bundle";
    public static final String PROMO_KEY = "some promo";
    public static final String ANAPLAN_ID = "some anaplan id";

    @Autowired
    private PushApiOrderAcceptHelper orderAcceptHelper;
    @Autowired
    private PushApiCartHelper cartHelper;
    @Autowired
    private ShopApiConfigurer shopApiConfigurer;
    @Autowired
    private WireMockServer shopadminStubMock;

    private final PromoDefinition promoDefinition = PromoDefinition.marketBundlePromo(
            PROMO_KEY,
            PROMO_KEY,
            null,
            ANAPLAN_ID,
            PROMO_BUNDLE,
            true,
            null
    );
    private final PromoDefinition promoDefinitionPromocode = PromoDefinition.marketPromocodePromo(
            PROMO_KEY,
            "L" + PROMO_KEY,
            123L
    );

    private OrderItemBuilder primaryOffer;
    private OrderItemBuilder secondaryOffer;
    private OrderItemBuilder promocodeOffer;

    @BeforeEach
    public void configure() {
        RequestContextHolder.createNewContext();

        primaryOffer = orderItemWithSortingCenter()
                .someId()
                .offer(PRIMARY_OFFER)
                .promoBundle(PROMO_BUNDLE)
                .promo(new ItemPromo(promoDefinition, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
                .price(9999);

        secondaryOffer = orderItemWithSortingCenter()
                .someId()
                .offer(GIFT_OFFER)
                .promoBundle(PROMO_BUNDLE)
                .promo(new ItemPromo(promoDefinition,
                        BigDecimal.valueOf(1999),
                        BigDecimal.valueOf(1999),
                        BigDecimal.valueOf(1999)))
                .price(1);

        promocodeOffer = orderItemWithSortingCenter()
                .someId()
                .offer(OFFER_WITH_PROMOCODE)
                .promoBundle(PROMO_BUNDLE)
                .promo(new ItemPromo(promoDefinitionPromocode, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
                .price(9999);

    }

    @Test
    void shouldSendCartOrderWithBundle() throws Exception {
        Cart cart = new Cart();
        cart.setDelivery(DeliveryProvider.yandexDelivery().build());
        cart.setCurrency(Currency.RUR);
        cart.setBuyer(BuyerProvider.getBuyer());
        cart.setItems(List.of(
                toCartItem(primaryOffer.build()),
                toCartItem(similar(primaryOffer).promoBundle(null).noPromos().build()),
                toCartItem(secondaryOffer.build())
        ));


        PushApiCartParameters parameters = new PushApiCartParameters(cart);
        parameters.setDataType(DataType.JSON);

        shopApiConfigurer.mockCart(parameters);
        mockSettingsForDifferentParameters(parameters);
        CartResponse cartResponse = cartHelper.cart(parameters);

        assertThat(cartResponse.getItems(), hasSize(3));
        assertThat(cartResponse.getItems(), hasItems(
                allOf(
                        hasProperty("offerItemKey", is(OfferItemKey.of(
                                PRIMARY_OFFER,
                                FEED_ID,
                                PROMO_BUNDLE
                        ))),
                        hasProperty("count", comparesEqualTo(1))
                ),
                allOf(
                        hasProperty("offerItemKey", is(OfferItemKey.of(
                                PRIMARY_OFFER,
                                FEED_ID,
                                null
                        ))),
                        hasProperty("count", comparesEqualTo(1))
                ),
                allOf(
                        hasProperty("offerItemKey", is(OfferItemKey.of(
                                GIFT_OFFER,
                                FEED_ID,
                                PROMO_BUNDLE
                        ))),
                        hasProperty("count", comparesEqualTo(1))
                )
        ));

        LoggedRequest request = extractRequest();
        assertThat(JsonPath.read(request.getBodyAsString(), "$.cart"), hasEntry(is("items"), hasSize(2)));
    }

    @Test
    void shouldSendCartOrderWithBundleWithItemMiss() throws Exception {
        Cart cart = new Cart();
        cart.setDelivery(DeliveryProvider.yandexDelivery().build());
        cart.setCurrency(Currency.RUR);
        cart.setBuyer(BuyerProvider.getBuyer());
        cart.setItems(List.of(
                toCartItem(primaryOffer.build()),
                toCartItem(similar(primaryOffer).promoBundle(null).noPromos().build()),
                toCartItem(secondaryOffer.build())
        ));

        PushApiCartParameters parameters = new PushApiCartParameters(cart);
        parameters.setDataType(DataType.JSON);

        CartResponse response = parameters.getShopCartResponse();

        CartResponse itemMissResponse = new CartResponse(List.of(
                similar(primaryOffer)
                        .promoBundle(null)
                        .feedCategoryId(null)
                        .categoryId(null)
                        .vat(null)
                        .noPromos()
                        .count(1)
                        .build(),
                similar(secondaryOffer)
                        .promoBundle(null)
                        .feedCategoryId(null)
                        .categoryId(null)
                        .vat(null)
                        .noPromos()
                        .count(1)
                        .build()
        ),
                response.getDeliveryOptions(),
                response.getPaymentMethods());

        parameters.setShopCartResponse(itemMissResponse);

        shopApiConfigurer.mockCart(parameters);
        mockSettingsForDifferentParameters(parameters);
        CartResponse cartResponse = cartHelper.cart(parameters);

        assertThat(cartResponse.getItems(), hasSize(3));
        assertThat(cartResponse.getItems(), hasItems(
                allOf(
                        hasProperty("offerItemKey", is(OfferItemKey.of(
                                PRIMARY_OFFER,
                                FEED_ID,
                                PROMO_BUNDLE
                        ))),
                        hasProperty("count", comparesEqualTo(1))
                ),
                allOf(
                        hasProperty("offerItemKey", is(OfferItemKey.of(
                                PRIMARY_OFFER,
                                FEED_ID,
                                null
                        ))),
                        hasProperty("count", comparesEqualTo(0))
                ),
                allOf(
                        hasProperty("offerItemKey", is(OfferItemKey.of(
                                GIFT_OFFER,
                                FEED_ID,
                                PROMO_BUNDLE
                        ))),
                        hasProperty("count", comparesEqualTo(1))
                )
        ));

        LoggedRequest request = extractRequest();
        assertThat(JsonPath.read(request.getBodyAsString(), "$.cart"), hasEntry(is("items"), hasSize(2)));
    }

    @Test
    void shouldSendAcceptOrderWithBundles() throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters(OrderProvider.orderBuilder()
                .configure(OrderProvider::applyDefaults)
                .someId()
                .status(OrderStatus.PLACING)
                .deliveryBuilder(DeliveryProvider.yandexDelivery())
                .itemBuilder(primaryOffer)
                .itemBuilder(similar(primaryOffer).promoBundle(null).noPromos())
                .itemBuilder(secondaryOffer)
                .build());
        parameters.setDataType(DataType.JSON);
        parameters.setFeatures(Features.builder()
                .enabledGenericBundleSupport(true).
                build());

        parameters.setOrderResponse(
                new OrderResponse(parameters.getOrder().getId().toString(), true, null));

        shopApiConfigurer.mockOrderResponse(parameters);
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        LoggedRequest request = extractRequest();
        assertThat(request, notNullValue());
        assertThat(JsonPath.read(request.getBodyAsString(), "$.order"), hasEntry(is("items"), hasSize(3)));
        assertThat(JsonPath.read(request.getBodyAsString(), "$.order.items"), hasItems(
                allOf(
                        hasEntry("offerId", PRIMARY_OFFER),
                        hasEntry("bundleId", PROMO_BUNDLE)
                ),
                allOf(
                        hasEntry("offerId", PRIMARY_OFFER),
                        not(hasEntry("bundleId", PROMO_BUNDLE))
                ),
                allOf(
                        hasEntry("offerId", GIFT_OFFER),
                        hasEntry("bundleId", PROMO_BUNDLE)
                )
        ));

        assertThat(JsonPath.read(request.getBodyAsString(), "$.order.items[*].promos"), everyItem(
                everyItem(hasKey("discount"))
        ));
    }

    @Test
    void shouldSendAcceptOrderWithPromocode() throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters(OrderProvider.orderBuilder()
                .configure(OrderProvider::applyDefaults)
                .someId()
                .status(OrderStatus.PLACING)
                .deliveryBuilder(DeliveryProvider.yandexDelivery())
                .itemBuilder(primaryOffer)
                .itemBuilder(promocodeOffer)
                .build());
        parameters.setDataType(DataType.JSON);
        parameters.setFeatures(Features.builder()
                .enabledGenericBundleSupport(true).
                build());

        parameters.setOrderResponse(
                new OrderResponse(parameters.getOrder().getId().toString(), true, null));

        shopApiConfigurer.mockOrderResponse(parameters);
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        LoggedRequest request = extractRequest();
        assertThat(request, notNullValue());
        assertThat(JsonPath.read(request.getBodyAsString(), "$.order"), hasEntry(is("items"), hasSize(2)));
        assertThat(JsonPath.read(request.getBodyAsString(), "$.order.items"), hasItems(
                allOf(
                        hasEntry("offerId", PRIMARY_OFFER),
                        hasEntry("bundleId", PROMO_BUNDLE)
                ),
                allOf(
                        hasEntry("offerId", OFFER_WITH_PROMOCODE),
                        hasEntry("bundleId", PROMO_BUNDLE)
                )
        ));

        assertThat(JsonPath.read(request.getBodyAsString(), "$.order.items[*].promos"), everyItem(
                everyItem(hasKey("discount"))
        ));
    }

    private LoggedRequest extractRequest() {
        List<ServeEvent> serveEvents = shopadminStubMock.getAllServeEvents();
        assertThat(serveEvents, hasSize(1));
        ServeEvent event = Iterables.getOnlyElement(serveEvents);
        return event.getRequest();
    }

    private CartItem toCartItem(OrderItem orderItem) {
        final CartItem cartItem = new CartItem();
        cartItem.setId(orderItem.getId());
        cartItem.setFeedId(orderItem.getFeedId());
        cartItem.setFeedCategoryId(orderItem.getFeedCategoryId());
        cartItem.setOfferId(orderItem.getOfferId());
        cartItem.setOfferName(orderItem.getOfferName());
        cartItem.setBundleId(orderItem.getBundleId());
        cartItem.setPrice(orderItem.getPrice());
        cartItem.setCount(orderItem.getCount());
        cartItem.setPromos(new HashSet<>(orderItem.getPromos()));
        return cartItem;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopCartResponse {

        private final CartResponse cart;

        @JsonCreator
        public ShopCartResponse(
                @JsonProperty("cart") CartResponse cart
        ) {
            this.cart = cart;
        }

        public CartResponse getCart() {
            return cart;
        }
    }
}
