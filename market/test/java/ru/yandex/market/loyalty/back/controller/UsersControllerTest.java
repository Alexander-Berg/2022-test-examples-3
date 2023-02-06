package ru.yandex.market.loyalty.back.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.DEFAULT_ACTIVATION_TOKEN;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultNoAuth;
import static ru.yandex.market.loyalty.core.utils.MatcherUtils.coinHasKey;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_SBER_ID_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(UsersController.class)
public class UsersControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private ConfigurationService configurationService;
    private Optional<Long> originalTop500PromoId;

    @Before
    public void setUp() {
        originalTop500PromoId = configurationService.top500PromoId();
    }

    @After
    public void tearDown() {
        configurationService.set(ConfigurationService.TOP500_PROMO_ID, originalTop500PromoId.orElse(null));
    }

    @Test
    public void shouldNotBindCoinToUserThatAlreadyHasSameCoinWhenBindingAllCoins() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(defaultFixed().setBindOnlyOnce(true));

        coinService.create.createCoin(firstPromo, null, null, defaultAuth(DEFAULT_UID).build());
        coinService.create.createCoin(firstPromo, null, null, defaultAuth(DEFAULT_SBER_ID_UID).build());

        marketLoyaltyClient.mergeUsers(DEFAULT_SBER_ID_UID, DEFAULT_UID);
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_SBER_ID_UID), hasSize(1));
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_UID), hasSize(1));
    }

    @Test
    public void shouldBindTwoCoinsToUser() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(defaultFixed());
        Promo secondPromo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey firstCoinKey = coinService.create.createCoin(
                firstPromo, null, null, defaultAuth(DEFAULT_SBER_ID_UID).build());
        CoinKey secondCoinKey = coinService.create.createCoin(
                secondPromo, null, null, defaultAuth(DEFAULT_SBER_ID_UID).build());

        assertThat(
                coinService.search.getCoin(firstCoinKey).orElseThrow(AssertionError::new),
                allOf(
                        hasProperty("status", equalTo(ACTIVE))
                )
        );
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_SBER_ID_UID), hasSize(2));
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_UID), empty());

        marketLoyaltyClient.mergeUsers(DEFAULT_SBER_ID_UID, DEFAULT_UID);

        assertThat(
                marketLoyaltyClient.getCoins(DEFAULT_UID),
                containsInAnyOrder(
                        coinHasKey(firstCoinKey),
                        coinHasKey(secondCoinKey)
                )
        );
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_SBER_ID_UID), empty());
    }

    @Test
    public void shouldBoundAlreadyBoundedCoinToAnotherUserWhenBindingAllCoins() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, null, null, defaultNoAuth().build());

        assertThat(marketLoyaltyClient.bindCoinsToUser(DEFAULT_SBER_ID_UID, DEFAULT_ACTIVATION_TOKEN), hasSize(1));
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_SBER_ID_UID), hasSize(1));
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_UID), empty());

        marketLoyaltyClient.mergeUsers(DEFAULT_SBER_ID_UID, DEFAULT_UID);
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_UID), hasSize(1));
    }
}
