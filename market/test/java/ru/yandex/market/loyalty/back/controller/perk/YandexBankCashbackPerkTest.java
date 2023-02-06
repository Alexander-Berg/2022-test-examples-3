package ru.yandex.market.loyalty.back.controller.perk;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.loyalty.api.model.perk.PerkStatResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.api.model.web.LoyaltyTag;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.perks.impl.YandexBankCashbackPerkProcessor;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.BankTestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_BANK_CASHBACK;
import static ru.yandex.market.loyalty.api.model.perk.PerkUnavailableReason.NOT_IN_EXPERIMENT;
import static ru.yandex.market.loyalty.api.model.perk.PerkUnavailableReason.NOT_SUITABLE;
import static ru.yandex.market.loyalty.api.model.perk.PerkUnavailableReason.PERK_DISABLED;
import static ru.yandex.market.loyalty.api.model.perk.PerkUnavailableReason.WRONG_CONFIG;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MAX_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.rule.RuleType.MAX_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.DEFAULT_REGION;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(YandexBankCashbackPerkProcessor.class)
public class YandexBankCashbackPerkTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final String TEST_PROMO_KEY = "some_key";
    private static final String TEST_REAR = "test_rearr";
    private static final BigDecimal TEST_MAX_ORDER_TOTAL = BigDecimal.valueOf(15_000);

    @Autowired
    private BankTestUtils bankTestUtils;
    @Autowired
    private YandexBankCashbackPerkProcessor yandexBankCashbackPerkProcessor;
    @Autowired
    private PromoManager promoManager;

    @Before
    public void createPromo() {
        promoManager.createCashbackPromo(PromoUtils.Cashback
                .defaultPercent(BigDecimal.ONE, CashbackLevelType.MULTI_ORDER)
                .setPromoKey(TEST_PROMO_KEY)
                .addCashbackRule(MAX_ORDER_TOTAL_CUTTING_RULE, MAX_ORDER_TOTAL, TEST_MAX_ORDER_TOTAL)
        );
    }

    private void setYandexBankCashbackConfig(boolean enabled, String promoKey, String rearr, boolean withPlusOnly) {
        configurationService.set(ConfigurationService.YANDEX_BANK_CASHBACK_ENABLED, enabled);
        configurationService.set(ConfigurationService.YANDEX_BANK_CASHBACK_PROMO_KEY, promoKey);
        configurationService.set(ConfigurationService.YANDEX_BANK_CASHBACK_REARR, rearr);
        configurationService.set(ConfigurationService.YANDEX_BANK_CASHBACK_WITH_PLUS_ONLY, withPlusOnly);
    }

    @Test
    public void shouldReturnCorrectPerkType() {
        assertThat(yandexBankCashbackPerkProcessor.getProcessedPerk(), equalTo(YANDEX_BANK_CASHBACK));
    }

    @Test
    public void shouldPurchasePerkSuccessfully() throws Exception {
        setYandexBankCashbackConfig(true, TEST_PROMO_KEY, null, false);
        bankTestUtils.mockCalculatorWithDefaultResponse();

        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                        .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_BANK_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(YANDEX_BANK_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(BankTestUtils.MAX_AMOUNT_DEFAULT)),
                hasProperty("cashbackPercentNominal", comparesEqualTo(BankTestUtils.PERCENT_DEFAULT)),
                hasProperty("promoKey", equalTo(TEST_PROMO_KEY)),
                hasProperty("maxOrderTotal", equalTo(TEST_MAX_ORDER_TOTAL)),
                hasProperty("unavailableReason", nullValue())
        ))));
    }

    @Test
    public void shouldNotPurchaseWhenConfigDisabled() throws Exception {
        setYandexBankCashbackConfig(false, TEST_PROMO_KEY, null, false);
        bankTestUtils.mockCalculatorWithDefaultResponse();

        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                        .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_BANK_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(YANDEX_BANK_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("maxCashbackTotal", nullValue()),
                hasProperty("cashbackPercentNominal", nullValue()),
                hasProperty("promoKey", nullValue()),
                hasProperty("maxOrderTotal", nullValue()),
                hasProperty("unavailableReason", equalTo(PERK_DISABLED))
        ))));
    }

    @Test
    public void shouldNotPurchaseWhenWrongConfig() throws Exception {
        setYandexBankCashbackConfig(true, null, null, false);
        bankTestUtils.mockCalculatorWithDefaultResponse();

        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                        .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_BANK_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(YANDEX_BANK_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("maxCashbackTotal", nullValue()),
                hasProperty("cashbackPercentNominal", nullValue()),
                hasProperty("promoKey", nullValue()),
                hasProperty("maxOrderTotal", nullValue()),
                hasProperty("unavailableReason", equalTo(WRONG_CONFIG))
        ))));
    }

    @Test
    public void shouldNotPurchaseWhenBankThrowsError() throws Exception {
        setYandexBankCashbackConfig(true, TEST_PROMO_KEY, null, false);
        bankTestUtils.mockCalculatorWithThrowable(new RuntimeException());

        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                        .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_BANK_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(YANDEX_BANK_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("maxCashbackTotal", nullValue()),
                hasProperty("cashbackPercentNominal", nullValue()),
                hasProperty("promoKey", nullValue()),
                hasProperty("maxOrderTotal", nullValue()),
                hasProperty("unavailableReason", equalTo(NOT_SUITABLE))
        ))));
    }

    @Test
    public void shouldNotPurchaseWhenBankRespondsWithNotFound() throws Exception {
        setYandexBankCashbackConfig(true, TEST_PROMO_KEY, null, false);
        bankTestUtils.mockCalculatorWithNotFoundResponse();

        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                        .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_BANK_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(YANDEX_BANK_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("maxCashbackTotal", nullValue()),
                hasProperty("cashbackPercentNominal", nullValue()),
                hasProperty("promoKey", nullValue()),
                hasProperty("maxOrderTotal", nullValue()),
                hasProperty("unavailableReason", equalTo(NOT_SUITABLE))
        ))));
    }

    @Test
    public void shouldPurchaseWhenRearrPresentAndRearrRequired() throws Exception {
        setYandexBankCashbackConfig(true, TEST_PROMO_KEY, TEST_REAR, false);
        bankTestUtils.mockCalculatorWithDefaultResponse();

        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Market-Rearrfactors", TEST_REAR)
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                        .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_BANK_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(YANDEX_BANK_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(BankTestUtils.MAX_AMOUNT_DEFAULT)),
                hasProperty("cashbackPercentNominal", comparesEqualTo(BankTestUtils.PERCENT_DEFAULT)),
                hasProperty("promoKey", equalTo(TEST_PROMO_KEY)),
                hasProperty("maxOrderTotal", equalTo(TEST_MAX_ORDER_TOTAL)),
                hasProperty("unavailableReason", nullValue())
        ))));
    }

    @Test
    public void shouldNotPurchaseWhenNoRearrAndRearrRequired() throws Exception {
        setYandexBankCashbackConfig(true, TEST_PROMO_KEY, TEST_REAR, false);
        bankTestUtils.mockCalculatorWithDefaultResponse();

        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        //.header("X-Market-Rearrfactors", TEST_REAR)   no rearr
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                        .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_BANK_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(YANDEX_BANK_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("maxCashbackTotal", nullValue()),
                hasProperty("cashbackPercentNominal", nullValue()),
                hasProperty("promoKey", nullValue()),
                hasProperty("maxOrderTotal", nullValue()),
                hasProperty("unavailableReason", equalTo(NOT_IN_EXPERIMENT))
        ))));
    }

    @Test
    public void shouldPurchaseWhenPlusPresentAndPlusRequired() throws Exception {
        setYandexBankCashbackConfig(true, TEST_PROMO_KEY, null, true);
        bankTestUtils.mockCalculatorWithDefaultResponse();
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);

        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                        .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_BANK_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(YANDEX_BANK_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(BankTestUtils.MAX_AMOUNT_DEFAULT)),
                hasProperty("cashbackPercentNominal", comparesEqualTo(BankTestUtils.PERCENT_DEFAULT)),
                hasProperty("promoKey", equalTo(TEST_PROMO_KEY)),
                hasProperty("maxOrderTotal", equalTo(TEST_MAX_ORDER_TOTAL)),
                hasProperty("unavailableReason", nullValue())
        ))));
    }

    @Test
    public void shouldNotPurchaseWhenNoPlusAndPlusRequired() throws Exception {
        setYandexBankCashbackConfig(true, TEST_PROMO_KEY, null, true);
        bankTestUtils.mockCalculatorWithDefaultResponse();
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, false, blackboxRestTemplate);

        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                        .queryParam(LoyaltyTag.PERK_TYPE, YANDEX_BANK_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(YANDEX_BANK_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("maxCashbackTotal", nullValue()),
                hasProperty("cashbackPercentNominal", nullValue()),
                hasProperty("promoKey", nullValue()),
                hasProperty("maxOrderTotal", nullValue()),
                hasProperty("unavailableReason", equalTo(NOT_SUITABLE))
        ))));
    }
}
