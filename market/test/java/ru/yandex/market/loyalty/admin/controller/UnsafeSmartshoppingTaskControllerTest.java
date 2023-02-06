package ru.yandex.market.loyalty.admin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.controller.dto.ChangeCoinOwnershipRequest;
import ru.yandex.market.loyalty.admin.controller.dto.ChangeConfigurationParamRequest;
import ru.yandex.market.loyalty.admin.controller.dto.ResetCoinOwnershipRequest;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.CoinNotificationProcessor;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.service.ConfigParam;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping;
import ru.yandex.market.loyalty.test.TestFor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.GENERIC_BUNDLES_ENABLE;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultNoAuth;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(UnsafeSmartshoppingTaskController.class)
public class UnsafeSmartshoppingTaskControllerTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CoinNotificationProcessor coinNotificationProcessor;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoService promoService;
    private LocalDateTime LAST_MONTH;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        LAST_MONTH = LocalDateTime.now(clock).minusMonths(1);
    }

    @Test
    public void shouldSearchCoin() throws Exception {
        SmartShoppingPromoBuilder coinPromoBuilder = SmartShopping.defaultFixed();
        Promo promo = promoManager.createSmartShoppingPromo(coinPromoBuilder);
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth("123").build());

        String contentAsString = mockMvc
                .perform(get("/api/unsafe/smartShopping/promo/searchCoin?term=" + coinKey.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<AdminCoinResponse> o = objectMapper.readValue(
                contentAsString, new TypeReference<List<AdminCoinResponse>>() {
                });

        assertThat(o, hasSize(1));
        assertThat(o, contains(hasProperty("activationToken", equalTo("123"))));
    }

    @Test
    public void shouldResetCoinOwnership() throws Exception {
        SmartShoppingPromoBuilder coinPromoBuilder = SmartShopping.defaultFixed();
        Promo promo = promoManager.createSmartShoppingPromo(coinPromoBuilder);
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth("123").build());
        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, "123");
        Coin coin = coinService.search.getCoin(coinKey).orElse(null);
        assertThat(coin, is(notNullValue()));
        assertThat(coin, hasProperty("uid", equalTo(DEFAULT_UID)));

        mockMvc
                .perform(post("/api/unsafe/smartShopping/promo/resetCoinOwnership")
                        .content(objectMapper.writeValueAsString(new ResetCoinOwnershipRequest(coinKey.getId(),
                                "MARKETDISCOUNT-10000")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk());

        coin = coinService.search.getCoin(coinKey).orElse(null);
        assertThat(coin, is(notNullValue()));
        assertThat(coin, hasProperty("uid", is(nullValue())));
    }

    @Test
    public void shouldChangeCoinOwnership() throws Exception {
        SmartShoppingPromoBuilder coinPromoBuilder = SmartShopping.defaultFixed();
        Promo promo = promoManager.createSmartShoppingPromo(coinPromoBuilder);
        CoinKey coinKey = coinService.create.createCoin(promo, defaultNoAuth("123").build());
        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, "123");
        Coin coin = coinService.search.getCoin(coinKey).orElse(null);
        assertThat(coin, is(notNullValue()));
        assertThat(coin, hasProperty("uid", equalTo(DEFAULT_UID)));

        mockMvc
                .perform(post("/api/unsafe/smartShopping/promo/changeCoinOwnership")
                        .content(objectMapper.writeValueAsString(new ChangeCoinOwnershipRequest(coinKey.getId(),
                                ANOTHER_UID, "MARKETDISCOUNT-1000")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk());

        coin = coinService.search.getCoin(coinKey).orElse(null);
        assertThat(coin, is(notNullValue()));
        assertThat(coin, hasProperty("uid", equalTo(ANOTHER_UID)));
    }

    @Test
    public void shouldNotFailNotificationAfterChangeCoinOwnership() throws Exception {
        Promo promo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed());
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth().build());

        mockMvc
                .perform(post("/api/unsafe/smartShopping/promo/changeCoinOwnership")
                        .content(objectMapper.writeValueAsString(new ChangeCoinOwnershipRequest(coinKey.getId(),
                                ANOTHER_UID, "MARKETDISCOUNT-1000")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk());

        coinNotificationProcessor.processBoundCoinsNotifications(500, LAST_MONTH);
    }

    @Test
    public void shouldSetEmissionFolding() throws Exception {
        Promo promo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed());

        mockMvc
                .perform(post("/api/unsafe/smartShopping/setEmissionFolding?promoId=" + promo.getId())
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk());

        assertEquals(
                promoService.getPromo(promo.getId())
                        .getPromoParam(PromoParameterName.EMISSION_FOLDING)
                        .orElseThrow(),
                Boolean.TRUE
        );
    }

    @Test
    public void shouldResetPerkCache() throws Exception {
        mockMvc
                .perform(post("/api/unsafe/smartShopping/resetPerkCache?uid=123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetProperties() throws Exception {
        setEnableProperty(GENERIC_BUNDLES_ENABLE, true);

        assertEquals("{\"value\":\"true\"}", mockMvc
                .perform(get("/api/unsafe/smartShopping/configuration/param/" + GENERIC_BUNDLES_ENABLE)
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    public void shouldRemoveProperty() throws Exception {
        setEnableProperty(GENERIC_BUNDLES_ENABLE, true);
        assertTrue(configurationService.getNullable(GENERIC_BUNDLES_ENABLE).isPresent());

        mockMvc
                .perform(put("/api/unsafe/smartShopping/configuration/param/")
                        .content(objectMapper.writeValueAsString(
                                new ChangeConfigurationParamRequest(GENERIC_BUNDLES_ENABLE.getName(), null,
                                        "MARKETDISCOUNT-0000")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk());

        assertTrue(configurationService.getNullable(GENERIC_BUNDLES_ENABLE).isEmpty());
    }

    @Test
    public void shouldEnableProperties() throws Exception {
        setEnableProperty(GENERIC_BUNDLES_ENABLE, true);

        assertTrue(configurationService.bundlesEnabled());
    }

    @Test
    public void shouldDisableProperties() throws Exception {
        setEnableProperty(GENERIC_BUNDLES_ENABLE, false);

        assertFalse(configurationService.bundlesEnabled());
    }

    private void setEnableProperty(ConfigParam property, boolean enable) throws Exception {
        mockMvc
                .perform(put("/api/unsafe/smartShopping/configuration/param")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeConfigurationParamRequest(property.getName(),
                                String.valueOf(enable), "MARKETDISCOUNT-1000")))
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk());
    }

    @Test
    public void getLocalCacheData() throws Exception {
        String property = "test.check";
        configurationService.set("market.loyalty.config." + property, "true");
        TypeReference<Map<String, String>> mapType =
                new TypeReference<>() {
                };
        Map<String, String> localCache = objectMapper.readValue(mockMvc
                .perform(get("/api/unsafe/smartShopping/configuration/param/getLocalCache")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString(), mapType);
        assertTrue(localCache.containsKey(property));
    }
}
