package ru.yandex.market.loyalty.back.controller.discount;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.discount.MultiCartDiscountResponse;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.config.properties.AntifraudExecutorProperties;
import ru.yandex.market.loyalty.core.dao.antifraud.FraudCoinDisposeQueueDao;
import ru.yandex.market.loyalty.core.mock.AntiFraudMockUtil;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.antifraud.AntiFraudService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.MatcherUtils.coinHasKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(DiscountController.class)
public class DiscountControllerAntifraudTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private AntiFraudMockUtil antiFraudMockUtil;
    @Autowired
    private FraudCoinDisposeQueueDao fraudCoinDisposeQueueDao;
    @Autowired
    private AntiFraudService antifraudService;
    @Autowired
    private AntifraudExecutorProperties executorProps;

    @Test
    public void shouldBlockUsedCoinCalc() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        CoinKey coinKey1 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        CoinKey coinKey2 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        antiFraudMockUtil.coinWasUsed(coinKey1);

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey1, coinKey2).build()
        );

        assertThat(discountResponse.getCoinErrors(), contains(
                hasProperty("error",
                        hasProperty("code",
                                equalTo(MarketLoyaltyErrorCode.FRAUD_COIN_ERROR.name())
                        )
                )
        ));
        assertThat(discountResponse.getOrders(), contains(
                hasProperty(
                        "items",
                        contains(
                                hasProperty(
                                        "promos",
                                        contains(
                                                hasProperty("usedCoin", equalTo(new IdObject(coinKey2.getId())))
                                        )
                                )
                        )
                ))
        );

        antifraudService.awaitAllExecutors(executorProps);

        assertThat(fraudCoinDisposeQueueDao.getReadyInQueueUidsWithRecordIds(10).keySet(), hasSize(1));
    }

    @Test
    public void shouldBlockUsedCoinSpend() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        CoinKey coinKey1 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        CoinKey coinKey2 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        antiFraudMockUtil.coinWasUsed(coinKey1);

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(coinKey1, coinKey2).build()
        );

        assertThat(discountResponse.getCoinErrors(), contains(
                hasProperty("error",
                        hasProperty("code",
                                equalTo(MarketLoyaltyErrorCode.FRAUD_COIN_ERROR.name())
                        )
                )
        ));
        assertThat(discountResponse.getOrders(), contains(
                hasProperty(
                        "items",
                        contains(
                                hasProperty(
                                        "promos",
                                        contains(
                                                hasProperty("usedCoin", coinHasKey(coinKey2))
                                        )
                                )
                        )
                ))
        );

        antifraudService.awaitAllExecutors(executorProps);

        assertThat(fraudCoinDisposeQueueDao.getReadyInQueueUidsWithRecordIds(10).keySet(), hasSize(1));
    }

    @Test
    public void shouldBlockAllFraudUserCoinsCalc() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        CoinKey coinKey1 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        CoinKey coinKey2 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        antiFraudMockUtil.userInBlacklist();

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                builder(order).withCoins(coinKey1, coinKey2).build()
        );

        assertThat(discountResponse.getCoinErrors(), contains(
                hasProperty("error",
                        hasProperty("code",
                                equalTo(MarketLoyaltyErrorCode.FRAUD_COIN_ERROR.name())
                        )
                ),
                hasProperty("error",
                        hasProperty("code",
                                equalTo(MarketLoyaltyErrorCode.FRAUD_COIN_ERROR.name())
                        )
                )
        ));

        antifraudService.awaitAllExecutors(executorProps);

        assertThat(fraudCoinDisposeQueueDao.getReadyInQueueUidsWithRecordIds(10).get(DEFAULT_UID), hasSize(2));
    }

    @Test
    public void shouldBlockAllFraudUserCoinsSpend() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        CoinKey coinKey1 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        CoinKey coinKey2 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        antiFraudMockUtil.userInBlacklist();

        OrderWithDeliveriesRequest order = orderRequestBuilder().withOrderItem().build();

        MultiCartDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                builder(order).withCoins(coinKey1, coinKey2).build()
        );

        assertThat(discountResponse.getCoinErrors(), contains(
                hasProperty("error",
                        hasProperty("code",
                                equalTo(MarketLoyaltyErrorCode.FRAUD_COIN_ERROR.name())
                        )
                ),
                hasProperty("error",
                        hasProperty("code",
                                equalTo(MarketLoyaltyErrorCode.FRAUD_COIN_ERROR.name())
                        )
                )
        ));

        antifraudService.awaitAllExecutors(executorProps);

        assertThat(fraudCoinDisposeQueueDao.getReadyInQueueUidsWithRecordIds(10).get(DEFAULT_UID), hasSize(2));
    }
}
