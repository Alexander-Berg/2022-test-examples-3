package ru.yandex.market.loyalty.back.controller.discount;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.report.model.Color;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.coin.CoinsForCart;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.OrderItemsRequest;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.constants.DeliveryPartnerType;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.ReportMockUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.DROPSHIP_ONLY;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.atSupplierWarehouse;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.dropship;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(DiscountController.class)
public class DiscountControllerWhiteMarketTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ReportMockUtils reportMockUtils;

    @Test
    public void shouldReturnBlueCoinsForWhiteMarketCart() throws IOException, InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        mockReport(DEFAULT_ITEM_KEY, 1, CoreMarketPlatform.WHITE);
        CoinsForCart response = marketLoyaltyClient
                .getCoinsForCartV2(
                        DEFAULT_UID,
                        213L,
                        false,
                        MarketPlatform.WHITE,
                        UsageClientDeviceType.DESKTOP,
                        new OrderItemsRequest(
                                Collections.singletonList(makeItem(DEFAULT_ITEM_KEY, 1))
                        )
                );
        assertThat(
                response.getApplicableCoins(),
                hasSize(1)
        );
    }


    @Test
    public void shouldReturnBlueCoinsForWhiteMarket() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        assertThat(marketLoyaltyClient.getCoinsForUser(DEFAULT_UID, 5, MarketPlatform.WHITE).getCoins(), hasSize(1));
    }

    @Test
    public void shouldUseCoinOnCalcForWhitePlatform() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.TEN));

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        assertThat(
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(2000)),
                                                atSupplierWarehouse(false),
                                                dropship()
                                        )
                                        .withPlatform(MarketPlatform.WHITE)
                                        .build())
                                .withCoins(coinKey)
                                .withPlatform(MarketPlatform.WHITE)
                                .build()
                ),
                allOf(
                        hasProperty(
                                "coinErrors",
                                is(empty())
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        hasProperty(
                                                "items",
                                                contains(
                                                        hasProperty(
                                                                "promos",
                                                                contains(
                                                                        hasProperty(
                                                                                "discount",
                                                                                comparesEqualTo(BigDecimal.TEN)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @Ignore("Disabled after project MARKETPROJECT-6048 was done")
    @Test
    public void shouldLetToUseCoinsAndForbidDeliveryCoins() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.TEN));
        Promo deliveryPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
        );

        CoinKey usualCoinKey = coinService.create.createCoin(promo, defaultAuth().build());
        CoinKey deliveryCoinKey = coinService.create.createCoin(deliveryPromo, defaultAuth().build());

        MultiCartWithBundlesDiscountResponse response = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder.builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderItem(
                                        itemKey(DEFAULT_ITEM_KEY),
                                        quantity(BigDecimal.ONE),
                                        price(BigDecimal.valueOf(2000)),
                                        atSupplierWarehouse(false),
                                        dropship()
                                )
                                .withPlatform(MarketPlatform.WHITE)
                                .build())
                        .withCoins(usualCoinKey, deliveryCoinKey)
                        .withPlatform(MarketPlatform.WHITE)
                        .build()
        );

        assertThat(
                response,
                allOf(
                        hasProperty(
                                "coinErrors",
                                is(hasSize(1))
                        ),
                        hasProperty(
                                "freeDeliveryReason",
                                equalTo(DROPSHIP_ONLY)
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        hasProperty(
                                                "items",
                                                contains(
                                                        hasProperty(
                                                                "promos",
                                                                contains(
                                                                        hasProperty(
                                                                                "discount",
                                                                                comparesEqualTo(BigDecimal.TEN)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }


    @Test
    public void shouldUseFixedCoinOnCalcForPartnerItemsOnWhitePlatform() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.TEN));

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        assertThat(
                marketLoyaltyClient.calculateDiscount(
                        DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderItem(
                                                itemKey(DEFAULT_ITEM_KEY),
                                                quantity(BigDecimal.ONE),
                                                price(BigDecimal.valueOf(2000)),
                                                atSupplierWarehouse(false),
                                                dropship()
                                        )
                                        .withPlatform(MarketPlatform.WHITE)
                                        .build())
                                .withCoins(coinKey)
                                .withPlatform(MarketPlatform.WHITE)
                                .build()
                ),
                allOf(
                        hasProperty(
                                "coinErrors",
                                is(empty())
                        ),
                        hasProperty(
                                "orders",
                                contains(
                                        hasProperty(
                                                "items",
                                                contains(
                                                        hasProperty(
                                                                "promos",
                                                                contains(
                                                                        hasProperty(
                                                                                "discount",
                                                                                comparesEqualTo(BigDecimal.TEN)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private void mockReport(ItemKey itemKey, int category, CoreMarketPlatform platform) throws IOException, InterruptedException {
        FoundOffer fo = new FoundOffer();
        fo.setFeedId(itemKey.getFeedId());
        fo.setShopOfferId(itemKey.getOfferId());
        fo.setPrice(BigDecimal.valueOf(100));
        fo.setHyperCategoryId(category);
        fo.setCargoTypes(Collections.emptySet());
        fo.setWeight(BigDecimal.ONE);
        fo.setRgb(Color.findByValue(platform.getApiPlatform().getCode()));
        fo.setDeliveryPartnerTypes(Collections.singletonList(DeliveryPartnerType.SHOP.getCode()));

        reportMockUtils.mockReportService(fo);
    }

    private static OrderItemsRequest.Item makeItem(ItemKey defaultItemKey, int count) {
        return OrderItemsRequest.Item.builder()
                .setFeedId(defaultItemKey.getFeedId())
                .setOfferId(defaultItemKey.getOfferId())
                .setCount(count)
                .build();
    }
}
