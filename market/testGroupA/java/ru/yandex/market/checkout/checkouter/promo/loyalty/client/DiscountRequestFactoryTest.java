package ru.yandex.market.checkout.checkouter.promo.loyalty.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoDiscount;
import ru.yandex.market.checkout.checkouter.promo.AbstractPromoTestBase;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyContext;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider.OrderItemBuilder;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.OfferPromo;
import ru.yandex.market.common.report.model.PromoDetails;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType.BLUE_FLASH;
import static ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType.BLUE_SET;
import static ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType.BLUE_SET_SECONDARY;
import static ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType.CHEAPEST_AS_GIFT;
import static ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType.DIRECT_DISCOUNT;
import static ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType.GENERIC_BUNDLE;
import static ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType.GENERIC_BUNDLE_SECONDARY;
import static ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType.PRICE_DROP_AS_YOU_SHOP;

public class DiscountRequestFactoryTest extends AbstractPromoTestBase {

    public static final long FEED = 1;
    public static final String FIRST_OFFER = "first offer";
    public static final String SECOND_OFFER = "second offer";
    public static final String PROMO_KEY = "promo key";

    @Autowired
    private DiscountRequestFactory requestFactory;

    @Test
    void shouldCreateRequestWithPriceDropDiscount() {
        MultiCartWithBundlesDiscountRequest discountRequest = requestFactory.buildDiscountWithBundlesRequest(
                MultiCartProvider.single(OrderProvider.orderBuilder()
                        .label("some label")
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, FIRST_OFFER))
                                .configure(price(10000))
                                .count(1)
                        )
                ),
                loyaltyContext(ReportPromoDiscount.builder()
                        .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                        .reportPromoType(PRICE_DROP_AS_YOU_SHOP)
                        .discount(BigDecimal.valueOf(2000))
                        .promoDetails(PromoDetails.builder()
                                .promoKey(PROMO_KEY)
                                .promoType(PRICE_DROP_AS_YOU_SHOP.getCode())
                                .build())
                        .build())
        );

        assertThat(discountRequest.getOrders(), not(empty()));
        assertThat(discountRequest.getOrders().iterator().next().getItems(), hasItem(allOf(
                hasProperty("price", comparesEqualTo(BigDecimal.valueOf(12000))),
                hasProperty("promoDiscounts", hasItem(allOf(
                        hasProperty("promoKey", is(PROMO_KEY)),
                        hasProperty("promoType", is(PRICE_DROP_AS_YOU_SHOP.name())),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(2000)))
                )))
        )));
    }

    @Test
    void shouldCreateRequestWithBlueFlashPromo() {
        MultiCartWithBundlesDiscountRequest discountRequest = requestFactory.buildDiscountWithBundlesRequest(
                MultiCartProvider.single(OrderProvider.orderBuilder()
                        .label("some label")
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, FIRST_OFFER))
                                .configure(price(2000))
                                .count(1)
                        )
                ),
                loyaltyContext(ReportPromoDiscount.builder()
                        .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                        .reportPromoType(BLUE_FLASH)
                        .discount(BigDecimal.valueOf(8000))
                        .promoDetails(PromoDetails.builder()
                                .promoFixedPrice(BigDecimal.valueOf(2000))
                                .promoKey(PROMO_KEY)
                                .promoType(BLUE_FLASH.getCode())
                                .build())
                        .build())
        );

        assertThat(discountRequest.getOrders(), not(empty()));
        assertThat(discountRequest.getOrders().iterator().next().getItems(), hasItem(allOf(
                hasProperty("price", comparesEqualTo(BigDecimal.valueOf(10000))),
                hasProperty("promoDiscounts", hasItem(allOf(
                        hasProperty("promoKey", is(PROMO_KEY)),
                        hasProperty("promoType", is(PromoType.BLUE_FLASH.getCode())),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(8000)))
                )))
        )));
    }

    @Test
    void shouldCreateRequestWithBlueGiftPromo() {
        MultiCartWithBundlesDiscountRequest discountRequest = requestFactory.buildDiscountWithBundlesRequest(
                MultiCartProvider.single(OrderProvider.orderBuilder()
                        .label("some label")
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, FIRST_OFFER))
                                .configure(price(10000))
                                .count(1)
                        )
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, SECOND_OFFER))
                                .configure(price(2000))
                                .count(1)
                        )
                ),
                loyaltyContext(ReportPromoDiscount.builder()
                        .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                        .reportPromoType(GENERIC_BUNDLE)
                        .promoDetails(PromoDetails.builder()
                                .promoKey(PROMO_KEY)
                                .promoType(GENERIC_BUNDLE.getCode())
                                .build())
                        .build())
        );

        assertThat(discountRequest.getOrders(), not(empty()));
        assertThat(discountRequest.getOrders().iterator().next().getItems(), hasItem(allOf(
                hasProperty("offerId", is(FIRST_OFFER)),
                hasProperty("price", comparesEqualTo(BigDecimal.valueOf(10000))),
                hasProperty("promoKeys", hasItem(PROMO_KEY))
        )));
    }

    @Test
    void shouldCreateRequestWithBlueGiftAndPriceDropPromos() {
        MultiCartWithBundlesDiscountRequest discountRequest = requestFactory.buildDiscountWithBundlesRequest(
                MultiCartProvider.single(OrderProvider.orderBuilder()
                        .label("some label")
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, FIRST_OFFER))
                                .configure(price(10000))
                                .count(1)
                        )
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, SECOND_OFFER))
                                .configure(price(2000))
                                .count(1)
                        )
                ),
                loyaltyContext(
                        ReportPromoDiscount.builder()
                                .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                                .reportPromoType(GENERIC_BUNDLE)
                                .promoDetails(PromoDetails.builder()
                                        .promoKey(PROMO_KEY)
                                        .promoType(GENERIC_BUNDLE.getCode())
                                        .build())
                                .build(),
                        ReportPromoDiscount.builder()
                                .feedOfferId(FeedOfferId.from(FEED, SECOND_OFFER))
                                .reportPromoType(PRICE_DROP_AS_YOU_SHOP)
                                .discount(BigDecimal.valueOf(2000))
                                .promoDetails(PromoDetails.builder()
                                        .promoKey(PROMO_KEY)
                                        .promoType(PRICE_DROP_AS_YOU_SHOP.getCode())
                                        .build())
                                .build())
        );

        assertThat(discountRequest.getOrders(), not(empty()));
        assertThat(discountRequest.getOrders().iterator().next().getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_OFFER)),
                hasProperty("price", comparesEqualTo(BigDecimal.valueOf(10000))),
                hasProperty("promoKeys", hasItem(PROMO_KEY))
        ), allOf(
                hasProperty("offerId", is(SECOND_OFFER)),
                hasProperty("price", comparesEqualTo(BigDecimal.valueOf(4000))),
                hasProperty("promoDiscounts", hasItem(allOf(
                        hasProperty("promoKey", is(PROMO_KEY)),
                        hasProperty("promoType", is(PRICE_DROP_AS_YOU_SHOP.name())),
                        hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(2000)))
                )))
        )));
    }

    @Test
    void shouldCreateRequestWithBlueSet() {
        MultiCartWithBundlesDiscountRequest discountRequest = requestFactory.buildDiscountWithBundlesRequest(
                MultiCartProvider.single(OrderProvider.orderBuilder()
                        .label("some label")
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, FIRST_OFFER))
                                .configure(price(3000))
                                .count(1)
                        )
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, SECOND_OFFER))
                                .configure(price(2000))
                                .count(1)
                        )
                ),
                loyaltyContext(
                        ReportPromoDiscount.builder()
                                .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                                .reportPromoType(BLUE_SET)
                                .promoDetails(PromoDetails.builder()
                                        .promoKey(PROMO_KEY)
                                        .promoType(BLUE_SET.getCode())
                                        .build())
                                .build(),
                        ReportPromoDiscount.builder()
                                .feedOfferId(FeedOfferId.from(FEED, SECOND_OFFER))
                                .reportPromoType(BLUE_SET_SECONDARY)
                                .promoDetails(PromoDetails.builder()
                                        .promoKey(PROMO_KEY)
                                        .promoType(BLUE_SET_SECONDARY.getCode())
                                        .build())
                                .build())
        );

        assertThat(discountRequest.getOrders(), not(empty()));
        assertThat(discountRequest.getOrders().iterator().next().getItems(), hasItems(allOf(
                hasProperty("offerId", is(FIRST_OFFER)),
                hasProperty("price", comparesEqualTo(BigDecimal.valueOf(3000))),
                hasProperty("promoKeys", hasItem(PROMO_KEY))
        ), allOf(
                hasProperty("offerId", is(SECOND_OFFER)),
                hasProperty("price", comparesEqualTo(BigDecimal.valueOf(2000))),
                hasProperty("promoKeys", hasItem(PROMO_KEY))
        )));
    }

    @Test
    void shouldCreateRequestWithMultiPromo() {
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        String key4 = "key4";
        String key5 = "key5";

        LoyaltyContext loyaltyContext =
                loyaltyContextWithMultiPromo(
                        ReportPromoDiscount.builder()
                                .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                                .reportPromoType(BLUE_SET)
                                .promoDetails(PromoDetails.builder()
                                        .promoKey(key1)
                                        .promoType(BLUE_SET.getCode())
                                        .build())
                                .build(),
                        ReportPromoDiscount.builder()
                                .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                                .reportPromoType(DIRECT_DISCOUNT)
                                .discount(BigDecimal.valueOf(20))
                                .promoDetails(PromoDetails.builder()
                                        .promoKey(key2)
                                        .promoType(DIRECT_DISCOUNT.getCode())
                                        .build())
                                .build(),
                        ReportPromoDiscount.builder()
                                .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                                .reportPromoType(GENERIC_BUNDLE)
                                .promoDetails(PromoDetails.builder()
                                        .promoKey(key4)
                                        .promoType(GENERIC_BUNDLE.getCode())
                                        .build())
                                .build(),
                        ReportPromoDiscount.builder()
                                .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                                .reportPromoType(CHEAPEST_AS_GIFT)
                                .promoDetails(PromoDetails.builder()
                                        .promoKey(key5)
                                        .promoType(CHEAPEST_AS_GIFT.getCode())
                                        .build())
                                .build(),
                        ReportPromoDiscount.builder()
                                .feedOfferId(FeedOfferId.from(FEED, SECOND_OFFER))
                                .reportPromoType(BLUE_SET_SECONDARY)
                                .promoDetails(PromoDetails.builder()
                                        .promoKey(key1)
                                        .promoType(BLUE_SET_SECONDARY.getCode())
                                        .build())
                                .build(),
                        ReportPromoDiscount.builder()
                                .feedOfferId(FeedOfferId.from(FEED, SECOND_OFFER))
                                .reportPromoType(GENERIC_BUNDLE_SECONDARY)
                                .promoDetails(PromoDetails.builder()
                                        .promoKey(key4)
                                        .promoType(GENERIC_BUNDLE_SECONDARY.getCode())
                                        .build())
                                .build()
                );

        MultiCartWithBundlesDiscountRequest discountRequest = requestFactory.buildDiscountWithBundlesRequest(
                MultiCartProvider.single(
                        OrderProvider.orderBuilder()
                                .label("some label")
                                .itemBuilder(OrderItemProvider.orderItemBuilder()
                                        .configure(OrderItemProvider::applyDefaults)
                                        .configure(offer(FEED, FIRST_OFFER))
                                        .configure(price(10000))
                                        .count(1)
                                )
                                .itemBuilder(OrderItemProvider.orderItemBuilder()
                                        .configure(OrderItemProvider::applyDefaults)
                                        .configure(offer(FEED, SECOND_OFFER))
                                        .configure(price(2000))
                                        .count(1)
                                )
                ),
                loyaltyContext
        );

        assertThat(discountRequest.getOrders(), not(empty()));
        List<BundledOrderItemRequest> items = discountRequest.getOrders().iterator().next().getItems();
        assertThat(items, hasSize(2));

        assertThat(items.get(0).getPromoKeys(), containsInAnyOrder(key1, key4, key5));
        assertThat(items.get(1).getPromoKeys(), containsInAnyOrder(key1, key4));
    }

    @Test
    void shouldCreateRequestWithDigitalOrder() {
        var discountRequest = requestFactory.buildDiscountWithBundlesRequest(
                MultiCartProvider.single(OrderProvider.orderBuilder()
                        .label("some label")
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, FIRST_OFFER))
                                .configure(price(10000))
                                .configure(b -> b.digital(true))
                                .count(1)
                        )
                ),
                loyaltyContext(ReportPromoDiscount.builder()
                        .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                        .reportPromoType(PRICE_DROP_AS_YOU_SHOP)
                        .discount(BigDecimal.valueOf(2000))
                        .promoDetails(PromoDetails.builder()
                                .promoKey(PROMO_KEY)
                                .promoType(PRICE_DROP_AS_YOU_SHOP.getCode())
                                .build())
                        .build())
        );

        assertThat(discountRequest.getOrders(), not(empty()));
        var orderWithBundlesRequest = Iterables.getOnlyElement(discountRequest.getOrders());
        assertThat(orderWithBundlesRequest.getItems(), hasItem(hasProperty("downloadable", is(true))));
    }

    @Test
    void shouldCreateRequestWithMarketBrandedDelivery() {
        CheckoutContextHolder.getExperiments().addExperiment(
                Experiments.CASHBACK_DELIVERY_PROMOTION_STRAIGHT_EXP,
                Experiments.CASHBACK_DELIVERY_PROMOTION_EXP_VALUE
        );

        var discountRequest = requestFactory.buildDiscountWithBundlesRequest(
                MultiCartProvider.single(OrderProvider.orderBuilder()
                        .label("some label")
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, FIRST_OFFER))
                                .configure(price(10000))
                                .configure(b -> b.digital(true))
                                .count(1)
                        )
                        .deliveryBuilder(DeliveryProvider
                                .yandexDelivery()
                                .pickup(true))
                ),
                loyaltyContext(ReportPromoDiscount.builder()
                        .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                        .reportPromoType(PRICE_DROP_AS_YOU_SHOP)
                        .discount(BigDecimal.valueOf(2000))
                        .promoDetails(PromoDetails.builder()
                                .promoKey(PROMO_KEY)
                                .promoType(PRICE_DROP_AS_YOU_SHOP.getCode())
                                .build())
                        .build())
        );

        assertThat(discountRequest.getOrders(), not(empty()));
        var orderWithBundlesRequest = Iterables.getOnlyElement(discountRequest.getOrders());
        assertThat(orderWithBundlesRequest.getDeliveries(), hasItem(hasProperty("isMarketBrandedPickup", is(true))));
        CheckoutContextHolder.getExperiments().remove(Experiments.CASHBACK_DELIVERY_PROMOTION_STRAIGHT_EXP);
    }

    @Test
    void shouldCreateRequestWithoutMarketBrandedDelivery() {
        CheckoutContextHolder.getExperiments().addExperiment(
                Experiments.CASHBACK_DELIVERY_PROMOTION_STRAIGHT_EXP,
                Experiments.CASHBACK_DELIVERY_PROMOTION_EXP_VALUE
        );

        var discountRequest = requestFactory.buildDiscountWithBundlesRequest(
                MultiCartProvider.single(OrderProvider.orderBuilder()
                        .label("some label")
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, FIRST_OFFER))
                                .configure(price(10000))
                                .configure(b -> b.digital(true))
                                .count(1)
                        )
                        .deliveryBuilder(DeliveryProvider
                                .yandexDelivery()
                                .pickup(false))
                ),
                loyaltyContext(ReportPromoDiscount.builder()
                        .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                        .reportPromoType(PRICE_DROP_AS_YOU_SHOP)
                        .discount(BigDecimal.valueOf(2000))
                        .promoDetails(PromoDetails.builder()
                                .promoKey(PROMO_KEY)
                                .promoType(PRICE_DROP_AS_YOU_SHOP.getCode())
                                .build())
                        .build())
        );

        assertThat(discountRequest.getOrders(), not(empty()));
        var orderWithBundlesRequest = Iterables.getOnlyElement(discountRequest.getOrders());
        assertThat(orderWithBundlesRequest.getDeliveries(), hasItem(hasProperty("isMarketBrandedPickup", is(false))));
        CheckoutContextHolder.getExperiments().remove(Experiments.CASHBACK_DELIVERY_PROMOTION_STRAIGHT_EXP);
    }

    @Test
    void shouldCreateRequestWithMarketBrandedDeliveryWhenReversedExperimentAndToggle() {
        CheckoutContextHolder.getExperiments().addExperiment(
                Experiments.CASHBACK_DELIVERY_PROMOTION_REVERSED_EXP,
                Experiments.CASHBACK_DELIVERY_PROMOTION_EXP_VALUE
        );
        checkouterProperties.setEnabledBrandedPickupBoostReversedExperiment(true);

        var discountRequest = requestFactory.buildDiscountWithBundlesRequest(
                MultiCartProvider.single(OrderProvider.orderBuilder()
                        .label("some label")
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, FIRST_OFFER))
                                .configure(price(10000))
                                .configure(b -> b.digital(true))
                                .count(1)
                        )
                        .deliveryBuilder(DeliveryProvider
                                .yandexDelivery()
                                .pickup(true))
                ),
                loyaltyContext(ReportPromoDiscount.builder()
                        .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                        .reportPromoType(PRICE_DROP_AS_YOU_SHOP)
                        .discount(BigDecimal.valueOf(2000))
                        .promoDetails(PromoDetails.builder()
                                .promoKey(PROMO_KEY)
                                .promoType(PRICE_DROP_AS_YOU_SHOP.getCode())
                                .build())
                        .build())
        );

        assertThat(discountRequest.getOrders(), not(empty()));
        var orderWithBundlesRequest = Iterables.getOnlyElement(discountRequest.getOrders());
        assertThat(orderWithBundlesRequest.getDeliveries(), hasItem(hasProperty("isMarketBrandedPickup", is(false))));

        CheckoutContextHolder.getExperiments().remove(Experiments.CASHBACK_DELIVERY_PROMOTION_REVERSED_EXP);
    }

    @Test
    void shouldCreateRequestWithoutMarketBrandedDeliveryWhenReversedExperimentWithToggle() {
        checkouterProperties.setEnabledBrandedPickupBoostReversedExperiment(true);

        var discountRequest = requestFactory.buildDiscountWithBundlesRequest(
                MultiCartProvider.single(OrderProvider.orderBuilder()
                        .label("some label")
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .configure(OrderItemProvider::applyDefaults)
                                .configure(offer(FEED, FIRST_OFFER))
                                .configure(price(10000))
                                .configure(b -> b.digital(true))
                                .count(1)
                        )
                        .deliveryBuilder(DeliveryProvider
                                .yandexDelivery()
                                .pickup(true))
                ),
                loyaltyContext(ReportPromoDiscount.builder()
                        .feedOfferId(FeedOfferId.from(FEED, FIRST_OFFER))
                        .reportPromoType(PRICE_DROP_AS_YOU_SHOP)
                        .discount(BigDecimal.valueOf(2000))
                        .promoDetails(PromoDetails.builder()
                                .promoKey(PROMO_KEY)
                                .promoType(PRICE_DROP_AS_YOU_SHOP.getCode())
                                .build())
                        .build())
        );

        assertThat(discountRequest.getOrders(), not(empty()));
        var orderWithBundlesRequest = Iterables.getOnlyElement(discountRequest.getOrders());
        assertThat(orderWithBundlesRequest.getDeliveries(), hasItem(hasProperty("isMarketBrandedPickup", is(true))));

        CheckoutContextHolder.getExperiments().remove(Experiments.CASHBACK_DELIVERY_PROMOTION_REVERSED_EXP);
    }

    @Nonnull
    public static Function<OrderItemBuilder, OrderItemBuilder> offer(
            @Nonnull Number feedId,
            @Nonnull String offer
    ) {
        return b -> b.feedId(feedId)
                .offer(offer)
                .shopSku(offer)
                .marketSku(offer.hashCode());
    }

    @Nonnull
    public static Function<OrderItemBuilder, OrderItemBuilder> price(@Nonnull Number number) {
        return b -> b.price(number)
                .reportPrice(number)
                .clientPrice(number);
    }

    private LoyaltyContext loyaltyContext(ReportPromoDiscount... discounts) {
        return new LoyaltyContext(
                Map.of(),
                Map.of(),
                Currency.RUR,
                Arrays.stream(discounts)
                        .map(d -> FoundOfferBuilder.create()
                                .feedId(d.getFeedOfferId().getFeedId())
                                .offerId(d.getFeedOfferId().getId())
                                .promoDetails(d.getPromoDetails())
                                .promoKey(d.getPromoDetails().getPromoKey())
                                .promoType(d.getPromoDetails().getPromoType())
                                .build())
                        .collect(Collectors.toUnmodifiableMap(FoundOffer::getFeedOfferId,
                                Function.identity(), (e1, e2) -> e1)),
                Arrays.stream(discounts)
                        .collect(Collectors.groupingBy(ReportPromoDiscount::getFeedOfferId,
                                Collectors.toUnmodifiableSet())),
                Collections.emptyMap(), null, null
        );
    }

    private LoyaltyContext loyaltyContextWithMultiPromo(ReportPromoDiscount... discounts) {
        return new LoyaltyContext(
                Map.of(),
                Map.of(),
                Currency.RUR,
                Arrays.stream(discounts)
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        ReportPromoDiscount::getFeedOfferId,
                                        discount -> {
                                            OfferPromo promo = new OfferPromo();
                                            promo.setPromoMd5(discount.getPromoDetails().getPromoKey());
                                            promo.setPromoType(discount.getPromoDetails().getPromoType());
                                            promo.setPromoDetails(discount.getPromoDetails());
                                            return FoundOfferBuilder.create()
                                                    .feedId(discount.getFeedOfferId().getFeedId())
                                                    .offerId(discount.getFeedOfferId().getId())
                                                    .promos(Collections.singletonList(promo))
                                                    .build();
                                        },
                                        (offer1, offer2) -> {
                                            List<OfferPromo> allPromos = new ArrayList<>();
                                            if (CollectionUtils.isNotEmpty(offer1.getPromos())) {
                                                allPromos.addAll(offer1.getPromos());
                                            }
                                            if (CollectionUtils.isNotEmpty(offer2.getPromos())) {
                                                allPromos.addAll(offer2.getPromos());
                                            }
                                            offer1.setPromos(allPromos);
                                            return offer1;
                                        })
                        ),
                Arrays.stream(discounts)
                        .collect(
                                Collectors.groupingBy(
                                        ReportPromoDiscount::getFeedOfferId,
                                        Collectors.toUnmodifiableSet()
                                )
                        ),
                Collections.emptyMap(), null, null
        );
    }
}
