package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationRequest;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResultCode;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeBudgetResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeCheckRequest;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeDetailsResponse;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeReservePeriodRequest;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.dao.UserActivePromocodeEntryDao;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.promo.UserPromocodeEntry;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.constants.DefaultCurrencyUnit;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.service.promocode.PromocodesActivationResult;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.lightweight.DateUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContextDto;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_BUDGET;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_MAX_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.promo.ShopPromoIdUtils.createLoyaltyShopPromoId;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;
import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;

@TestFor(PromocodesController.class)
public class PromocodesControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final String PROMOCODE = "some_promocode";
    private static final long USER_ID = 123L;
    private static final String LANDING_URL = "some_landing_url";
    private static final long SHOP_ID = 151251;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private UserActivePromocodeEntryDao userActivePromocodeEntryDao;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;

    private Promo coinPromocode;

    @Before
    public void configure() {
        coinPromocode = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setPromoSource(LOYALTY_VALUE)
                .setShopPromoId(SHOP_ID + "_" + PROMOCODE)
                .setShopId(SHOP_ID)
                .setCode(PROMOCODE)
                .setLandingUrl(LANDING_URL)
                .addCoinRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        Set.of(DEFAULT_MIN_ORDER_TOTAL))
                .addCoinRule(
                        RuleType.UPPER_BOUND_DISCOUNT_BASE_RULE,
                        RuleParameterName.MAX_ORDER_TOTAL,
                        Set.of(DEFAULT_MAX_ORDER_TOTAL)));
    }

    @Test
    public void shouldActivatePromocodes() {
        assertThat(userActivePromocodeEntryDao.selectActive(UserActivePromocodeEntryDao.USER_ID.eqTo(USER_ID)),
                empty());

        PromocodeActivationResponse activationResponse = marketLoyaltyClient.activatePromocodes(
                new PromocodeActivationRequest(USER_ID,
                        null,
                        Set.of(PROMOCODE),
                        null,
                        null)
        );

        assertThat(activationResponse.getActivationResults(), hasItem(allOf(
                hasProperty("code", is(PROMOCODE)),
                hasProperty("resultCode", is(PromocodeActivationResultCode.SUCCESS))
        )));

        List<UserPromocodeEntry> activePromocodes = userActivePromocodeEntryDao.selectActive(
                UserActivePromocodeEntryDao.USER_ID.eqTo(USER_ID)
        );

        assertThat(activePromocodes, hasSize(1));
        assertThat(activePromocodes, hasItem(allOf(
                hasProperty("code", equalToIgnoringCase(PROMOCODE)),
                hasProperty("promoId", comparesEqualTo(coinPromocode.getId()))
        )));
    }

    @Test
    public void shouldActivatePromocodesInDifferentCase() {
        assertThat(userActivePromocodeEntryDao.selectActive(UserActivePromocodeEntryDao.USER_ID.eqTo(USER_ID)),
                empty());

        PromocodeActivationResponse activationResponse = marketLoyaltyClient.activatePromocodes(
                new PromocodeActivationRequest(USER_ID,
                        null,
                        Set.of(PROMOCODE),
                        null,
                        null));

        assertThat(activationResponse.getActivationResults(), hasItem(allOf(
                hasProperty("code", is(PROMOCODE.toLowerCase())),
                hasProperty("resultCode", is(PromocodeActivationResultCode.SUCCESS))
        )));

        List<UserPromocodeEntry> activePromocodes = userActivePromocodeEntryDao.selectActive(
                UserActivePromocodeEntryDao.USER_ID.eqTo(USER_ID)
        );

        assertThat(activePromocodes, hasSize(1));
        assertThat(activePromocodes, hasItem(allOf(
                hasProperty("code", equalToIgnoringCase(PROMOCODE)),
                hasProperty("promoId", comparesEqualTo(coinPromocode.getId())),
                hasProperty("coinKey")
        )));

        Coin coin = coinService.search.getCoin(activePromocodes.get(0).getCoinKey()).orElseThrow();

        assertThat(coin.getStatus(), is(ACTIVE));
    }

    @Test
    public void shouldGetPromocodeDescriptionByShopPromoId() {

        String shopPromoId = createLoyaltyShopPromoId(coinPromocode.getPromoId().getId());

        PromocodeDetailsResponse promocodeDetailsResponse = marketLoyaltyClient.getPromocodeDetailsByShopPromoId(
                shopPromoId);
        assertThat(promocodeDetailsResponse, notNullValue());
        assertThat(promocodeDetailsResponse.getCode(), is(PROMOCODE));
        assertThat(promocodeDetailsResponse.getLandingUrl(), is(LANDING_URL));
        assertThat(promocodeDetailsResponse.getDiscountCurrency(), is(DefaultCurrencyUnit.RUB.getCode()));
        assertThat(promocodeDetailsResponse.getPromoSource(), is(LOYALTY_VALUE));
        assertThat(promocodeDetailsResponse.isBindOnlyOnce(), is(false));
        assertThat(promocodeDetailsResponse.getOrderMinPrice(), is(DEFAULT_MIN_ORDER_TOTAL));
        assertThat(promocodeDetailsResponse.getOrderMaxPrice(), is(DEFAULT_MAX_ORDER_TOTAL));
    }

    @Test
    public void shouldGetPromocodeDescriptionBy3PShopPromoId() {
        PromocodeDetailsResponse promocodeDetailsResponse = marketLoyaltyClient
                .getPromocodeDetailsByShopPromoId(coinPromocode.getShopPromoId());
        assertThat(promocodeDetailsResponse, notNullValue());
        assertThat(promocodeDetailsResponse.getCode(), is(PROMOCODE));
        assertThat(promocodeDetailsResponse.getLandingUrl(), is(LANDING_URL));
        assertThat(promocodeDetailsResponse.getDiscountCurrency(), is(DefaultCurrencyUnit.RUB.getCode()));
        assertThat(promocodeDetailsResponse.getPromoSource(), is(LOYALTY_VALUE));
        assertThat(promocodeDetailsResponse.isBindOnlyOnce(), is(false));
        assertThat(promocodeDetailsResponse.getShopId(), equalTo(SHOP_ID));
    }

    @Test
    public void shouldNotGetPromocodeDescriptionByShopPromoId() {
        MarketLoyaltyException e = assertThrows(MarketLoyaltyException.class, () -> marketLoyaltyClient
                .getPromocodeDetailsByShopPromoId("some shop promo id"));

        assertEquals(((HttpClientErrorException) e.getCause()).getRawStatusCode(), HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void shouldGenerateNewPromoCode() throws Exception {
        var promoCodeGenerationFirstResponse = marketLoyaltyClient.generatePromocode();
        assertThat(promoCodeGenerationFirstResponse, notNullValue());
        var firstCode = promoCodeGenerationFirstResponse.getCode();
        assertThat(firstCode, not(blankOrNullString()));

        var promoCodeGenerationSecondResponse = marketLoyaltyClient.generatePromocode();
        assertThat(promoCodeGenerationSecondResponse, notNullValue());
        var secondCode = promoCodeGenerationSecondResponse.getCode();
        assertThat(secondCode, not(blankOrNullString()));

        assertThat(firstCode, not(equalTo(secondCode)));
    }

    @Test
    public void shouldGetBudgetByPromocode() throws Exception {
        var promocodeBudgetResponse = getPromocodeBudgetResponse();

        assertThat(promocodeBudgetResponse, notNullValue());
        assertThat(promocodeBudgetResponse.getCurrentBudget(), comparesEqualTo(DEFAULT_BUDGET));
        assertThat(promocodeBudgetResponse.getSpentBudget(), comparesEqualTo(BigDecimal.ZERO));

        //Используем промокод и проверяем, что бюджет обновился
        final PromocodesActivationResult activateResult = promocodeService.activatePromocodes(
                ru.yandex.market.loyalty.core.service.promocode.PromocodeActivationRequest.builder()
                        .userId(USER_ID)
                        .externalPromocodes(Set.of(PROMOCODE))
                        .build());

        assertThat(activateResult.getActivationResults(), hasItem(allOf(
                hasProperty("activationResultCode", is(PromocodeActivationResultCode.SUCCESS))
        )));

        OrderWithBundlesRequest order = orderRequestWithBundlesBuilder()
                .withOrderItem(itemKey(DEFAULT_ITEM_KEY)).build();

        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(order)
                        .withOperationContext(uidOperationContextDto(USER_ID))
                        .withCoupon(PROMOCODE)
                        .build());

        assertThat(discountResponse.getPromocodeErrors(), empty());

        var promocodeBudgetResponseAfterUsage = getPromocodeBudgetResponse();

        assertThat(promocodeBudgetResponseAfterUsage, notNullValue());
        assertThat(
                promocodeBudgetResponseAfterUsage.getCurrentBudget(),
                comparesEqualTo(DEFAULT_BUDGET.subtract(DEFAULT_COIN_FIXED_NOMINAL))
        );
        assertThat(
                promocodeBudgetResponseAfterUsage.getSpentBudget(),
                comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)
        );
    }

    @Test
    public void shouldCheckAvailablePromocode() {
        var promocodeCheckRequest = new PromocodeCheckRequest("SOME_CODE",
                DateUtils.toDate(LocalDateTime.now()), DateUtils.toDate(LocalDateTime.now().plusDays(10L)));

        var response = marketLoyaltyClient.checkPromocode(promocodeCheckRequest);
        assertThat(response, notNullValue());
        assertTrue(response.isCodeAvailable());
    }

    @Test
    public void shouldCheckNotAvailablePromocode() {
        var promocodeCheckRequest = new PromocodeCheckRequest(PROMOCODE,
                DateUtils.toDate(LocalDateTime.now()), DateUtils.toDate(LocalDateTime.now().plusDays(10L)));

        var response = marketLoyaltyClient.checkPromocode(promocodeCheckRequest);
        assertThat(response, notNullValue());
        assertFalse(response.isCodeAvailable());
        assertThat(response.getReason(), is(MarketLoyaltyErrorCode.PROMOCODE_IS_OCCUPIED_NOW));
    }

    @Test
    public void shouldCheckBadPromocode() {
        var promocodeCheckRequest = new PromocodeCheckRequest("sammudak",
                DateUtils.toDate(LocalDateTime.now()), DateUtils.toDate(LocalDateTime.now().plusDays(10L)));

        var response = marketLoyaltyClient.checkPromocode(promocodeCheckRequest);
        assertThat(response, notNullValue());
        assertFalse(response.isCodeAvailable());
        assertThat(response.getReason(), is(MarketLoyaltyErrorCode.PROMOCODE_VALUE_IS_BAD));
    }

    @Test
    public void shouldCheckReservedPromocode() {
        var promocodeCheckRequest = new PromocodeCheckRequest("SOME_PROMO",
                DateUtils.toDate(LocalDateTime.now()), DateUtils.toDate(LocalDateTime.now().plusDays(10L)));

        assertTrue(marketLoyaltyClient.checkPromocode(promocodeCheckRequest).isCodeAvailable());

        marketLoyaltyClient.reservePromocode(promocodeCheckRequest.getCode());
        var response = marketLoyaltyClient.checkPromocode(promocodeCheckRequest);
        assertThat(response, notNullValue());
        assertFalse(response.isCodeAvailable());
        assertThat(response.getReason(), is(MarketLoyaltyErrorCode.PROMOCODE_IS_OCCUPIED_NOW));
    }

    @Test
    public void shouldCheckReservedForPeriodPromocode() {
        var periodStartsAt = DateUtils.toDate(LocalDateTime.now());
        var periodEndsAt = DateUtils.toDate(LocalDateTime.now().plusDays(10L));
        var code = "SOME_PROMO";

        var promocodeCheckRequest = new PromocodeCheckRequest(code, periodStartsAt, periodEndsAt);
        assertTrue(marketLoyaltyClient.checkPromocode(promocodeCheckRequest).isCodeAvailable());

        var reserveRequest = new PromocodeReservePeriodRequest(periodStartsAt, periodEndsAt);
        marketLoyaltyClient.reservePromocode(code, reserveRequest);

        var checkResponse = marketLoyaltyClient.checkPromocode(promocodeCheckRequest);
        assertThat(checkResponse, notNullValue());
        assertFalse(checkResponse.isCodeAvailable());
        assertThat(checkResponse.getReason(), is(MarketLoyaltyErrorCode.PROMOCODE_IS_OCCUPIED_NOW));

        var checkBeforeReservationPeriodRequest =
                new PromocodeCheckRequest(code, new Date(periodStartsAt.getTime() - 1000),
                        new Date(periodStartsAt.getTime() - 1));
        assertTrue(marketLoyaltyClient.checkPromocode(checkBeforeReservationPeriodRequest).isCodeAvailable());

        var checkAfterReservationPeriodRequest =
                new PromocodeCheckRequest(code, new Date(periodEndsAt.getTime() + 1),
                        new Date(periodEndsAt.getTime() + 1000));
        assertTrue(marketLoyaltyClient.checkPromocode(checkAfterReservationPeriodRequest).isCodeAvailable());
    }

    private PromocodeBudgetResponse getPromocodeBudgetResponse() throws Exception {
        return objectMapper.readValue(
                mockMvc.perform(get("/promocodes/v1/" + PROMOCODE + "/budget"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), PromocodeBudgetResponse.class
        );
    }
}
