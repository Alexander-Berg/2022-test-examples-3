package ru.yandex.market.checkout.checkouter.cashback;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.ItemPrices;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyContext;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.loyalty.api.model.CashbackOptionsRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class CashbackRequestFactoryTest extends AbstractWebTestBase {

    public static final long FEED = 1;
    public static final String OFFER = "first offer";
    public static final String CART_LABEL = "145_ABCDE";

    @Test
    void shouldBuildCorrectCashbackRequest() {
        CashbackOptionsRequest cashbackOptionsRequest = CashbackRequestFactory.buildCashbackOptionsRequest(
                multiCart(false, false),
                loyaltyContext(),
                checkouterFeatureReader);

        assertEquals(1, cashbackOptionsRequest.getOrders().size());
        assertThat(cashbackOptionsRequest.getOrders().get(0), allOf(
                hasProperty("cartId", is(CART_LABEL)),
                hasProperty("orderId", nullValue()),
                hasProperty("items", hasItem(allOf(
                        hasProperty("feedId", is(FEED)),
                        hasProperty("hyperCategoryId", is(123)),
                        hasProperty("offerId", is(OFFER)),
                        hasProperty("price", is(BigDecimal.valueOf(9099))),
                        hasProperty("discount", is(BigDecimal.valueOf(1499))),
                        hasProperty("quantity", is(BigDecimal.valueOf(1))),
                        hasProperty("shopSku", is(OFFER)),
                        hasProperty("supplierId", is(123L)),
                        hasProperty("warehouseId", is(123)),
                        hasProperty("sku", is(String.valueOf(FEED + OFFER.hashCode()))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("vendorId", is(10545982L)),
                        hasProperty("deliveryPartnerTypes", contains(DeliveryPartnerType.SHOP.name(),
                                DeliveryPartnerType.YANDEX_MARKET.name())),
                        hasProperty("downloadable", is(true))
                ))),
                hasProperty("isMarketBrandedPickup", is(false))
        ));
    }

    @Test
    void shouldBuildCorrectCashbackRequestWithMarketBrandedOptions() {
        CheckoutContextHolder.getExperiments().addExperiment(
                Experiments.CASHBACK_DELIVERY_PROMOTION_STRAIGHT_EXP,
                Experiments.CASHBACK_DELIVERY_PROMOTION_EXP_VALUE
        );

        CashbackOptionsRequest cashbackOptionsRequest = CashbackRequestFactory.buildCashbackOptionsRequest(
                multiCart(true, false),
                loyaltyContext(),
                checkouterFeatureReader);

        assertEquals(1, cashbackOptionsRequest.getOrders().size());
        assertThat(cashbackOptionsRequest.getOrders().get(0), allOf(
                hasProperty("cartId", is(CART_LABEL)),
                hasProperty("orderId", nullValue()),
                hasProperty("items", hasItem(allOf(
                        hasProperty("feedId", is(FEED)),
                        hasProperty("hyperCategoryId", is(123)),
                        hasProperty("offerId", is(OFFER)),
                        hasProperty("price", is(BigDecimal.valueOf(9099))),
                        hasProperty("discount", is(BigDecimal.valueOf(1499))),
                        hasProperty("quantity", is(BigDecimal.valueOf(1))),
                        hasProperty("shopSku", is(OFFER)),
                        hasProperty("supplierId", is(123L)),
                        hasProperty("warehouseId", is(123)),
                        hasProperty("sku", is(String.valueOf(FEED + OFFER.hashCode()))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("vendorId", is(10545982L)),
                        hasProperty("deliveryPartnerTypes", contains(DeliveryPartnerType.SHOP.name(),
                                DeliveryPartnerType.YANDEX_MARKET.name())),
                        hasProperty("downloadable", is(true))
                ))),
                hasProperty("isMarketBrandedPickup", is(true))
        ));
        CheckoutContextHolder.getExperiments().remove(Experiments.CASHBACK_DELIVERY_PROMOTION_STRAIGHT_EXP);
    }

    @Test
    void shouldBuildCorrectCashbackRequestWithMarketBrandedDelivery() {
        CheckoutContextHolder.getExperiments().addExperiment(
                Experiments.CASHBACK_DELIVERY_PROMOTION_STRAIGHT_EXP,
                Experiments.CASHBACK_DELIVERY_PROMOTION_EXP_VALUE
        );
        CashbackOptionsRequest cashbackOptionsRequest = CashbackRequestFactory.buildCashbackOptionsRequest(
                multiCart(false, true),
                loyaltyContext(),
                checkouterFeatureReader);

        assertEquals(1, cashbackOptionsRequest.getOrders().size());
        assertThat(cashbackOptionsRequest.getOrders().get(0), allOf(
                hasProperty("cartId", is(CART_LABEL)),
                hasProperty("orderId", nullValue()),
                hasProperty("items", hasItem(allOf(
                        hasProperty("feedId", is(FEED)),
                        hasProperty("hyperCategoryId", is(123)),
                        hasProperty("offerId", is(OFFER)),
                        hasProperty("price", is(BigDecimal.valueOf(9099))),
                        hasProperty("discount", is(BigDecimal.valueOf(1499))),
                        hasProperty("quantity", is(BigDecimal.valueOf(1))),
                        hasProperty("shopSku", is(OFFER)),
                        hasProperty("supplierId", is(123L)),
                        hasProperty("warehouseId", is(123)),
                        hasProperty("sku", is(String.valueOf(FEED + OFFER.hashCode()))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("vendorId", is(10545982L)),
                        hasProperty("deliveryPartnerTypes", contains(DeliveryPartnerType.SHOP.name(),
                                DeliveryPartnerType.YANDEX_MARKET.name())),
                        hasProperty("downloadable", is(true))
                ))),
                hasProperty("isMarketBrandedPickup", is(true))
        ));
        CheckoutContextHolder.getExperiments().remove(Experiments.CASHBACK_DELIVERY_PROMOTION_STRAIGHT_EXP);
    }

    @Test
    void shouldBuildCorrectCashbackRequestWithMarketBrandedDeliveryAndStraightExperiment() {
        CheckoutContextHolder.getExperiments().addExperiment(
                Experiments.CASHBACK_DELIVERY_PROMOTION_STRAIGHT_EXP,
                Experiments.CASHBACK_DELIVERY_PROMOTION_EXP_VALUE
        );

        CashbackOptionsRequest cashbackOptionsRequest = CashbackRequestFactory.buildCashbackOptionsRequest(
                multiCart(false, true),
                loyaltyContext(),
                checkouterFeatureReader);

        assertEquals(1, cashbackOptionsRequest.getOrders().size());
        assertThat(cashbackOptionsRequest.getOrders().get(0), allOf(
                hasProperty("cartId", is(CART_LABEL)),
                hasProperty("orderId", nullValue()),
                hasProperty("items", hasItem(allOf(
                        hasProperty("feedId", is(FEED)),
                        hasProperty("hyperCategoryId", is(123)),
                        hasProperty("offerId", is(OFFER)),
                        hasProperty("price", is(BigDecimal.valueOf(9099))),
                        hasProperty("discount", is(BigDecimal.valueOf(1499))),
                        hasProperty("quantity", is(BigDecimal.valueOf(1))),
                        hasProperty("shopSku", is(OFFER)),
                        hasProperty("supplierId", is(123L)),
                        hasProperty("warehouseId", is(123)),
                        hasProperty("sku", is(String.valueOf(FEED + OFFER.hashCode()))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("vendorId", is(10545982L)),
                        hasProperty("deliveryPartnerTypes", contains(DeliveryPartnerType.SHOP.name(),
                                DeliveryPartnerType.YANDEX_MARKET.name())),
                        hasProperty("downloadable", is(true))
                ))),
                hasProperty("isMarketBrandedPickup", is(true))
        ));

        CheckoutContextHolder.getExperiments().remove(Experiments.CASHBACK_DELIVERY_PROMOTION_STRAIGHT_EXP);
    }

    @Test
    void shouldBuildCorrectCashbackRequestWithMarketBrandedDeliveryAndReversedExperimentAndToggle() {
        CheckoutContextHolder.getExperiments().addExperiment(
                Experiments.CASHBACK_DELIVERY_PROMOTION_REVERSED_EXP,
                Experiments.CASHBACK_DELIVERY_PROMOTION_EXP_VALUE
        );
        checkouterProperties.setEnabledBrandedPickupBoostReversedExperiment(true);

        CashbackOptionsRequest cashbackOptionsRequest = CashbackRequestFactory.buildCashbackOptionsRequest(
                multiCart(false, true),
                loyaltyContext(),
                checkouterFeatureReader);

        assertEquals(1, cashbackOptionsRequest.getOrders().size());
        assertThat(cashbackOptionsRequest.getOrders().get(0), allOf(
                hasProperty("cartId", is(CART_LABEL)),
                hasProperty("orderId", nullValue()),
                hasProperty("items", hasItem(allOf(
                        hasProperty("feedId", is(FEED)),
                        hasProperty("hyperCategoryId", is(123)),
                        hasProperty("offerId", is(OFFER)),
                        hasProperty("price", is(BigDecimal.valueOf(9099))),
                        hasProperty("discount", is(BigDecimal.valueOf(1499))),
                        hasProperty("quantity", is(BigDecimal.valueOf(1))),
                        hasProperty("shopSku", is(OFFER)),
                        hasProperty("supplierId", is(123L)),
                        hasProperty("warehouseId", is(123)),
                        hasProperty("sku", is(String.valueOf(FEED + OFFER.hashCode()))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("vendorId", is(10545982L)),
                        hasProperty("deliveryPartnerTypes", contains(DeliveryPartnerType.SHOP.name(),
                                DeliveryPartnerType.YANDEX_MARKET.name())),
                        hasProperty("downloadable", is(true))
                ))),
                hasProperty("isMarketBrandedPickup", is(false))
        ));

        CheckoutContextHolder.getExperiments().remove(Experiments.CASHBACK_DELIVERY_PROMOTION_REVERSED_EXP);
    }

    @Test
    void shouldBuildCorrectCashbackRequestWithMarketBrandedDeliveryAndReversedExperimentWithoutToggle() {
        CheckoutContextHolder.getExperiments().addExperiment(
                Experiments.CASHBACK_DELIVERY_PROMOTION_REVERSED_EXP,
                Experiments.CASHBACK_DELIVERY_PROMOTION_EXP_VALUE
        );
        checkouterProperties.setEnabledBrandedPickupBoostReversedExperiment(false);

        CashbackOptionsRequest cashbackOptionsRequest = CashbackRequestFactory.buildCashbackOptionsRequest(
                multiCart(false, true),
                loyaltyContext(),
                checkouterFeatureReader);

        assertEquals(1, cashbackOptionsRequest.getOrders().size());
        assertThat(cashbackOptionsRequest.getOrders().get(0), allOf(
                hasProperty("cartId", is(CART_LABEL)),
                hasProperty("orderId", nullValue()),
                hasProperty("items", hasItem(allOf(
                        hasProperty("feedId", is(FEED)),
                        hasProperty("hyperCategoryId", is(123)),
                        hasProperty("offerId", is(OFFER)),
                        hasProperty("price", is(BigDecimal.valueOf(9099))),
                        hasProperty("discount", is(BigDecimal.valueOf(1499))),
                        hasProperty("quantity", is(BigDecimal.valueOf(1))),
                        hasProperty("shopSku", is(OFFER)),
                        hasProperty("supplierId", is(123L)),
                        hasProperty("warehouseId", is(123)),
                        hasProperty("sku", is(String.valueOf(FEED + OFFER.hashCode()))),
                        hasProperty("bundleId", nullValue()),
                        hasProperty("vendorId", is(10545982L)),
                        hasProperty("deliveryPartnerTypes", contains(DeliveryPartnerType.SHOP.name(),
                                DeliveryPartnerType.YANDEX_MARKET.name())),
                        hasProperty("downloadable", is(true))
                ))),
                hasProperty("isMarketBrandedPickup", is(false))
        ));

        CheckoutContextHolder.getExperiments().remove(Experiments.CASHBACK_DELIVERY_PROMOTION_REVERSED_EXP);
    }

    private static MultiCart multiCart(boolean marketBrandedOptions, boolean marketBrandedDelivery) {
        OrderItem orderItem = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .configure(b -> b.feedId(FEED)
                        .offer(OFFER)
                        .warehouseId(123))
                .count(1)
                .configure(b -> b.digital(true))
                .build();
        ItemPrices prices = orderItem.getPrices();
        //после применения скидок в LoyaltyProcessor эти цены будут посчитаны
        prices.setBuyerPriceBeforeDiscount(BigDecimal.valueOf(9099));
        prices.setBuyerDiscount(BigDecimal.valueOf(1499));

        var order = OrderProvider.orderBuilder()
                .deliveryBuilder(
                        DeliveryProvider
                                .yandexDelivery()
                                .pickup(marketBrandedDelivery))
                .label(CART_LABEL)
                .item(orderItem)
                .build();

        var deliveryOption = DeliveryProvider.getShopDelivery();
        deliveryOption.setMarketBranded(marketBrandedOptions);
        order.setDeliveryOptions(List.of(deliveryOption));

        return MultiCartProvider.single(order);
    }

    private static LoyaltyContext loyaltyContext() {
        return new LoyaltyContext(
                Map.of(),
                Map.of(),
                Currency.RUR,
                Stream.of(FoundOfferBuilder.create()
                                .feedId(FEED)
                                .offerId(OFFER)
                                .deliveryPartnerType(DeliveryPartnerType.SHOP.name())
                                .deliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET.name())
                                .build())
                        .collect(Collectors.toUnmodifiableMap(FoundOffer::getFeedOfferId,
                                Function.identity(), (e1, e2) -> e1)),
                Collections.emptyMap(), Collections.emptyMap(), null, null);
    }
}
