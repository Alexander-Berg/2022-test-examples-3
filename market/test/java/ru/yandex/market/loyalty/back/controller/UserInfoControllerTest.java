package ru.yandex.market.loyalty.back.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.TakeOutResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.CoinRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(UserInfoController.class)
public class UserInfoControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;


    @Test
    public void shouldReturnTakeoutInfo() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, CoinRequestUtils.defaultNoAuth().build());

        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, DEFAULT_MUID, promo.getPlatform());

        marketLoyaltyClient.spendDiscount(
                builder(orderRequestBuilder().withOrderItem().build()).withCoins(coinKey).build());

        TakeOutResponse takeout = marketLoyaltyClient.takeout(DEFAULT_UID, System.currentTimeMillis(),
                MarketPlatform.BLUE);

        assertThat(takeout.getCoins(), contains(
                hasProperty("id", equalTo(coinKey.getId()))
        ));

        assertThat(takeout.getCoinsHistory(), containsInAnyOrder(
                hasProperty("recordType", equalTo(DiscountHistoryRecordType.CREATION)),
                hasProperty("recordType", equalTo(DiscountHistoryRecordType.BINDING)),
                hasProperty("recordType", equalTo(DiscountHistoryRecordType.USAGE))
        ));
    }
}
