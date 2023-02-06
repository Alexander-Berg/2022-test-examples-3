package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CashbackOptionsRequest;
import ru.yandex.market.loyalty.api.model.CashbackPromoRequest;
import ru.yandex.market.loyalty.api.model.CashbackRequest;
import ru.yandex.market.loyalty.api.model.ItemCashbackRequest;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.OrderCashbackRequest;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NO_ORDER_ID;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.FIRST_CHILD_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.cashbackPromo;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

@TestFor(DiscountController.class)
public class DiscountControllerValidationTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final ItemKey OFFER_KEY = ItemKey.ofFeedOffer(368604L, "6230");

    @Test
    public void noPrice() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(OFFER_KEY),
                        quantity(BigDecimal.valueOf(2)),
                        price(null)
                ).build();

        MarketLoyaltyError error = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).build())
        ).getModel();
        assertEquals(MarketLoyaltyErrorCode.OTHER_ERROR.name(), error.getCode());
        assertThat(
                error.getMessage(),
                containsString("orders[0].items[0].price")
        );
        assertNull(error.getUserMessage());
    }

    @Test
    public void negativePrice() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(OFFER_KEY),
                        quantity(BigDecimal.valueOf(2)),
                        price(BigDecimal.valueOf(-100))
                ).build();

        MarketLoyaltyError error = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).build())
        ).getModel();
        assertThat(error.getMessage(), containsString("orders[0].items[0].price"));
    }

    @Test
    public void wrongQuantity() {
        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem(
                itemKey(OFFER_KEY),
                quantity(BigDecimal.ZERO),
                price(BigDecimal.valueOf(100))
        ).build();

        MarketLoyaltyError error = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).build())
        ).getModel();
        assertThat(error.getMessage(), containsString("orders[0].items[0].quantity"));
    }

    @Test
    public void emptyOfferId() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderItem(
                        itemKey(ItemKey.ofFeedOffer(1L, " ")),
                        quantity(BigDecimal.valueOf(2)),
                        price(BigDecimal.valueOf(100))
                ).build();

        MarketLoyaltyError error = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).build())
        ).getModel();
        assertThat(error.getMessage(), containsString("orders[0].items[0].offerId"));
    }

    @Test
    public void shouldSerializeCashbackPromoRequestsOnCashbackOptions() {
        marketLoyaltyClient.calculateCashbackProfiles(
                DEFAULT_UID,
                100,
                new CashbackOptionsRequest(
                        Collections.singletonList(new OrderCashbackRequest(
                                "DEFAULT_CART_ID",
                                1L,
                                null,
                                Collections.singletonList(
                                        createItemCashbackRequest(
                                                new CashbackPromoRequest(
                                                        "test_promo",
                                                        BigDecimal.TEN,
                                                        null,
                                                        null,
                                                        null,
                                                        Collections.singletonList("test"),
                                                        null,
                                                        null,
                                                        null,
                                                        null
                                                )
                                        )
                                ),
                                null
                        )),
                        UsageClientDeviceType.DESKTOP,
                        null
                )
        );

    }

    @Test
    public void shouldSerializeCashbackPromosOnCalc() {
        DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder()
                        .withOrderItem(
                                itemKey(DEFAULT_ITEM_KEY),
                                quantity(BigDecimal.ONE),
                                price(BigDecimal.valueOf(2000)),
                                cashbackPromo(
                                        new CashbackPromoRequest(
                                                "test",
                                                BigDecimal.valueOf(10),
                                                null,
                                                null,
                                                null,
                                                Arrays.asList("testThreshold1", "testThreshold2"),
                                                null,
                                                null,
                                                null,
                                                null
                                        ))
                        )
                        .withPlatform(MarketPlatform.WHITE)
                        .build())
                .withPlatform(MarketPlatform.WHITE)
                .build();
    }

    @Test
    public void nullCartId() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withCartId(null)
                .withOrderItem(
                        itemKey(OFFER_KEY),
                        quantity(BigDecimal.valueOf(2)),
                        price(BigDecimal.valueOf(100))
                ).build();

        assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.calculateDiscount(DiscountRequestBuilder.builder(order).build())
        );
    }

    @Test
    public void emptyOrderId() {
        OrderWithDeliveriesRequest order = orderRequestBuilder()
                .withOrderId(" ")
                .withOrderItem(
                        itemKey(OFFER_KEY),
                        quantity(BigDecimal.valueOf(2)),
                        price(BigDecimal.valueOf(100))
                ).build();

        MarketLoyaltyError error = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.spendDiscount(DiscountRequestBuilder.builder(order).build())
        ).getModel();
        assertEquals(NO_ORDER_ID.name(), error.getCode());
    }

    @NotNull
    private ItemCashbackRequest createItemCashbackRequest(CashbackPromoRequest... requests) {
        return new ItemCashbackRequest(
                "testOfferId1",
                100L,
                BigDecimal.valueOf(1000),
                BigDecimal.ONE,
                false,
                BigDecimal.ZERO,
                FIRST_CHILD_CATEGORY_ID,
                null,
                null,
                100L,
                MARKET_WAREHOUSE_ID,
                false,
                100L,
                "shopId",
                "bundleId",
                new CashbackRequest("123", 1),
                Arrays.asList(requests),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    public void emptyDiscountToken() {
        MarketLoyaltyError error = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.revertDiscount(Collections.singleton(""))
        ).getModel();
        assertEquals(error.getCode(), MarketLoyaltyErrorCode.OTHER_ERROR.name());
        assertThat(error.getMessage(), containsString("discountToken"));
    }

    @Test
    public void nullDiscountToken() {
        MarketLoyaltyError error = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.revertDiscount(Collections.emptySet())
        ).getModel();
        assertEquals(error.getCode(), MarketLoyaltyErrorCode.OTHER_ERROR.name());
        assertEquals("Error while POST request to url http://localhost:123456/discount/revert : \n" +
                "request body [ null ]\n" +
                "response [ org.springframework.web.bind.MissingServletRequestParameterException: " +
                "Required Set parameter 'discountToken' is not present ]", error.getMessage());
    }
}
