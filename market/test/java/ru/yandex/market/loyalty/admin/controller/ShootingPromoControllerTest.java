package ru.yandex.market.loyalty.admin.controller;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.controller.dto.IdObject;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.utils.FakePromoUtils;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinSearchRequest;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.admin.utils.FakePromoUtils.FAKE_PROMOCODE_NOMINAL;
import static ru.yandex.market.loyalty.admin.utils.FakePromoUtils.isFakePromo;
import static ru.yandex.market.loyalty.admin.utils.FakePromoUtils.prepareFakeCoinPromoBuilder;
import static ru.yandex.market.loyalty.admin.utils.FakePromoUtils.prepareFakePromocodePromoBuilder;
import static ru.yandex.market.loyalty.core.rule.RuleType.FAKE_USER_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.fakeUserAuth;
import static ru.yandex.market.sdk.userinfo.service.UidConstants.NO_SIDE_EFFECT_UID;

@TestFor(ShootingPromoController.class)
public class ShootingPromoControllerTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    @MarketLoyaltyAdmin
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;

    @Test
    public void updateInactiveFakePromo() throws Exception {
        Promo fakeSmarShoppingPromo = promoManager.createFakeSmartShoppingPromo(prepareFakeCoinPromoBuilder());
        long promoId = fakeSmarShoppingPromo.getPromoId().getId();

        assertEquals(fakeSmarShoppingPromo.getStatus(), PromoStatus.ACTIVE);

        inactiveFakePromo(promoId);
        Promo promoInactive = promoService.getPromo(promoId);

        assertEquals(promoInactive.getStatus(), PromoStatus.INACTIVE);
    }

    @Test
    public void updateInactiveFakePromocode() throws Exception {
        Promo fakeSmarShoppingPromo = promoManager.createFakePromocodePromo(prepareFakePromocodePromoBuilder());
        long promoId = fakeSmarShoppingPromo.getPromoId().getId();

        assertEquals(fakeSmarShoppingPromo.getStatus(), PromoStatus.ACTIVE);

        inactiveFakePromo(promoId);
        Promo promoInactive = promoService.getPromo(promoId);

        assertEquals(promoInactive.getStatus(), PromoStatus.INACTIVE);
    }

    @Test
    public void shouldCreatePromocode() throws Exception {
        long promoId = createFakePromo(PromoSubType.PROMOCODE);
        Promo promo = promoService.getPromo(promoId);
        assertTrue(isFakePromo(promo));
        assertTrue(promo.getPromoId().getId() >= FakePromoUtils.FAKE_PROMO_INITIAL_ID);

        coinService.create.createCoin(promo, fakeUserAuth().build());

        Coin coin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(NO_SIDE_EFFECT_UID)).get(0);
        assertThat(coin.getNominal(), comparesEqualTo(BigDecimal.valueOf(FAKE_PROMOCODE_NOMINAL)));
        assertTrue(coin.getRulesContainer().hasRule(FAKE_USER_CUTTING_RULE));
    }

    @Test
    public void shouldDoubleCreatePromocode() throws Exception {
        long promoIdFirst = createFakePromo(PromoSubType.PROMOCODE);
        long promoIdSecond = createFakePromo(PromoSubType.PROMOCODE);
        Promo promoFisrt = promoService.getPromo(promoIdFirst);
        Promo promoSecond = promoService.getPromo(promoIdSecond);
        assertTrue(isFakePromo(promoFisrt));
        assertTrue(isFakePromo(promoSecond));
    }

    @Test
    public void shouldCreateFakePromo() throws Exception {
        long promoId = createFakePromo(PromoSubType.MARKET_BONUS);
        Promo promo = promoService.getPromo(promoId);
        assertTrue(isFakePromo(promo));
        assertTrue(promo.getPromoId().getId() >= FakePromoUtils.FAKE_PROMO_INITIAL_ID);

        coinService.create.createCoin(promoService.getPromo(promoId), fakeUserAuth().build());

        Coin coin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(NO_SIDE_EFFECT_UID)).get(0);
        assertThat(coin.getNominal(), comparesEqualTo(BigDecimal.valueOf(100L)));
        assertTrue(coin.getRulesContainer().hasRule(FAKE_USER_CUTTING_RULE));
    }

    private void inactiveFakePromo(Long promoId) throws Exception {
        objectMapper.readValue(mockMvc
                .perform(put("/api/shooting/promo/inactiveFakePromo/" + promoId)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), IdObject.class);
    }

    private Long createFakePromo(PromoSubType promoSubType) throws Exception {
        String jsonResponse = mockMvc
                .perform(post("/api/shooting/promo/createFakePromo/" + promoSubType.getCode())
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.valueOf(objectMapper.readValue(jsonResponse, IdObject.class).getId());
    }
}
