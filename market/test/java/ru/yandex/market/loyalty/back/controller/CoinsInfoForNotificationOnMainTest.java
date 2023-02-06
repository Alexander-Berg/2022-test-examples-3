package ru.yandex.market.loyalty.back.controller;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.coin.CoinInfoForNotificationOnMain;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy.expireByDays;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.FIRST_CHILD_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultPercent;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(CoinsController.class)
public class CoinsInfoForNotificationOnMainTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;

    @Test
    public void shouldReturnInfoForOneFixedCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.valueOf(300L)));

        coinService.create.createCoin(promo, defaultAuth().build());

        assertEquals(
                "У вас есть Беру Бонус на 300\u20BD. Успейте его потратить",
                marketLoyaltyClient.getCoinsInfoForNotificationOnMain(DEFAULT_UID).getText()
        );
    }

    @Test
    public void shouldReturnInfoForOneFixedCoinWithMinOrderTotal() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed(BigDecimal.valueOf(300L))
                        .addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(2000))
        );

        coinService.create.createCoin(promo, defaultAuth().build());

        assertEquals(
                "У вас есть Беру Бонус на 300\u20BD на заказ от 2 000\u20BD. Успейте его потратить",
                marketLoyaltyClient.getCoinsInfoForNotificationOnMain(DEFAULT_UID).getText()
        );
    }

    @Test
    public void shouldReturnInfoForOnePercentCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultPercent(BigDecimal.valueOf(10L))
                        .addCoinRule(UPPER_BOUND_DISCOUNT_BASE_RULE, MAX_ORDER_TOTAL, BigDecimal.valueOf(20000))
        );

        coinService.create.createCoin(promo, defaultAuth().build());

        assertEquals(
                "У вас есть Беру Бонус на 10% на заказ до 20 000\u20BD. Успейте его потратить",
                marketLoyaltyClient.getCoinsInfoForNotificationOnMain(DEFAULT_UID).getText()
        );
    }

    @Test
    public void shouldReturnInfoForSeveralCoins() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultAuth().build());
        coinService.create.createCoin(promo, defaultAuth().build());

        assertEquals(
                "Не забудьте про Беру Бонусы. Успейте их потратить",
                marketLoyaltyClient.getCoinsInfoForNotificationOnMain(DEFAULT_UID).getText()
        );
    }

    @Test
    public void shouldReturnInfoForAlmostExpiredCoins() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed().setExpiration(expireByDays(30)));

        coinService.create.createCoin(promo, defaultAuth().build());
        clock.spendTime(Duration.ofDays(20));

        coinService.create.createCoin(promo, defaultAuth().build());
        clock.spendTime(Duration.ofDays(7));

        assertEquals(
                "Успейте потратить, некоторые ваши Беру Бонусы сгорят через 3 дня",
                marketLoyaltyClient.getCoinsInfoForNotificationOnMain(DEFAULT_UID).getText()
        );
    }

    @Test
    public void shouldReturnInfoForMostExpiredCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed().setExpiration(expireByDays(30)));

        coinService.create.createCoin(promo, defaultAuth().build());
        clock.spendTime(Duration.ofDays(2));

        coinService.create.createCoin(promo, defaultAuth().build());
        clock.spendTime(Duration.ofDays(25));

        assertEquals(
                "Успейте потратить, некоторые ваши Беру Бонусы сгорят через 3 дня",
                marketLoyaltyClient.getCoinsInfoForNotificationOnMain(DEFAULT_UID).getText()
        );
    }

    @Test
    public void shouldReturnNormalInfoForCoinWithEightDayBeforeExpiration() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed().setExpiration(expireByDays(30)));

        coinService.create.createCoin(promo, defaultAuth().build());
        clock.spendTime(Duration.ofDays(22));

        assertEquals(
                "У вас есть Беру Бонус на 300\u20BD. Успейте его потратить",
                marketLoyaltyClient.getCoinsInfoForNotificationOnMain(DEFAULT_UID).getText()
        );
    }

    @Test
    public void shouldNotReturnInfoForSingleCategoryCoin() {
        Promo categoryPromo = promoManager.createSmartShoppingPromo(defaultFixed()
                .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, FIRST_CHILD_CATEGORY_ID)
        );

        coinService.create.createCoin(categoryPromo, defaultAuth().build());

        CoinInfoForNotificationOnMain coinsInfo = marketLoyaltyClient.getCoinsInfoForNotificationOnMain(DEFAULT_UID);
        assertThat(coinsInfo, hasNoCoins());
    }

    @Test
    public void shouldReturnInfoAsSeveralCoinsIfOneCoinIsCategoryAndAnotherIsStandard() {
        Promo standardPromo = promoManager.createSmartShoppingPromo(defaultFixed());

        Promo categoryPromo = promoManager.createSmartShoppingPromo(defaultFixed()
                .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, FIRST_CHILD_CATEGORY_ID)
        );

        coinService.create.createCoin(categoryPromo, defaultAuth().build());
        coinService.create.createCoin(standardPromo, defaultAuth().build());

        assertEquals(
                "Не забудьте про Беру Бонусы. Успейте их потратить",
                marketLoyaltyClient.getCoinsInfoForNotificationOnMain(DEFAULT_UID).getText()
        );
    }

    @Test
    public void shouldReturnInfoAsSeveralCoinsIfHasTwoCategoryCoins() {
        Promo categoryPromo = promoManager.createSmartShoppingPromo(defaultFixed()
                .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, FIRST_CHILD_CATEGORY_ID)
        );

        coinService.create.createCoin(categoryPromo, defaultAuth().build());
        coinService.create.createCoin(categoryPromo, defaultAuth().build());

        assertEquals(
                "Не забудьте про Беру Бонусы. Успейте их потратить",
                marketLoyaltyClient.getCoinsInfoForNotificationOnMain(DEFAULT_UID).getText()
        );
    }

    private static Matcher<CoinInfoForNotificationOnMain> hasNoCoins() {
        return allOf(
                hasProperty("text", isEmptyString()),
                hasProperty("noCoins", equalTo(true))
        );
    }
}
