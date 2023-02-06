package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackRegionSettingsTest;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NOT_SUITABLE_COIN;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.LARGE_SIZED_CART;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.THRESHOLD_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.NO_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_PLUS;
import static ru.yandex.market.loyalty.core.test.BlackboxUtils.mockBlackbox;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.DEFAULT_DELIVERY_PRICE;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.SELECTED;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withRegion;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContextDto;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MOSCOW_REGION;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.dropship;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@Ignore("Disabled after project MARKETPROJECT-6048 was done")
@TestFor(DiscountController.class)
public class DiscountControllerLargeSizedCartDeliveryTest extends MarketLoyaltyBackRegionSettingsTest {
    private static final BigDecimal HEAVY_WEIGHT = BigDecimal.valueOf(21.0);
    private static final String firstCartId = "firstCartId";
    private static final String secondCartId = "secondCartId";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void shouldNotReturnFreeDeliveryIfLargeSizedCartV2() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withCartId(firstCartId)
                                .withWeight(HEAVY_WEIGHT)
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.TEN),
                                        price(BigDecimal.valueOf(400))
                                )
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build(),
                        orderRequestBuilder()
                                .withCartId(secondCartId)
                                .withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withWeight(BigDecimal.ONE)
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build()
                )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertPayForLargeSizedCartAndRestIsFree(discountResponse);
    }

    @Test
    public void shouldNotReturnFreeDeliveryIfLargeSizedCartV3() {
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withWeight(HEAVY_WEIGHT)
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.TEN),
                                        price(BigDecimal.valueOf(400))
                                )
                                .withDeliveries(courierDelivery(SELECTED.andThen(withRegion(MOSCOW_REGION))))
                                .build(),
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withWeight(BigDecimal.ONE)
                                .withDeliveries(courierDelivery(SELECTED.andThen(withRegion(MOSCOW_REGION))))
                                .build()
                )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertPayForLargeSizedCartAndRestIsFree(discountResponse);
    }

    @Test
    public void shouldBeCalculatedAsKgt() {
        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(
                        itemKey(DEFAULT_ITEM_KEY),
                        price(5000)
                )
                .withLargeSize(true)
                .build();

        MultiCartWithBundlesDiscountResponse discountResponse =
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order)
                                .build()
                );

        assertEquals(NO_FREE_DELIVERY, discountResponse.getFreeDeliveryStatus());
        assertEquals(LARGE_SIZED_CART, discountResponse.getFreeDeliveryReason());
    }

    @Test
    public void shouldNotReturnFreeDeliveryIfLargeSizedCartAndDropshipV2() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withCartId(firstCartId)
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withWeight(HEAVY_WEIGHT)
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build(),
                        orderRequestBuilder()
                                .withCartId(secondCartId)
                                .withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        dropship(),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withWeight(BigDecimal.ONE)
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build()
                )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertPayForLargeSizedCartAndRestIsFree(discountResponse);
    }

    @Test
    public void shouldNotReturnFreeDeliveryIfLargeSizedCartAndDropshipV3() {
        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withWeight(HEAVY_WEIGHT)
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build(),
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        dropship(),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withWeight(BigDecimal.ONE)
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build()
                )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertPayForLargeSizedCartAndRestIsFree(discountResponse);
    }

    @Test
    public void shouldReturnUnusedDeliveryCoinIfLargeSizedCart() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
        );

        CoinKey deliveryCoinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withCartId(firstCartId).withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(BigDecimal.valueOf(2000))
                        )
                                .withWeight(HEAVY_WEIGHT)
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build(),
                        orderRequestBuilder()
                                .withCartId(secondCartId).withOrderItem(
                                itemKey(ANOTHER_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(BigDecimal.valueOf(2000))
                        )
                                .withWeight(BigDecimal.ONE)
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build()
                )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .withCoins(deliveryCoinKey)
                        .build()
        );


        assertThat(
                discountResponse.getCoinErrors(),
                contains(
                        allOf(
                                hasProperty("coin", equalTo(new IdObject(deliveryCoinKey.getId()))),
                                hasProperty(
                                        "error",
                                        hasProperty("code", equalTo(NOT_SUITABLE_COIN.toString()))
                                )
                        )
                )
        );
    }

    @Test
    public void shouldNotReturnFreeDeliveryForAliceWithLargeSizedCart() {
        enableAlicePromo();
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withCartId(firstCartId).withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(BigDecimal.valueOf(2000))
                        )
                                .withWeight(HEAVY_WEIGHT)
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build()
                ).withOperationContext(
                        OperationContextFactory.emptyBuilder()
                                .withClientDevice(UsageClientDeviceType.ALICE)
                                .buildOperationContextDto()
                )
                        .build()
        );
        disableAlicePromo();

        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(LARGE_SIZED_CART));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
    }

    @Test
    public void shouldNotReturnFreeDeliveryForYPlusWithLargeSizedCart() {
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withCartId(firstCartId).withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(BigDecimal.valueOf(1000))
                        )
                                .withWeight(HEAVY_WEIGHT)
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build()
                )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );
        disableAlicePromo();

        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(LARGE_SIZED_CART));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
    }

    @Test
    public void shouldNotReturnFreeDeliveryForSpecialAddress() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withCartId(firstCartId)
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(2000))
                                )
                                .withWeight(HEAVY_WEIGHT)
                                .build()
                )
                        .withOperationContext(uidOperationContextDto(DEFAULT_UID))
                        .build()
        );

        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(LARGE_SIZED_CART));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
    }

    @Test
    public void shouldNotReturnLargeDimensionCartForShopDeliveryItemsV2() {
        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestBuilder.builder(
                        orderRequestBuilder()
                                .withCartId(firstCartId)
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(200))
                                )
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build(),
                        orderRequestBuilder()
                                .withCartId(secondCartId)
                                .withOrderItem(
                                        itemKey(ANOTHER_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(500)),
                                        dropship()
                                )
                                .withWeight(HEAVY_WEIGHT)
                                .withDeliveries(courierDelivery(withRegion(MOSCOW_REGION)))
                                .build()
                )
                        .build()
        );
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), comparesEqualTo(BigDecimal.valueOf(2299)));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(THRESHOLD_FREE_DELIVERY));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(WILL_BE_FREE_WITH_MORE_ITEMS));
    }

    private void enableAlicePromo() {
        configurationService.set(ConfigurationService.ALICE_PROMO_START, LocalDateTime.now().minusDays(1).toString());
        configurationService.set(ConfigurationService.ALICE_PROMO_END, LocalDateTime.now().plusDays(1).toString());
    }

    private void disableAlicePromo() {
        configurationService.set(ConfigurationService.ALICE_PROMO_START, "");
        configurationService.set(ConfigurationService.ALICE_PROMO_END, "");
    }

    private static void assertPayForLargeSizedCartAndRestIsFree(MultiCartDiscountResponse discountResponse) {
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), is(nullValue()));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(LARGE_SIZED_CART));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("cartId", equalTo(firstCartId)),
                        hasProperty("deliveries", contains(
                                hasProperty("promos", is(empty()))
                        ))
                ),
                allOf(
                        hasProperty("cartId", equalTo(secondCartId)),
                        hasProperty("deliveries", contains(
                                hasProperty("promos", contains(
                                        allOf(
                                                hasProperty("promoType", equalTo(PromoType.FREE_DELIVERY_FOR_LSC)),
                                                hasProperty("discount", comparesEqualTo(DEFAULT_DELIVERY_PRICE))
                                        ))
                                ))
                        )
                )
                )
        );
    }

    private static void assertPayForLargeSizedCartAndRestIsFree(MultiCartWithBundlesDiscountResponse discountResponse) {
        assertThat(discountResponse.getPriceLeftForFreeDelivery(), is(nullValue()));
        assertThat(discountResponse.getFreeDeliveryReason(), equalTo(LARGE_SIZED_CART));
        assertThat(discountResponse.getFreeDeliveryStatus(), equalTo(NO_FREE_DELIVERY));
        assertThat(discountResponse.getOrders(), containsInAnyOrder(
                allOf(
                        hasProperty("deliveries", contains(
                                hasProperty("promos", is(empty()))
                        ))
                ),
                allOf(
                        hasProperty("deliveries", contains(
                                hasProperty("promos", contains(
                                        allOf(
                                                hasProperty("promoType", equalTo(PromoType.FREE_DELIVERY_FOR_LSC)),
                                                hasProperty("discount", comparesEqualTo(DEFAULT_DELIVERY_PRICE))
                                        ))
                                ))
                        )
                )
                )
        );
    }

}
