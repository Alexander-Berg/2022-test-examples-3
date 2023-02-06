package ru.yandex.market.loyalty.back.controller.discount;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.coin.EmissionRestriction;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.BindingConflictException;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.lightweight.ExceptionUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;


public class DiscountControllerOneActiveCoinEmissionModeTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void shouldNotAllowCreateSecondActiveCoin() {
        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEmissionRestriction(EmissionRestriction.ONE_ACTIVE_COIN)
        );
        try {
            coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        } catch (BindingConflictException e) {
            throw new IllegalStateException(e);
        }
        assertThrows(MarketLoyaltyException.class,
                () -> coinService.create.createCoin(smartShoppingPromo, defaultAuth().build()));
    }

    @Test
    public void shouldAllowCreateSecondCoinAfterFirstCoinUsed() {
        clock.useRealClock();

        Promo smartShoppingPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEmissionRestriction(EmissionRestriction.ONE_ACTIVE_COIN)
        );
        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithBundlesRequest order1 = orderRequestWithBundlesBuilder()
                .withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();
        OrderWithBundlesRequest order2 = orderRequestWithBundlesBuilder()
                .withOrderItem(itemKey(ANOTHER_ITEM_KEY)).build();

        marketLoyaltyClient.spendDiscount(
                builder(order1, order2).withCoins(coinKey).build()
        );

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);
        assertThat(coin.getStatus(), equalTo(CoreCoinStatus.USED));

        CoinKey coin1 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());
        assertThat(coin, not(equalTo(coin1)));
    }

    @SuppressWarnings("CatchMayIgnoreException")
    @Repeat(5)
    @Test
    public void shouldPreventRaceConditionOnCoinCreationWithOneActiveCoinEmissionRestrictionType() throws Exception {
        clock.useRealClock();

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setEmissionRestriction(EmissionRestriction.ONE_ACTIVE_COIN));

        testConcurrency((cpuCount) ->
                Stream.generate((Supplier<ExceptionUtils.RunnableWithException<Exception>>) () -> () -> {
                            try {
                                coinService.create.createCoin(promo, defaultAuth().build());
                            } catch (BindingConflictException|MarketLoyaltyException e) {

                            }
                        }).limit(cpuCount - 1)
                        .collect(Collectors.toList())
        );

        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject("SELECT count(*) FROM discount", Integer.class));
    }

}
