package ru.yandex.market.loyalty.core.service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.UnhandledException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.services.auth.blackbox.UserInfo;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.core.config.Blackbox;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoinSearchRequest;
import ru.yandex.market.loyalty.core.model.coin.EmissionRestriction;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.antifraud.AsyncUserRestrictionResult;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.mail.AlertNotificationService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.SameCollection;
import ru.yandex.market.monitoring.MonitoringStatus;

import static java.sql.Timestamp.valueOf;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.CATEGORY_ID;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.rule.RuleType.CATEGORY_FILTER_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.service.coin.CoinLifecycleService.BIND_COIN_FRAUD_THRESHOLD;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultNoAuth;
import static ru.yandex.market.loyalty.core.utils.MonitorHelper.assertMonitor;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_COINS_LIMIT;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class CoinServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private SqlMonitorService sqlMonitorService;
    @Autowired
    @Blackbox
    HttpClient httpClient;
    @Autowired
    private AlertNotificationService alertNotificationService;
    @Autowired
    private CoinService coinService;

    @Ignore("https://st.yandex-team.ru/MARKETDISCOUNT-7701 заблокировали функционал групп акций в админке, далее удалим его")
    @Test(expected = MarketLoyaltyException.class)
    public void shouldNotCreateRestrictedGroupCoinIfUserAlreadyHasGroupCoin() {
        Promo promo1 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setPromoGroupId("test")
                        .setEmissionRestriction(EmissionRestriction.ONE_COIN)
        );

        coinService.create.createCoin(promo1, defaultAuth().build());

        List<Coin> coins = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID));
        assertThat(coins, hasSize(1));

        Promo promo2 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setBindOnlyOnce(true)
                        .setPromoGroupId("test")
        );

        coinService.create.createCoin(promo2, defaultAuth().build());
    }

    @Test
    public void shouldCreateUnrestrictedGroupCoinIfUserAlreadyHasGroupCoin() {
        Promo promo1 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setPromoGroupId("test")
        );

        coinService.create.createCoin(promo1, defaultAuth().build());

        List<Coin> coins = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID));
        assertThat(coins, hasSize(1));

        Promo promo2 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setPromoGroupId("test")
        );

        coinService.create.createCoin(promo2, defaultAuth().build());


        List<Coin> coins1 = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID));
        assertThat(coins1, hasSize(2));
    }

    @Test
    public void shouldBindCoinPropsToCreatedCoin() {
        ImmutableSet<String> mskus = ImmutableSet.of("1", "2");
        ImmutableSet<Integer> categoryIds = ImmutableSet.of(1, 2);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .addCoinRule(CATEGORY_FILTER_RULE, CATEGORY_ID, categoryIds)
                        .addCoinRule(MSKU_FILTER_RULE, MSKU_ID, mskus)
        );

        coinService.create.createCoin(promo, defaultAuth().build());

        List<Coin> coins = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID));
        assertThat(coins, hasSize(1));

        Coin coin = coins.iterator().next();
        assertTrue(coin.getRulesContainer().hasRule(CATEGORY_FILTER_RULE));
        assertTrue(coin.getRulesContainer().hasRule(MSKU_FILTER_RULE));
        assertThat(
                coin.getRulesContainer().get(MSKU_FILTER_RULE).getParams(MSKU_ID),
                SameCollection.sameCollectionInAnyOrder(mskus)
        );
        assertThat(
                coin.getRulesContainer().get(CATEGORY_FILTER_RULE).getParams(CATEGORY_ID),
                SameCollection.sameCollectionInAnyOrder(categoryIds)
        );
    }

    @Test
    public void shouldSpendEmissionOnCoinCreate() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        coinService.create.createCoin(promo, defaultAuth().build());

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldExpireCoinByDays() {
        clock.setDate(valueOf("2019-01-09 10:00:00"));

        int daysToExpire = 1;
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEndDate(valueOf("2019-01-11 10:00:00"))
                        .setExpiration(ExpirationPolicy.expireByDays(daysToExpire))
        );

        coinService.create.createCoin(promo, defaultAuth().build());

        Coin coin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID)).iterator().next();
        assertEquals(new Date(valueOf("2019-01-09 10:00:00").getTime()), coin.getStartDate());
        assertEquals(new Date(valueOf("2019-01-10 23:59:59").getTime()), coin.getRoundedEndDate());

        assertTrue(coin.isActive(clock));

        clock.spendTime(daysToExpire + 1, DAYS);

        assertFalse(coin.isActive(clock));
        alertNotificationService.processEmailQueue(100);
    }

    @Test
    public void shouldExpireCoinByHours() {
        clock.setDate(valueOf("2019-01-09 10:00:00"));

        int hoursToExpire = 1;
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEndDate(valueOf("2019-01-11 10:00:00"))
                        .setExpiration(ExpirationPolicy.flash(Duration.ofHours(hoursToExpire)))
        );

        coinService.create.createCoin(promo, defaultAuth().build());

        Coin coin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID)).iterator().next();
        assertEquals(new Date(valueOf("2019-01-09 10:00:00").getTime()), coin.getStartDate());
        assertEquals(new Date(valueOf("2019-01-09 11:00:00").getTime()), coin.getRoundedEndDate());

        assertTrue(coin.isActive(clock));

        clock.spendTime(hoursToExpire + 1, HOURS);

        assertFalse(coin.isActive(clock));
        alertNotificationService.processEmailQueue(100);
    }

    @Test
    public void shouldExpireCoinByEndOfPromo() {
        clock.setDate(valueOf("2019-01-09 10:00:00"));

        Date afterYear = valueOf("2020-01-09 10:00:00");
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEndDate(afterYear)
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );

        coinService.create.createCoin(promo, defaultAuth().build());

        Coin coin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID)).iterator().next();
        assertEquals(new Date(valueOf("2019-01-09 10:00:00").getTime()), coin.getStartDate());
        assertEquals(new Date(valueOf("2020-01-09 23:59:59").getTime()), coin.getRoundedEndDate());

        assertTrue(coin.isActive(clock));

        clock.setDate(afterYear);
        clock.spendTime(1, DAYS);

        assertFalse(coin.isActive(clock));
        alertNotificationService.processEmailQueue(100);
    }

    @Test
    public void shouldActivateCoinAfterPromoStart() {
        Date tomorrow = Date.from(clock.instant().plus(1, DAYS));
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setStartDate(tomorrow)
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new);
        assertEquals(tomorrow, coin.getStartDate());

        assertFalse(coin.isActive(clock));

        clock.spendTime(2, DAYS);

        assertTrue(coin.isActive(clock));
    }

    @Ignore // TODO: Fix test blinking in MARKETDISCOUNT-1413
    @Test
    public void shouldNotFireFraudMonitorOnBindDifferentPromos() {
        long uid = 12312313123L;

        Set<String> activationTokens = IntStream.rangeClosed(0, BIND_COIN_FRAUD_THRESHOLD)
                .mapToObj(String::valueOf).collect(Collectors.toSet());

        for (String activationToken : activationTokens) {
            Promo promo = promoManager.createSmartShoppingPromo(
                    PromoUtils.SmartShopping.defaultFixed()
            );
            coinService.create.createCoin(promo, defaultNoAuth(activationToken).build());
        }

        for (String activationToken : activationTokens) {
            coinService.lifecycle.bindCoinsToUser(uid, activationToken);
        }

        assertMonitor(MonitoringStatus.OK, sqlMonitorService.checkDbState());
    }

    @Test
    public void shouldFindCoinByUid() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        List<Coin> coins = coinService.search.searchCoins(Long.toString(DEFAULT_UID), DEFAULT_COINS_LIMIT);

        assertThat(coins, hasSize(1));
        assertEquals(coinKey, coins.get(0).getCoinKey());
    }

    @Test
    public void shouldFindCoinById() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        List<Coin> coins = coinService.search.searchCoins(Long.toString(coinKey.getId()), DEFAULT_COINS_LIMIT);

        assertThat(coins, hasSize(1));
        assertEquals(coinKey, coins.get(0).getCoinKey());
    }

    @Test
    public void shouldFindCoinByOrderId() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        long orderId = 0L;
        CoinKey coinKey = coinService.create.createCoin(promo, orderId, null, defaultAuth().build());

        List<Coin> coins = coinService.search.searchCoins(Long.toString(orderId), DEFAULT_COINS_LIMIT);

        assertThat(coins, hasSize(1));
        assertEquals(coinKey, coins.get(0).getCoinKey());
    }

    @Test
    public void shouldFindCoinByActivationToken() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        String activationToken = "ABCDE";
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth(activationToken).build());

        List<Coin> coins = coinService.search.searchCoins(activationToken, DEFAULT_COINS_LIMIT);

        assertThat(coins, hasSize(1));
        assertEquals(coinKey, coins.get(0).getCoinKey());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void shouldFindCoinByLogin() throws IOException {
        String login = "test@test.ru";
        UserInfo info = new UserInfo();
        setUid(info, DEFAULT_UID);
        setEmail(info, login);

        when(httpClient.execute(any(HttpUriRequest.class),
                ArgumentMatchers.<ResponseHandler<UserInfo>>any((Class) ResponseHandler.class)))
                .thenReturn(info);

        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID).build());

        List<Coin> coins = coinService.search.searchCoins(login, DEFAULT_COINS_LIMIT);

        assertThat(coins, hasSize(1));
        assertEquals(coinKey, coins.get(0).getCoinKey());
    }


    @Test(expected = MarketLoyaltyException.class)
    public void shouldNotBindRestrictedGroupCoinIfUserAlreadyHasGroupCoin() {
        Promo promo1 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setPromoGroupId("test")
                        .setEmissionRestriction(EmissionRestriction.ONE_COIN)
        );

        coinService.create.createCoin(promo1, defaultAuth().build());

        List<Coin> coins = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID));
        assertThat(coins, hasSize(1));

        Promo promo2 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setBindOnlyOnce(true)
                        .setPromoGroupId("test")
        );

        final CoinKey unboundCoin = coinService.create.createCoin(promo2, defaultNoAuth().build());

        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, unboundCoin, false);
    }


    @Test(expected = MarketLoyaltyException.class)
    public void shouldNotBindRestrictedCoinIfUserAlreadyHasCoin() {
        Promo promo1 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setBindOnlyOnce(true)
        );

        coinService.create.createCoin(promo1, defaultAuth().build());

        List<Coin> coins = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID));
        assertThat(coins, hasSize(1));

        final CoinKey unboundCoin = coinService.create.createCoin(promo1, defaultNoAuth().build());

        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, unboundCoin, false);
    }

    @Test
    public void shouldCreateCoinIfUserAlreadyHasCoin() {
        Promo promo1 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setBindOnlyOnce(false)
        );

        coinService.create.createCoinsBatch(
                promo1,
                Collections.singletonList(DEFAULT_UID),
                uid -> "batch_1_" + uid,
                AsyncUserRestrictionResult.empty()
        );
        coinService.create.createCoinsBatch(
                promo1,
                Arrays.asList(DEFAULT_UID, ANOTHER_UID),
                uid -> "batch_2_" + uid,
                AsyncUserRestrictionResult.empty()
        );

        List<Coin> coins = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID));
        assertThat(coins, hasSize(2));
    }


    private static void setUid(UserInfo info, long uid) {
        try {
            Field field = UserInfo.class.getDeclaredField("uid");
            field.setAccessible(true);
            field.set(info, uid);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new UnhandledException(e);
        }
    }

    private static void setEmail(UserInfo info, String login) {
        try {
            Field field = UserInfo.class.getDeclaredField("login");
            field.setAccessible(true);
            field.set(info, login);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new UnhandledException(e);
        }
    }
}
