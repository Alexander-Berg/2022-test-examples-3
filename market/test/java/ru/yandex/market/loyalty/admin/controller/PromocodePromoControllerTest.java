package ru.yandex.market.loyalty.admin.controller;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.controller.dto.CoinCreationReasonDto;
import ru.yandex.market.loyalty.admin.controller.dto.CoinPropsDto;
import ru.yandex.market.loyalty.admin.controller.dto.ExpirationPolicyDto;
import ru.yandex.market.loyalty.admin.controller.dto.IdObject;
import ru.yandex.market.loyalty.admin.controller.dto.PromocodePromoDto;
import ru.yandex.market.loyalty.admin.controller.dto.UsageRestrictionsDto;
import ru.yandex.market.loyalty.admin.controller.dto.UsageRestrictionsDto.FilterRestriction;
import ru.yandex.market.loyalty.admin.support.service.promocode.PromocodeCheckRequest;
import ru.yandex.market.loyalty.admin.support.service.promocode.PromocodeCheckResponse;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.coin.CoinCreationReason;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.dao.promocode.PromocodeEntryDao;
import ru.yandex.market.loyalty.core.model.accounting.Account;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoinProps;
import ru.yandex.market.loyalty.core.model.coin.CoinSearchRequest;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.NominalStrategy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.model.promo.PromocodeEntry;
import ru.yandex.market.loyalty.core.model.promo.PromocodePromoBuilder;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdminMockConfigurer.StartrekServiceConfiguration.TEST_TICKET_OK;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinType.FIXED;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.ANAPLAN_ID;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.PROMO_GLOBAL_PRIORITY;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.PROMO_SOURCE;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.rule.RuleType.FIRST_ORDER_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.RESET_COIN_PROPERTIES_WHEN_PROMOCODE_UPDATE_ANYWAY;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_PERCENT_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(PromocodePromoController.class)
public class PromocodePromoControllerTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String PROMOCODE = "some_promocode";
    private static final String PROMO_ANAPLAN_ID = "anaplan id";
    private static final long UID = 123L;
    private static final long ANOTHER_UID = 1231L;
    private static final TypeReference<List<CoinCreationReasonDto>> COIN_CREATION_REASON_LIST_TYPE_REF =
            new TypeReference<>() {
    };
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    @MarketLoyaltyAdmin
    private ObjectMapper objectMapper;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private CoinDao coinDao;
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromocodeEntryDao promocodeEntryDao;

    @Test
    public void shouldReturnCoinReasons() throws Exception {
        String jsonResponse = mockMvc
                .perform(get("/api/promocode/promo/possibleReasons").with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<CoinCreationReasonDto> reasons = objectMapper.readValue(
                jsonResponse,
                COIN_CREATION_REASON_LIST_TYPE_REF
        );
        assertThat(
                reasons.stream().map(CoinCreationReasonDto::getReason).collect(Collectors.toList()),
                containsInAnyOrder(
                        Stream.of(CoinCreationReason.FOR_USER_ACTION, CoinCreationReason.PARTNER)
                                .map(Matchers::equalTo)
                                .collect(Collectors.toList())
                )
        );
    }

    @Test
    public void shouldCreatePromocodeWithFixedNominal() throws Exception {
        String msku = "1231123";
        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton(msku))
        );

        Long promoId = createPromo(coinPromoDto);

        coinService.create.createCoin(promoService.getPromo(promoId), defaultAuth().build());

        Coin coin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID)).get(0);
        assertThat(coin.getNominal(), comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));
        assertTrue(coin.getRulesContainer().hasRule(MSKU_FILTER_RULE));
        assertThat(
                coin.getRulesContainer().get(MSKU_FILTER_RULE).getParams(MSKU_ID),
                contains(msku)
        );
        PromocodeEntry promocodeEntry =
                promocodeEntryDao.selectActiveCurrentFirst(PromocodeEntryDao.PROMO_ID.eqTo(promoId))
                .orElse(null);
        assertThat(promocodeEntry, notNullValue());
    }

    @Test
    public void shouldCreatePromocodeWithFixedNominalWithFirstOrderRule() throws Exception {
        String msku = "1231123";
        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton(msku))
        );
        coinPromoDto.getCoinProps().getUsageRestrictions().setFirstOrder(true);

        Long promoId = createPromo(coinPromoDto);

        coinService.create.createCoin(promoService.getPromo(promoId), defaultAuth().build());

        Coin coin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID)).get(0);
        assertThat(coin.getNominal(), comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL));
        assertTrue(coin.getRulesContainer().hasRule(FIRST_ORDER_CUTTING_RULE));
        assertThat(
                coin.getRulesContainer().get(MSKU_FILTER_RULE).getParams(MSKU_ID),
                contains(msku)
        );
        PromocodeEntry promocodeEntry =
                promocodeEntryDao.selectActiveCurrentFirst(PromocodeEntryDao.PROMO_ID.eqTo(promoId))
                .orElse(null);
        assertThat(promocodeEntry, notNullValue());
    }

    @Test
    public void shouldUpdateCodeOfPromocodePromo() throws Exception {
        String msku = "1231123";
        PromocodePromoDto promoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton(msku))
        );

        Long promoId = createPromo(promoDto);

        promoDto.setId(promoId);
        promoDto.setCode("another_code");

        updatePromo(promoDto);

        PromocodeEntry promocodeEntry =
                promocodeEntryDao.selectActiveCurrentFirst(PromocodeEntryDao.PROMO_ID.eqTo(promoId))
                .orElse(null);
        assertThat(promocodeEntry, notNullValue());
        assertThat(promocodeEntry.getCode(), equalToIgnoringCase("another_code"));
    }

    @Test
    public void shouldCreatePromocodeWithPlatform() throws Exception {
        String msku = "1231123";
        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton(msku))
        );

        Long promoId = createPromo(coinPromoDto);

        coinService.create.createCoin(promoService.getPromo(promoId), defaultAuth().build());

        Coin coin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID)).get(0);
        assertEquals(coinPromoDto.getMarketPlatform(), coin.getPlatform().getApiPlatform());
    }

    @Test
    public void shouldCreatePercentPromocode() throws Exception {
        String msku = "1231123";
        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generatePercentCoinProps(DEFAULT_COIN_PERCENT_NOMINAL, Collections.singleton(msku))
        );

        Long promoId = createPromo(coinPromoDto);

        coinService.create.createCoin(promoService.getPromo(promoId), defaultAuth().build());

        Coin coin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID)).get(0);
        assertThat(coin.getNominal(), comparesEqualTo(DEFAULT_COIN_PERCENT_NOMINAL));
        assertTrue(coin.getRulesContainer().hasRule(MSKU_FILTER_RULE));
        assertThat(
                coin.getRulesContainer().get(MSKU_FILTER_RULE).getParams(MSKU_ID),
                contains(msku)
        );

        PromocodeEntry promocodeEntry =
                promocodeEntryDao.selectActiveCurrentFirst(PromocodeEntryDao.PROMO_ID.eqTo(promoId))
                .orElse(null);
        assertThat(promocodeEntry, notNullValue());
    }

    @Test
    public void shouldUpdatePromocodePromo() throws Exception {

        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps()
        );

        long promoId = createPromo(coinPromoDto);

        PromocodePromoDto fetchedDto = getPromo(promoId);
        Date newEndDate = Date.from(fetchedDto.getEndDate().toInstant().plus(30, ChronoUnit.DAYS));
        fetchedDto.setEndDate(newEndDate);

        updatePromo(fetchedDto);

        fetchedDto = getPromo(promoId);
        assertEquals(newEndDate, fetchedDto.getEndDate());
        assertEquals(
                Date.from(newEndDate.toInstant()
                        .minus(fetchedDto.getCoinProps()
                                .getExpirationPolicy().getParam() + 1, ChronoUnit.DAYS)),
                fetchedDto.getEmissionDateTo()
        );
    }

    @Test
    public void shouldUpdateBudgetThreshold() throws Exception {
        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.emptySet())
        );

        long promoId = createPromo(coinPromoDto);

        BigDecimal budgetThreshold = BigDecimal.valueOf(1000);

        PromocodePromoDto fetchedDto = getPromo(promoId);
        assertNull(fetchedDto.getBudgetThreshold());
        fetchedDto.setBudgetThreshold(budgetThreshold);

        updatePromo(fetchedDto);

        fetchedDto = getPromo(promoId);
        assertThat(fetchedDto.getBudgetThreshold(), comparesEqualTo(budgetThreshold));

        Account account = budgetService.getAccount(promoService.getPromo(promoId).getBudgetAccountId());
        assertThat(account.getBudgetThreshold(), comparesEqualTo(budgetThreshold));
    }

    @Test
    public void shouldUpdateCanBeRestoredFromReserveBudget() throws Exception {
        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.emptySet())
        );

        long promoId = createPromo(coinPromoDto);

        PromocodePromoDto fetchedDto = getPromo(promoId);
        assertFalse(fetchedDto.getCanBeRestoredFromReserveBudget());
        fetchedDto.setCanBeRestoredFromReserveBudget(true);

        updatePromo(fetchedDto);

        fetchedDto = getPromo(promoId);
        assertTrue(fetchedDto.getCanBeRestoredFromReserveBudget());

        Account account = budgetService.getAccount(promoService.getPromo(promoId).getBudgetAccountId());
        assertTrue(account.getCanBeRestoredFromReserveBudget());
    }

    @Test
    public void shouldUpdateEndDate() throws Exception {
        PromocodePromoDto promocodePromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps()
        );

        long promoId = createPromo(promocodePromoDto);

        PromocodePromoDto fetchedDto = getPromo(promoId);
        Date updatedEndDate = Date.from(LocalDate.of(3100, 10, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        fetchedDto.setEndDate(updatedEndDate);
        updatePromo(fetchedDto);

        fetchedDto = getPromo(promoId);
        assertEquals(updatedEndDate, fetchedDto.getEndDate());
    }

    @Test
    public void shouldUpdateCoinProps() throws Exception {
        String firstMsku = "1231123";
        String secondMsku = "1231123";
        BigDecimal firstNominal = DEFAULT_COIN_FIXED_NOMINAL;
        BigDecimal secondNominal = BigDecimal.valueOf(314);

        PromocodePromoDto promocodePromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps(firstNominal, Collections.singleton(firstMsku), "experiments1;experiments2")
        );

        long promoId = createPromo(promocodePromoDto);

        PromocodePromoDto fetchedDto = getPromo(promoId);
        assertThat(fetchedDto.getCoinProps().getNominal(), comparesEqualTo(firstNominal));
        assertThat(
                fetchedDto.getCoinProps().getUsageRestrictions().getMskusRestriction().getItems(), contains(firstMsku));
        assertThat(
                fetchedDto.getCoinProps().getUsageRestrictions().getExperimentRearrFlags(), equalTo("experiments1;experiments2"));
        assertThat(fetchedDto.getCoinProps().getUsageRestrictions().getMskusRestriction().isEnabled(), equalTo(true));
        assertThat(fetchedDto.getCoinProps().getUsageRestrictions().getCategoriesRestriction().getItems(), is(empty()));
        assertThat(
                fetchedDto.getCoinProps().getUsageRestrictions().getCategoriesRestriction().isEnabled(),
                equalTo(false));

        fetchedDto.getCoinProps().setNominal(secondNominal);
        fetchedDto.getCoinProps().getUsageRestrictions().setMskusRestriction(
                new FilterRestriction<>(Collections.singleton(secondMsku), false)
        );
        updatePromo(fetchedDto);
        coinService.search.invalidateCaches();

        fetchedDto = getPromo(promoId);
        assertThat(fetchedDto.getCoinProps().getNominal(), comparesEqualTo(secondNominal));
        assertThat(
                fetchedDto.getCoinProps().getUsageRestrictions().getMskusRestriction().getItems(),
                contains(secondMsku));
        assertThat(fetchedDto.getCoinProps().getUsageRestrictions().getMskusRestriction().isEnabled(), equalTo(true));
        assertThat(fetchedDto.getCoinProps().getUsageRestrictions().getCategoriesRestriction().getItems(), is(empty()));
        assertThat(
                fetchedDto.getCoinProps().getUsageRestrictions().getCategoriesRestriction().isEnabled(),
                equalTo(false));
    }

    @Test
    public void shouldUpdateCoinPromoWithoutChangeOfPreviousCoin() throws Exception {
        String firstMsku = "1231123";
        String secondMsku = "1231123";
        BigDecimal firstNominal = DEFAULT_COIN_FIXED_NOMINAL;
        BigDecimal secondNominal = BigDecimal.valueOf(314);

        PromocodePromoDto promocodePromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps(firstNominal, Collections.singleton(firstMsku))
        );

        long promoId = createPromo(promocodePromoDto);

        coinService.create.createCoin(promoService.getPromo(promoId), defaultAuth(UID).build());

        PromocodePromoDto fetchedDto = getPromo(promoId);
        fetchedDto.getCoinProps().setNominal(secondNominal);
        fetchedDto.getCoinProps().getUsageRestrictions().setMskusRestriction(
                new FilterRestriction<>(Collections.singleton(secondMsku), false)
        );

        updatePromo(fetchedDto);
        coinService.search.invalidateCaches();

        coinService.create.createCoin(promoService.getPromo(promoId), defaultAuth(ANOTHER_UID).build());

        Coin firstCoin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(UID)).get(0);
        assertThat(firstCoin.getNominal(), comparesEqualTo(firstNominal));
        assertTrue(firstCoin.getRulesContainer().hasRule(MSKU_FILTER_RULE));
        assertThat(
                firstCoin.getRulesContainer().get(MSKU_FILTER_RULE).getParams(MSKU_ID),
                contains(firstMsku)
        );

        Coin secondCoin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(ANOTHER_UID)).get(0);
        assertThat(secondCoin.getNominal(), comparesEqualTo(secondNominal));
        assertTrue(secondCoin.getRulesContainer().hasRule(MSKU_FILTER_RULE));
        assertThat(
                secondCoin.getRulesContainer().get(MSKU_FILTER_RULE).getParams(MSKU_ID),
                contains(secondMsku)
        );
    }

    @Test
    public void shouldResetCoinPropertiesWithoutEndDate() throws Exception {
        configurationService.set(RESET_COIN_PROPERTIES_WHEN_PROMOCODE_UPDATE_ANYWAY, true);

        Date firstEndDate = new GregorianCalendar(2030, Calendar.FEBRUARY, 1, 23, 59, 59).getTime();
        Date secondEndDate = new GregorianCalendar(2040, Calendar.FEBRUARY, 1, 23, 59, 59).getTime();

        PromocodePromoBuilder promocodePromoBuilder = PromoUtils.SmartShopping.defaultFixedPromocode()
                .setEndDate(firstEndDate)
                .setExpiration(ExpirationPolicy.toEndOfPromo());

        Promo promo = promoManager.createSmartShoppingPromo(promocodePromoBuilder);
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth(1).build());

        assertEquals(
                firstEndDate,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getRoundedEndDate()
        );

        promoManager.updateCoinPromo(
                promocodePromoBuilder
                        .addCoinRule(MSKU_FILTER_RULE, MSKU_ID, ImmutableSet.of("1", "2", "3"))
                        .setEndDate(secondEndDate)
        );

        mockMvc
                .perform(post("/api/promocode/promo/resetCoinProps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetCoinPropertiesRequest(
                                promo.getPromoId().getId(),
                                "test")))
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk());

        Coin coin = coinService.search.getCoin(coinKey).orElseThrow();
        assertEquals(
                firstEndDate,
                coin.getRoundedEndDate()
        );

        CoinProps coinProps = coinDao.getCoinPropsByIds(Set.of(coin.getCoinPropsId()))
                .get(coin.getCoinPropsId()).orElseThrow();

        assertEquals(
                coinProps.getForwardingCoinPropsId(),
                promoService.getPromo(promo.getId()).getCoinPropsId()
        );
    }

    @Test
    public void shouldUpdateEndDateOnResetCoinProperties() throws Exception {
        configurationService.set(RESET_COIN_PROPERTIES_WHEN_PROMOCODE_UPDATE_ANYWAY, true);

        Date firstEndDate = new GregorianCalendar(2030, Calendar.FEBRUARY, 1, 23, 59, 59).getTime();
        Date secondEndDate = new GregorianCalendar(2040, Calendar.FEBRUARY, 1, 23, 59, 59).getTime();

        PromocodePromoBuilder promocodePromoBuilder = PromoUtils.SmartShopping.defaultFixedPromocode()
                .setEndDate(firstEndDate)
                .setExpiration(ExpirationPolicy.toEndOfPromo());

        Promo promo = promoManager.createSmartShoppingPromo(promocodePromoBuilder);
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth(1).build());

        assertEquals(
                firstEndDate,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getRoundedEndDate()
        );

        promoManager.updateCoinPromo(promocodePromoBuilder.setEndDate(secondEndDate));

        mockMvc
                .perform(post("/api/promocode/promo/resetCoinProps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetCoinPropertiesRequest(
                                promo.getPromoId().getId(),
                                "test")))
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk());

        assertEquals(
                firstEndDate,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getRoundedEndDate()
        );


        clock.setDate(Date.from(firstEndDate.toInstant().plus(1, ChronoUnit.HOURS)));

        coinService.search.invalidateCaches();

        coinService.lifecycle.expireCoins(
                100,
                10,
                100000,
                false
        );

        assertEquals(
                secondEndDate,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getRoundedEndDate()
        );
    }

    @Test
    public void shouldGetPromoByPromostorageId() throws Exception {
        final String PROMOSTORAGE_ID = "#6465";
        PromocodePromoBuilder promocodePromoBuilder = PromoUtils.SmartShopping.defaultFixedPromocode()
                .setPromoStorageId(PROMOSTORAGE_ID)
                .setExpiration(ExpirationPolicy.toEndOfPromo());
        promoManager.createSmartShoppingPromo(promocodePromoBuilder);

        String jsonResponse = mockMvc
                .perform(get(
                        "/api/promocode/promo/promostorage/{id}",
                        URLEncoder.encode(PROMOSTORAGE_ID.substring(1), StandardCharsets.UTF_8)
                ))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        PromocodePromoDto promoDto = objectMapper.readValue(jsonResponse, PromocodePromoDto.class);

        assertEquals(
                promoDto.getPromoStorageId(),
                PROMOSTORAGE_ID
        );
    }

    @Test
    public void shouldCreatePromocodeWithPriority() throws Exception {
        String msku = "1231123";
        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton(msku))
        );

        coinPromoDto.setPriority(100);

        Long promoId = createPromo(coinPromoDto);

        Promo promo = promoService.getPromo(promoId);

        int priority = promo.getPromoParam(PROMO_GLOBAL_PRIORITY).orElseThrow();

        assertThat(priority, comparesEqualTo(100));
    }

    @Test
    public void shouldCreatePromocodeWithShopPromoIdWhenAnaplanIdIsNotEmpty() throws Exception {
        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton("213"))
        );

        Long promoId = createPromo(coinPromoDto);

        Promo promo = promoService.getPromo(promoId);

        String anaplanId = promo.getPromoParam(ANAPLAN_ID).orElseThrow();
        String shopPromoId = promo.getShopPromoId();
        int promoSource = promo.getPromoParam(PROMO_SOURCE).orElseThrow();

        assertThat(anaplanId, comparesEqualTo(PROMO_ANAPLAN_ID));
        assertThat(shopPromoId, comparesEqualTo(PROMO_ANAPLAN_ID));
        assertThat(promoSource, comparesEqualTo(LOYALTY_VALUE));
    }

    @Test
    public void shouldCreatePromocodeWithShopPromoIdWhenAnaplanIdIsEmpty() throws Exception {
        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton("213"))
        );
        coinPromoDto.setAnaplanId(null);

        Long promoId = createPromo(coinPromoDto);

        Promo promo = promoService.getPromo(promoId);

        String shopPromoId = promo.getShopPromoId();
        int promoSource = promo.getPromoParam(PROMO_SOURCE).orElseThrow();

        assertThat(shopPromoId, comparesEqualTo("L" + promoId));
        assertThat(promoSource, comparesEqualTo(LOYALTY_VALUE));
    }

    @Test
    public void shouldUpdatePromocodePromoWithPriority() throws Exception {

        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps()
        );

        coinPromoDto.setPriority(100);

        long promoId = createPromo(coinPromoDto);

        PromocodePromoDto fetchedDto = getPromo(promoId);
        fetchedDto.setPriority(101);
        updatePromo(fetchedDto);

        Promo promo = promoService.getPromo(promoId);

        int priority = promo.getPromoParam(PROMO_GLOBAL_PRIORITY).orElseThrow();

        assertThat(priority, comparesEqualTo(101));
    }

    @Test
    public void shouldCheckPromocode() throws Exception {

        PromocodePromoDto coinPromoDto = createPromoDto(
                CoreCoinCreationReason.FOR_USER_ACTION,
                generateFixedCoinProps()
        );

        createPromo(coinPromoDto);

        PromocodeCheckRequest request = new PromocodeCheckRequest(
                PROMOCODE,
                Date.from(clock.instant()),
                Date.from(clock.instant().plus(100, ChronoUnit.DAYS))
        );
        PromocodeCheckResponse response = checkPromoCode(request);

        assertThat(response, is(notNullValue()));
        assertThat(response.getIsCodeUsed(), equalTo(StringUtils.EMPTY));
    }

    private static CoinPropsDto generateFixedCoinProps() {
        return generateFixedCoinProps(BigDecimal.valueOf(300), Collections.emptySet());
    }

    private static @NotNull
    CoinPropsDto generateFixedCoinProps(
            BigDecimal nominal, @Nullable Set<String> mskusRestriction
    ) {
        return createCoinPropsDto(nominal, mskusRestriction, FIXED, null);
    }

    private static @NotNull
    CoinPropsDto generateFixedCoinProps(
            BigDecimal nominal, @Nullable Set<String> mskusRestriction, String rearrs
    ) {
        return createCoinPropsDto(nominal, mskusRestriction, FIXED, rearrs);
    }

    private static @NotNull
    CoinPropsDto generatePercentCoinProps(
            @NotNull BigDecimal nominal, @Nullable Set<String> mskusRestriction
    ) {
        return createCoinPropsDto(nominal, mskusRestriction, CoreCoinType.PERCENT, null);
    }

    private static @NotNull
    CoinPropsDto createCoinPropsDto(
            @Nullable BigDecimal nominal, @Nullable Set<String> mskusRestriction, @NotNull CoreCoinType percent,
            @Nullable String rearrs
    ) {
        CoinPropsDto coinPropsDto = new CoinPropsDto();
        coinPropsDto.setCoinType(percent);
        ExpirationPolicyDto expirationPolicyDto = new ExpirationPolicyDto(ExpirationPolicy.expireByDays(90));
        coinPropsDto.setExpirationPolicy(expirationPolicyDto);
        coinPropsDto.setNominal(nominal);
        UsageRestrictionsDto usageRestrictions = new UsageRestrictionsDto();
        if (mskusRestriction != null) {
            usageRestrictions.setMskusRestriction(new FilterRestriction<>(mskusRestriction, false));
        }
        if (Strings.isNotBlank(rearrs)) {
            usageRestrictions.setExperimentRearrFlags(rearrs);
        }
        coinPropsDto.setUsageRestrictions(usageRestrictions);
        return coinPropsDto;
    }

    private Long createPromo(PromocodePromoDto couponPromoDto) throws Exception {
        String jsonResponse = mockMvc
                .perform(post("/api/promocode/promo/create")
                        .content(objectMapper.writeValueAsString(couponPromoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.valueOf(objectMapper.readValue(jsonResponse, IdObject.class).getId());
    }

    private PromocodeCheckResponse checkPromoCode(PromocodeCheckRequest request) throws Exception {
        String jsonResponse = mockMvc
                .perform(post("/api/promocode/promo/checkcode")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(jsonResponse, PromocodeCheckResponse.class);
    }

    private void updatePromo(PromocodePromoDto coinPromoDto) throws Exception {
        mockMvc
                .perform(put("/api/promocode/promo/update")
                        .content(objectMapper.writeValueAsString(coinPromoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

    private PromocodePromoDto getPromo(long promoId) throws Exception {
        String jsonResponse = mockMvc
                .perform(get("/api/promocode/promo/{id}", promoId))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(jsonResponse, PromocodePromoDto.class);
    }

    private PromocodePromoDto createPromoDto(
            @Nonnull CoreCoinCreationReason reason,
            @Nullable CoinPropsDto coinPropsDto
    ) {
        return createPromoDto(reason, coinPropsDto,
                NominalStrategy.DefaultStrategy.instance()
        );
    }

    private PromocodePromoDto createPromoDto(
            @Nonnull CoreCoinCreationReason reason,
            @Nullable CoinPropsDto coinPropsDto,
            NominalStrategy nominalStrategy
    ) {
        PromocodePromoDto promoDto = new PromocodePromoDto();
        promoDto.setGenerateCode(false);
        promoDto.setCode(PROMOCODE);
        promoDto.setMarketPlatform(MarketPlatform.BLUE);
        promoDto.setPromoSubType(PromoSubType.MARKET_BONUS);
        promoDto.setName("some name");
        promoDto.setStatus(PromoStatus.ACTIVE);
        promoDto.setDescription("bla bla");
        promoDto.setTicketNumber(TEST_TICKET_OK);
        promoDto.setCurrentBudget(BigDecimal.valueOf(3000));
        promoDto.setCurrentEmissionBudget(BigDecimal.valueOf(3000));
        promoDto.setStartDate(Date.from(clock.instant()));
        promoDto.setEndDate(
                Date.from(clock.instant().plus(150, ChronoUnit.DAYS)));
        promoDto.setEmissionDateFrom(
                Date.from(clock.instant()));
        promoDto.setEmissionDateTo(
                Date.from(clock.instant().plus(6, ChronoUnit.DAYS)));
        promoDto.setCanBeRestoredFromReserveBudget(false);
        promoDto.setConversion(BigDecimal.valueOf(100));
        promoDto.setCoinCreationReason(reason);
        promoDto.setAnaplanId(PROMO_ANAPLAN_ID);
        if (reason.getAdminInfo().hasAnalyticName()) {
            promoDto.setAnalyticName("some analytic name");
        }

        if (coinPropsDto != null) {
            promoDto.setCoinProps(coinPropsDto);
        }

        return applyNominalStrategy(promoDto, coinPropsDto, nominalStrategy);
    }

    private static PromocodePromoDto applyNominalStrategy(
            PromocodePromoDto promoDto,
            @Nullable CoinPropsDto coinPropsDto,
            NominalStrategy nominalStrategy
    ) {
        promoDto.setNominalStrategy(nominalStrategy);

        return nominalStrategy.apply(new NominalStrategy.Visitor<>() {
            @Override
            public PromocodePromoDto visit(NominalStrategy.DefaultStrategy defaultStrategy) {
                if (coinPropsDto == null) {
                    return promoDto;
                }
                switch (coinPropsDto.getCoinType()) {
                    case FIXED:
                        promoDto.setConversion(BigDecimal.valueOf(100));
                        break;
                    case PERCENT:
                        promoDto.setConversion(BigDecimal.valueOf(100));
                        promoDto.setAverageBill(BigDecimal.valueOf(300));
                        promoDto.setBindOnlyOnce(true);
                        break;
                    default:
                        break;
                }
                return promoDto;
            }

            @Override
            public PromocodePromoDto visit(
                    NominalStrategy.PercentOfFilteredOrderItems percentOfFilteredOrderItems
            ) {
                if (coinPropsDto == null || coinPropsDto.getCoinType() != FIXED) {
                    throw new AssertionError("Unsupported coin type for this strategy: " + coinPropsDto);
                }
                return promoDto;
            }
        });
    }
}
