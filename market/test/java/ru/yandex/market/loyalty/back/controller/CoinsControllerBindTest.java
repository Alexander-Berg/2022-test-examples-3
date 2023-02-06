package ru.yandex.market.loyalty.back.controller;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Repeat;
import org.springframework.web.client.HttpStatusCodeException;

import ru.yandex.market.checkout.checkouter.auth.AuthInfo;
import ru.yandex.market.checkout.checkouter.auth.UserInfo;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.coin.BindOrdersCoinResponse;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.UserBlackListDao;
import ru.yandex.market.loyalty.core.model.BlacklistRecord;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.UserBlacklistService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.test.CheckouterMockUtils;
import ru.yandex.market.loyalty.test.TestFor;

import javax.servlet.http.Cookie;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.NOT_COIN_OWNER;
import static ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode.SAME_COIN_ALREADY_BOUND;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.DEFAULT_ACTIVATION_TOKEN;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultNoAuth;
import static ru.yandex.market.loyalty.core.utils.MatcherUtils.coinHasKey;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

@TestFor(CoinsController.class)
public class CoinsControllerBindTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final long DEFAULT_SBER_ID_UID = (1L << 61) - 1L;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private CheckouterClient checkouterClient;
    @Autowired
    private UserBlacklistService userBlacklistService;
    @Autowired
    private UserBlackListDao userBlackListDao;
    @Autowired
    private CheckouterMockUtils checkouterMockUtils;


    @Test
    public void shouldBindCoinToUser() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        assertThat(
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new),
                allOf(
                        hasProperty("status", equalTo(ACTIVE)),
                        hasProperty("requireAuth", equalTo(true))
                )
        );

        assertThat(
                marketLoyaltyClient.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN),
                contains(allOf(
                        coinHasKey(coinKey),
                        hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                        hasProperty("requireAuth", equalTo(false)),
                        hasProperty("activationToken", nullValue())
                ))
        );

        assertEquals(CoreCoinStatus.ACTIVE,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());
    }

    @Test
    public void shouldBindReferralCoinToUser() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        assertThat(
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new),
                allOf(
                        hasProperty("status", equalTo(ACTIVE)),
                        hasProperty("requireAuth", equalTo(true))
                )
        );

        assertThat(
                marketLoyaltyClient.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN),
                contains(allOf(
                        coinHasKey(coinKey),
                        hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                        hasProperty("requireAuth", equalTo(false)),
                        hasProperty("activationToken", nullValue())
                ))
        );

        assertEquals(CoreCoinStatus.ACTIVE,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());
    }

    @Test
    public void shouldBindCoinToUserByMuid() {
        String cookie = DEFAULT_MUID + ":val";
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        assertThat(
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new),
                allOf(
                        hasProperty("status", equalTo(ACTIVE)),
                        hasProperty("requireAuth", equalTo(true))
                )
        );

        when(checkouterClient.doAuth(anyString(), any(UserInfo.class))).thenReturn(new AuthInfo(DEFAULT_MUID, cookie));

        assertThat(
                marketLoyaltyClient.bindCoinsToUserByMuid(DEFAULT_UID, DEFAULT_MUID, "9.9.9.9", "Mozilla", cookie),
                contains(allOf(
                        coinHasKey(coinKey),
                        hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                        hasProperty("requireAuth", equalTo(false)),
                        hasProperty("activationToken", nullValue())
                ))
        );

        assertEquals(CoreCoinStatus.ACTIVE,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());
    }

    @Test
    public void shouldBindCoinToUserByMuidIfSomeCoinsAlreadyBound() {
        String cookie = DEFAULT_MUID + ":val";
        Promo firstPromo = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.valueOf(300)));
        Promo secondPromo = promoManager.createSmartShoppingPromo(defaultFixed(BigDecimal.valueOf(500)));
        coinService.create.createCoin(firstPromo, defaultNoAuth("1").build());
        Coin secondPromoCoin = coinService.search
                .getCoin(
                        coinService.create.createCoin(secondPromo, defaultNoAuth("2").build()))
                .orElseThrow(RuntimeException::new);

        marketLoyaltyClient.bindCoinsToUser(DEFAULT_UID, "1");

        when(checkouterClient.doAuth(anyString(), any(UserInfo.class))).thenReturn(new AuthInfo(DEFAULT_MUID, cookie));

        final List<UserCoinResponse> coins = marketLoyaltyClient.bindCoinsToUserByMuid(
                ANOTHER_UID, DEFAULT_MUID, "9.9.9.9", "Mozilla", cookie
        );

        assertThat(
                coins,
                allOf(
                        contains(
                                allOf(
                                        coinHasKey(secondPromoCoin.getCoinKey()),
                                        hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                                        hasProperty("requireAuth", equalTo(false)),
                                        hasProperty("activationToken", nullValue())
                                )
                        )
                )
        );
    }

    @Test
    public void shouldBindCoinToUserByOrderId() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        final long orderId = 123456L;
        CoinKey coinKey = coinService.create.createCoin(promo, orderId, null, defaultNoAuth().build());

        assertThat(
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new),
                allOf(
                        hasProperty("status", equalTo(ACTIVE)),
                        hasProperty("requireAuth", equalTo(true))
                )
        );

        List<BindOrdersCoinResponse> bindOrdersCoinResponses = marketLoyaltyClient.bindCoinsToUserByOrders(
                new long[]{orderId}, DEFAULT_UID);
        assertThat(bindOrdersCoinResponses, hasSize(1));
        BindOrdersCoinResponse singleBindOrderResult = Iterables.getOnlyElement(bindOrdersCoinResponses);
        assertThat(singleBindOrderResult.getOrderId(), is(orderId));
        assertThat(singleBindOrderResult.getBindOrderCoinResults(), hasSize(1));
        assertThat(
                Iterables.getOnlyElement(singleBindOrderResult.getBindOrderCoinResults()).isSuccessful(),
                is(true)
        );

        assertEquals(CoreCoinStatus.ACTIVE,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());
    }

    @Test
    public void shouldHandleSecondCallOfBindCoinToUserByMuid() {
        String cookie = DEFAULT_MUID + ":val";
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed().setBindOnlyOnce(true));
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        when(checkouterClient.doAuth(anyString(), any(UserInfo.class))).thenReturn(new AuthInfo(DEFAULT_MUID, cookie));

        marketLoyaltyClient.bindCoinsToUserByMuid(DEFAULT_UID, DEFAULT_MUID, "9.9.9.9", "Mozilla", cookie);

        assertThat(
                marketLoyaltyClient.bindCoinsToUserByMuid(DEFAULT_UID, DEFAULT_MUID, "9.9.9.9", "Mozilla", cookie),
                contains(allOf(
                        coinHasKey(coinKey),
                        hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                        hasProperty("requireAuth", equalTo(false)),
                        hasProperty("activationToken", nullValue())
                ))
        );

        assertEquals(CoreCoinStatus.ACTIVE,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus());
    }


    @Test
    public void shouldNotBindCoinToUserByMuidIfSameCoinAlreadyBound() {
        String cookie = DEFAULT_MUID + ":val";
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed().setBindOnlyOnce(true));
        coinService.create.createCoin(promo, defaultAuth().build());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        when(checkouterClient.doAuth(anyString(), any(UserInfo.class))).thenReturn(new AuthInfo(DEFAULT_MUID, cookie));

        assertThat(
                marketLoyaltyClient.bindCoinsToUserByMuid(DEFAULT_UID, DEFAULT_MUID, "9.9.9.9", "Mozilla", cookie),
                is(empty())
        );

        assertTrue(coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getRequireAuth());
    }


    @Test
    public void shouldNotBindCoinToUserByOrderIdIfSameCoinAlreadyBound() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed().setBindOnlyOnce(true));
        coinService.create.createCoin(promo, defaultAuth().build());

        final long orderId = 123456L;
        CoinKey coinKey = coinService.create.createCoin(promo, orderId, null, defaultNoAuth().build());

        List<BindOrdersCoinResponse> bindOrdersCoinResponses = marketLoyaltyClient.bindCoinsToUserByOrders(
                new long[]{orderId}, DEFAULT_UID);
        assertThat(bindOrdersCoinResponses, hasSize(1));
        BindOrdersCoinResponse singleBindOrderResult = Iterables.getOnlyElement(bindOrdersCoinResponses);
        assertThat(singleBindOrderResult.getOrderId(), is(orderId));
        assertThat(singleBindOrderResult.getBindOrderCoinResults(), hasSize(1));
        assertThat(
                Iterables.getOnlyElement(singleBindOrderResult.getBindOrderCoinResults()).isSuccessful(),
                is(false)
        );

        assertTrue(coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getRequireAuth());
    }


    @Test
    public void should422IfMissingUserAgentHeaderOnBindCoinToUserByMuid() throws Exception {
        String cookie = DEFAULT_MUID + ":val";
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        assertThat(
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new),
                allOf(
                        hasProperty("status", equalTo(ACTIVE)),
                        hasProperty("requireAuth", equalTo(true))
                )
        );

        when(checkouterClient.doAuth(anyString(), any(UserInfo.class))).thenReturn(new AuthInfo(DEFAULT_MUID, cookie));

        mockMvc.perform(put("/coins/bindByMuid?ip=8.8.8.8&muid=1234213423&uid=1231")
                .contentType(MediaType.APPLICATION_JSON).cookie(new Cookie("muid", cookie)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void should403IfInvalidCookieOnBindCoinToUserByMuid() throws Exception {
        String cookie = DEFAULT_MUID + ":val";
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        assertThat(
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new),
                allOf(
                        hasProperty("status", equalTo(ACTIVE)),
                        hasProperty("requireAuth", equalTo(true))
                )
        );

        when(checkouterClient.doAuth(anyString(), any(UserInfo.class))).thenReturn(new AuthInfo(ANOTHER_MUID, cookie));

        mockMvc.perform(put("/coins/bindByMuid?ip=8.8.8.8&muid=1234213423&uid=1231")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "Mozilla")
                .cookie(new Cookie("muid", cookie))
        ).andExpect(status().isForbidden());
    }

    @Test
    public void should422IfMissingCookieOnBindCoinToUserByMuid() throws Exception {
        String cookie = DEFAULT_MUID + ":val";
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        assertThat(
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new),
                allOf(
                        hasProperty("status", equalTo(ACTIVE)),
                        hasProperty("requireAuth", equalTo(true))
                )
        );

        when(checkouterClient.doAuth(anyString(), any(UserInfo.class))).thenReturn(new AuthInfo(DEFAULT_MUID, cookie));

        mockMvc.perform(put("/coins/bindByMuid?ip=8.8.8.8&muid=1234213423&uid=1231")
                .contentType(MediaType.APPLICATION_JSON).header(HttpHeaders.USER_AGENT, "Mozilla"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void shouldBindCoinToUserThatAlreadyHasAnotherCoin() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(defaultFixed());
        Promo secondPromo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(firstPromo, defaultAuth().build());
        CoinKey secondCoinKey = coinService.create.createCoin(secondPromo, defaultNoAuth().build());

        assertThat(
                marketLoyaltyClient.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN),
                contains(allOf(
                        hasProperty("id", equalTo(secondCoinKey.getId())),
                        hasProperty("status", equalTo(CoinStatus.ACTIVE)),
                        hasProperty("activationToken", nullValue())
                ))
        );
    }

    @Test
    public void shouldNotBindCoinToUserThatAlreadyHasSameCoin() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(defaultFixed().setBindOnlyOnce(true));

        coinService.create.createCoin(firstPromo, defaultAuth().build());

        coinService.create.createCoin(firstPromo, defaultNoAuth().build());

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN)
        );
        assertEquals(SAME_COIN_ALREADY_BOUND, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void shouldNotBindCoinToUserThatAlreadyHasSameCoinWhenBindingAllCoins() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(defaultFixed().setBindOnlyOnce(true));

        coinService.create.createCoin(firstPromo, defaultAuth(DEFAULT_UID).build());
        coinService.create.createCoin(firstPromo, defaultAuth(DEFAULT_SBER_ID_UID).build());

        List<UserCoinResponse> bindCoins = marketLoyaltyClient.rebindCoinsToUser(DEFAULT_SBER_ID_UID, DEFAULT_UID);
        assertThat(bindCoins, empty());
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_SBER_ID_UID), hasSize(1));
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_UID), hasSize(1));
    }

    @Test
    public void shouldBindTwoCoinsToUserBySameActivationToken() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(defaultFixed());
        Promo secondPromo = promoManager.createSmartShoppingPromo(defaultFixed());

        String activationToken = "someActivationToken";
        CoinKey firstCoinKey = coinService.create.createCoin(firstPromo, defaultNoAuth(activationToken).build());
        CoinKey secondCoinKey = coinService.create.createCoin(secondPromo, defaultNoAuth(activationToken).build());

        assertThat(
                coinService.search.getCoin(firstCoinKey).orElseThrow(AssertionError::new),
                allOf(
                        hasProperty("status", equalTo(ACTIVE)),
                        hasProperty("requireAuth", equalTo(true))
                )
        );

        assertThat(
                marketLoyaltyClient.bindCoinsToUser(DEFAULT_UID, activationToken),
                containsInAnyOrder(
                        coinHasKey(firstCoinKey),
                        coinHasKey(secondCoinKey)
                )
        );
    }

    @Test
    public void shouldBindTwoCoinsToUser() {
        Promo firstPromo = promoManager.createSmartShoppingPromo(defaultFixed());
        Promo secondPromo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey firstCoinKey = coinService.create.createCoin(firstPromo, defaultAuth(DEFAULT_SBER_ID_UID).build());
        CoinKey secondCoinKey = coinService.create.createCoin(secondPromo, defaultAuth(DEFAULT_SBER_ID_UID).build());

        assertThat(
                coinService.search.getCoin(firstCoinKey).orElseThrow(AssertionError::new),
                allOf(
                        hasProperty("status", equalTo(ACTIVE))
                )
        );
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_SBER_ID_UID), hasSize(2));
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_UID), empty());

        assertThat(
                marketLoyaltyClient.rebindCoinsToUser(DEFAULT_SBER_ID_UID, DEFAULT_UID),
                containsInAnyOrder(
                        coinHasKey(firstCoinKey),
                        coinHasKey(secondCoinKey)
                )
        );

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
    public void shouldHandleSecondCallOfBindCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed().setBindOnlyOnce(true));

        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        assertThat(
                marketLoyaltyClient.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN),
                contains(allOf(
                        coinHasKey(coinKey),
                        hasProperty("status", equalTo(CoinStatus.ACTIVE))
                ))
        );

        assertThat(
                marketLoyaltyClient.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN),
                contains(allOf(
                        coinHasKey(coinKey),
                        hasProperty("status", equalTo(CoinStatus.ACTIVE))
                ))
        );
    }

    @Test
    public void shouldNotBoundAlreadyBoundedCoinToAnotherUser() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultNoAuth().build());

        marketLoyaltyClient.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN);

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.bindCoinsToUser(ANOTHER_UID, DEFAULT_ACTIVATION_TOKEN)
        );
        assertEquals(NOT_COIN_OWNER, exception.getMarketLoyaltyErrorCode());
    }

    @Test
    public void shouldBoundAlreadyBoundedCoinToAnotherUserWhenBindingAllCoins() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultNoAuth().build());

        assertThat(marketLoyaltyClient.bindCoinsToUser(DEFAULT_SBER_ID_UID, DEFAULT_ACTIVATION_TOKEN), hasSize(1));
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_SBER_ID_UID), hasSize(1));
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_UID), empty());

        assertThat(marketLoyaltyClient.rebindCoinsToUser(DEFAULT_SBER_ID_UID, DEFAULT_UID), hasSize(1));
        assertThat(marketLoyaltyClient.getCoins(DEFAULT_UID), hasSize(1));
    }

    @Test
    public void shouldNotBindCoinForUserFromBlackList() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        coinService.create.createCoin(promo, defaultNoAuth().build());

        long uid = 12312313123L;
        userBlackListDao.addRecord(new BlacklistRecord.Uid(uid));
        userBlacklistService.reloadBlacklist();

        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                marketLoyaltyClient.bindCoinsToUser(uid, DEFAULT_ACTIVATION_TOKEN)
        );

        assertEquals(MarketLoyaltyErrorCode.USER_IN_BLACKLIST, exception.getMarketLoyaltyErrorCode());
    }

    @Repeat(5)
    @Test
    public void shouldHandleParallelBindCoin() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth().build());

        testConcurrency(() -> () -> {
            try {
                assertThat(
                        marketLoyaltyClient.bindCoinsToUser(DEFAULT_UID, DEFAULT_ACTIVATION_TOKEN),
                        contains(allOf(
                                coinHasKey(coinKey),
                                hasProperty("status", equalTo(CoinStatus.ACTIVE))
                        ))
                );
            } catch (MarketLoyaltyException e) {
                assertTrue(e.getCause() instanceof HttpStatusCodeException);
                assertThat(((HttpStatusCodeException) e.getCause()).getStatusCode(), equalTo(HttpStatus.CONFLICT));
            }
        });
    }
}
