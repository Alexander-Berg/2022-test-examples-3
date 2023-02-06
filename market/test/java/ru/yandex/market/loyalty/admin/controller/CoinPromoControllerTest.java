package ru.yandex.market.loyalty.admin.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.controller.dto.AvatarImageWithLink;
import ru.yandex.market.loyalty.admin.controller.dto.CoinCreationReasonDto;
import ru.yandex.market.loyalty.admin.controller.dto.CoinDescriptionDto;
import ru.yandex.market.loyalty.admin.controller.dto.CoinPromoDto;
import ru.yandex.market.loyalty.admin.controller.dto.CoinPropsDto;
import ru.yandex.market.loyalty.admin.controller.dto.ExpirationPolicyDto;
import ru.yandex.market.loyalty.admin.controller.dto.IdObject;
import ru.yandex.market.loyalty.admin.controller.dto.TriggerActionDto;
import ru.yandex.market.loyalty.admin.controller.dto.TriggerDto;
import ru.yandex.market.loyalty.admin.controller.dto.TriggerRestrictionDto;
import ru.yandex.market.loyalty.admin.controller.dto.UsageRestrictionsDto;
import ru.yandex.market.loyalty.admin.controller.dto.UsageRestrictionsDto.FilterRestriction;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.coin.CoinCreationReason;
import ru.yandex.market.loyalty.core.dao.PromoDao;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.accounting.Account;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinDescription;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoinProps;
import ru.yandex.market.loyalty.core.model.coin.CoinSearchRequest;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.promo.NominalStrategy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.avatar.AvatarImageId;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.trigger.actions.TriggerActionType;
import ru.yandex.market.loyalty.core.trigger.restrictions.SetRelation;
import ru.yandex.market.loyalty.core.trigger.restrictions.SetWithRelationDto;
import ru.yandex.market.loyalty.core.trigger.restrictions.actiononce.ActionOnceRestrictionBody;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static NMarket.Common.Promo.Promo.ESourceType.LOYALTY_VALUE;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdminMockConfigurer.StartrekServiceConfiguration.TEST_TICKET_OK;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason.ORDER;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason.ORDER_DYNAMIC;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinType.FIXED;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.CREATE_INACTIVE_COIN;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MIN_ORDER_TOTAL;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType.MANDATORY_TRIGGERS;
import static ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes.ORDER_STATUS_UPDATED;
import static ru.yandex.market.loyalty.core.rule.RuleType.MIN_ORDER_TOTAL_CUTTING_RULE;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.trigger.actions.TriggerActionTypes.CREATE_COIN_ACTION;
import static ru.yandex.market.loyalty.core.trigger.actions.TriggerActionTypes.SEND_COUPON_BY_EMAIL_ACTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.ACTION_ONCE_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.ORDER_MSKU_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.ORDER_STATUS_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.actiononce.ActionOnceRestrictionType.CHECK_ACTION_PERIOD;
import static ru.yandex.market.loyalty.core.trigger.restrictions.actiononce.ActionOnceRestrictionType.CHECK_USER;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_ITEMS_COUNT;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_ITEM_PRICE;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrderItem;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.EventFactory.DEFAULT_ORDER_STATUS;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withItem;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUid;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_IMAGE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_PERCENT_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_OUTGOING_LINK;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_COINS_LIMIT;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.swallowException;

@TestFor(CoinPromoController.class)
public class CoinPromoControllerTest extends MarketLoyaltyAdminMockedDbTest {
    private static final long UID = 123L;
    private static final long ANOTHER_UID = 1231L;
    private static final TypeReference<List<CoinCreationReasonDto>> COIN_CREATION_REASON_LIST_TYPE_REF =
            new TypeReference<>() {
    };
    private static final int DEFAULT_COIN_PERCENT_OF_ORDER_ITEMS = 15;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PromoService promoService;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
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
    private PromoDao promoDao;
    @Autowired
    private CoinService coinService;

    @Test
    public void shouldReturnCoinReasons() throws Exception {
        String jsonResponse = mockMvc
                .perform(get("/api/coin/promo/possibleReasons").with(csrf()))
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
                        Arrays.stream(CoinCreationReason.values())
                                .filter(r -> r != CoinCreationReason.UNKNOWN)
                                .map(Matchers::equalTo)
                                .collect(Collectors.toList())
                )
        );
    }

    @Test
    public void shouldCreatePromoWithPercentNominalStrategy() throws Exception {
        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(null, Collections.emptySet()),
                generateTrigger(List.of(
                        createActionOnceRestriction(),
                        createOrderStatusRestriction(),
                        createMskuRestriction(
                                "614777014",
                                "614778016"
                        )
                )),
                generateCoinDescription("Номинал монеты - {nominal}"),
                createPercentNominalStrategy()
        );

        createPromo(coinPromoDto);

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(UID),
                withItem(defaultOrderItem().setItemKey(ItemKey.ofFeedOffer(1L, "1")).setMsku(1L)),
                withItem(defaultOrderItem().setItemKey(ItemKey.ofFeedOffer(2L, "2")).setMsku(614778016L))));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        Coin coin = coinService.search.getCoinsByUid(UID, DEFAULT_COINS_LIMIT).get(0);

        BigDecimal expectedNominal = DEFAULT_ITEM_PRICE.multiply(DEFAULT_ITEMS_COUNT)
                .multiply(BigDecimal.valueOf(DEFAULT_COIN_PERCENT_OF_ORDER_ITEMS))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        assertThat(coin.getNominal(), comparesEqualTo(expectedNominal));

        CoinDescription coinDesc =
                coinService.search.getCoinDescriptionsFromCacheOrLoadDynamicallyForCoins(Collections.singletonList(coin))
                        .get(coin.getCoinKey());

        assertEquals("Номинал монеты - " + expectedNominal, coinDesc.getDescription());
    }

    @Test
    public void shouldCreatePromo() throws Exception {
        String msku = "1231123";
        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton(msku)),
                generateTrigger(),
                generateCoinDescription()
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
    }

    private static NominalStrategy.PercentOfFilteredOrderItems createPercentNominalStrategy() {
        NominalStrategy.PercentOfFilteredOrderItems percentOfFilteredOrderItems =
                new NominalStrategy.PercentOfFilteredOrderItems();
        percentOfFilteredOrderItems.setPercent(DEFAULT_COIN_PERCENT_OF_ORDER_ITEMS);
        return percentOfFilteredOrderItems;
    }

    @Test
    public void shouldCreateNoTriggerPromo() throws Exception {
        String msku = "1231123";
        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton(msku)),
                null,
                generateCoinDescription()
        );

        Long promoId = createPromo(coinPromoDto);

        CoinPromoDto coinPromoDtoRestored = getPromo(promoId);

        assertThat(
                coinPromoDtoRestored, allOf(
                        hasProperty("emissionDateFrom", equalTo(coinPromoDto.getEmissionDateFrom())),
                        hasProperty("emissionDateTo", equalTo(coinPromoDto.getEmissionDateTo())),
                        hasProperty("trigger", is(nullValue())),
                        hasProperty("budgetThreshold", is(nullValue())),
                        hasProperty("canBeRestoredFromReserveBudget",
                                equalTo(coinPromoDto.getCanBeRestoredFromReserveBudget()))
                )
        );
    }

    @Test
    public void shouldCreateCoinNoTriggerPromo() throws Exception {
        String msku = "1231123";
        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton(msku)),
                null,
                generateCoinDescription()
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
    }

    @Test
    public void shouldCreateCoinWithPlatform() throws Exception {
        String msku = "1231123";
        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton(msku)),
                null,
                generateCoinDescription()
        );

        Long promoId = createPromo(coinPromoDto);

        coinService.create.createCoin(promoService.getPromo(promoId), defaultAuth().build());

        Coin coin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID)).get(0);
        assertEquals(coinPromoDto.getMarketPlatform(), coin.getPlatform().getApiPlatform());
    }

    @Test
    public void shouldCreatePercentPromo() throws Exception {
        String msku = "1231123";
        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generatePercentCoinProps(DEFAULT_COIN_PERCENT_NOMINAL, Collections.singleton(msku)),
                generateTrigger(),
                generateCoinDescription()
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
    }

    @Test
    public void shouldNotAllowCreatePercentPromoWithoutActionOnceRestriction() throws Exception {
        String msku = "1231123";
        TriggerRestrictionDto orderStatusRestriction = createOrderStatusRestriction();

        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generatePercentCoinProps(DEFAULT_COIN_PERCENT_NOMINAL, Collections.singleton(msku)),
                generateTrigger(Collections.singletonList(orderStatusRestriction)),
                generateCoinDescription()
        );

        createPromoWithFail(coinPromoDto);
    }

    @Test
    public void shouldCreateCoinByCreatedTrigger() throws Exception {
        String msku = "1231123";
        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton(msku)),
                generateTrigger(),
                generateCoinDescription()
        );

        Long promoId = createPromo(coinPromoDto);

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(UID)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        Coin coin = coinService.search.getCoinsByUid(UID, DEFAULT_COINS_LIMIT).get(0);
        assertEquals(promoId, coin.getPromoId());
    }

    @Test
    public void shouldCreateCoinByCreatedTriggerWithOldCode() throws Exception {
        String msku = "1231123";
        TriggerDto trigger = generateTrigger();
        List<TriggerRestrictionDto> restrictions = trigger.getRestrictions();
        restrictions.forEach(restriction -> restriction.setBody("103"));
        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.singleton(msku)),
                trigger,
                generateCoinDescription()
        );

        Long promoId = createPromo(coinPromoDto);

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(UID)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        Coin coin = coinService.search.getInactiveCoinsByUid(UID, CoreMarketPlatform.BLUE).get(0);
        assertEquals(promoId, coin.getPromoId());
    }

    @Test
    public void shouldUpdateCoinPromo() throws Exception {
        Date emissionDateFrom = Date.from(clock.instant().plus(1, ChronoUnit.DAYS));
        Date emissionDateTo = Date.from(clock.instant().plus(30, ChronoUnit.DAYS));
        Date changedEmissionDateTo = Date.from(clock.instant().plus(30, ChronoUnit.DAYS));

        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(),
                generateTrigger(),
                generateCoinDescription()
        );
        coinPromoDto.setEmissionDateFrom(emissionDateFrom);
        coinPromoDto.setEmissionDateTo(emissionDateTo);

        long promoId = createPromo(coinPromoDto);

        CoinPromoDto fetchedDto = getPromo(promoId);
        fetchedDto.setEmissionDateTo(changedEmissionDateTo);

        updatePromo(fetchedDto);

        fetchedDto = getPromo(promoId);
        assertEquals(emissionDateFrom, fetchedDto.getEmissionDateFrom());
        assertEquals(changedEmissionDateTo, fetchedDto.getEmissionDateTo());
    }

    @Test
    public void shouldUpdateCoinPromoAndNotEraseTagParams() throws Exception {
        final Promo promo = promoManager.createSmartShoppingPromo(defaultFixed()
                .setCreateInactiveCoin());
        long promoId = promo.getPromoId().getId();

        CoinPromoDto fetchedDto = getPromo(promoId);

        updatePromo(fetchedDto);

        assertTrue(
                promoDao.getPromoParams(Collections.singleton(promoId))
                        .get(promoId)
                        .keys()
                        .contains(CREATE_INACTIVE_COIN)
        );
    }

    @Test
    public void shouldFixMarketdiscount2252() throws Exception {
        final ImmutableSet<String> mskus = ImmutableSet.of("1", "2", "3");

        final CoinPropsDto coinPropsDto = createCoinPropsDto(
                BigDecimal.valueOf(100L), mskus, FIXED);

        CoinPromoDto coinPromoDto = createPromo(
                ORDER,
                coinPropsDto,
                generateTrigger(),
                generateCoinDescription()
        );

        long promoId = createPromo(coinPromoDto);

        CoinPromoDto fetchedDto = getPromo(promoId);
        UsageRestrictionsDto usageRestrictions = new UsageRestrictionsDto();
        usageRestrictions.setMskusRestriction(new FilterRestriction<>(mskus, false, false));
        coinPropsDto.setUsageRestrictions(usageRestrictions);
        fetchedDto.setCoinProps(coinPropsDto);

        updatePromo(fetchedDto);
        coinService.search.invalidateCaches();

        fetchedDto = getPromo(promoId);

        CoinPropsDto coinProps = fetchedDto.getCoinProps();
        assertNotNull(coinProps);
        assertThat(
                coinProps.getUsageRestrictions().getMskusRestriction(),
                allOf(
                        hasProperty("enabled", equalTo(false)),
                        hasProperty("items", empty())
                )
        );
    }

    @Test
    public void shouldUpdateBudgetThreshold() throws Exception {
        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.emptySet()),
                generateTrigger(),
                generateCoinDescription()
        );

        long promoId = createPromo(coinPromoDto);

        BigDecimal budgetThreshold = BigDecimal.valueOf(1000);

        CoinPromoDto fetchedDto = getPromo(promoId);
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
        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(DEFAULT_COIN_FIXED_NOMINAL, Collections.emptySet()),
                generateTrigger(),
                generateCoinDescription()
        );

        long promoId = createPromo(coinPromoDto);

        CoinPromoDto fetchedDto = getPromo(promoId);
        assertFalse(fetchedDto.getCanBeRestoredFromReserveBudget());
        fetchedDto.setCanBeRestoredFromReserveBudget(true);

        updatePromo(fetchedDto);

        fetchedDto = getPromo(promoId);
        assertTrue(fetchedDto.getCanBeRestoredFromReserveBudget());

        Account account = budgetService.getAccount(promoService.getPromo(promoId).getBudgetAccountId());
        assertTrue(account.getCanBeRestoredFromReserveBudget());
    }

    @Test
    public void shouldUpdateCoinDescription() throws Exception {
        String firstTitle = "some title";
        String firstOutgoingLink = null;
        String secondTitle = "another title";
        String secondOutgoingLink = "https://beru.ru/";
        String secondRestrictionDescription = "На любой заказ из категории 'Х'";

        CoinDescriptionDto coinDescriptionDto = generateCoinDescription();
        coinDescriptionDto.setTitle(firstTitle);
        coinDescriptionDto.setOutgoingLink(firstOutgoingLink);
        CoinPromoDto couponPromoDto = createPromo(
                ORDER, generateFixedCoinProps(),
                generateTrigger(),
                coinDescriptionDto
        );

        long promoId = createPromo(couponPromoDto);

        CoinPromoDto fetchedDto = getPromo(promoId);
        CoinDescriptionDto coinDescription = fetchedDto.getCoinDescription();
        assertNotNull(coinDescription);
        assertEquals(firstTitle, coinDescription.getTitle());
        assertEquals(firstOutgoingLink, coinDescription.getOutgoingLink());
        assertNull(coinDescription.getRestrictionDescription());

        coinDescription.setTitle(secondTitle);
        coinDescription.setRestrictionDescription(secondRestrictionDescription);
        coinDescription.setOutgoingLink(secondOutgoingLink);
        updatePromo(fetchedDto);

        fetchedDto = getPromo(promoId);
        coinDescription = fetchedDto.getCoinDescription();
        assertNotNull(coinDescription);
        assertEquals(secondTitle, coinDescription.getTitle());
        assertEquals(secondOutgoingLink, coinDescription.getOutgoingLink());
        assertEquals(secondRestrictionDescription, coinDescription.getRestrictionDescription());
    }

    @Test
    public void shouldUpdateEndDAte() throws Exception {

        CoinPromoDto couponPromoDto = createPromo(
                ORDER, generateFixedCoinProps(),
                generateTrigger(),
                generateCoinDescription()
        );

        long promoId = createPromo(couponPromoDto);

        CoinPromoDto fetchedDto = getPromo(promoId);
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

        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(firstNominal, Collections.singleton(firstMsku)),
                generateTrigger(),
                generateCoinDescription()
        );

        long promoId = createPromo(coinPromoDto);

        CoinPromoDto fetchedDto = getPromo(promoId);
        CoinPropsDto coinProps = fetchedDto.getCoinProps();
        assertNotNull(coinProps);
        assertThat(coinProps.getNominal(), comparesEqualTo(firstNominal));
        assertThat(coinProps.getUsageRestrictions().getMskusRestriction().getItems(), contains(firstMsku));
        assertThat(coinProps.getUsageRestrictions().getMskusRestriction().isEnabled(), equalTo(true));
        assertThat(coinProps.getUsageRestrictions().getCategoriesRestriction().getItems(), is(empty()));
        assertThat(coinProps.getUsageRestrictions().getCategoriesRestriction().isEnabled(), equalTo(false));

        coinProps.setNominal(secondNominal);
        coinProps.getUsageRestrictions().setMskusRestriction(
                new FilterRestriction<>(Collections.singleton(secondMsku), false)
        );
        updatePromo(fetchedDto);
        coinService.search.invalidateCaches();
        fetchedDto = getPromo(promoId);
        coinProps = fetchedDto.getCoinProps();
        assertNotNull(coinProps);
        assertThat(coinProps.getNominal(), comparesEqualTo(secondNominal));
        assertThat(coinProps.getUsageRestrictions().getMskusRestriction().getItems(), contains(secondMsku));
        assertThat(coinProps.getUsageRestrictions().getMskusRestriction().isEnabled(), equalTo(true));
        assertThat(coinProps.getUsageRestrictions().getCategoriesRestriction().getItems(), is(empty()));
        assertThat(coinProps.getUsageRestrictions().getCategoriesRestriction().isEnabled(), equalTo(false));
    }

    @Test
    public void shouldUpdateCoinPromoWithoutChangeOfPreviousCoin() throws Exception {
        String firstMsku = "1231123";
        String secondMsku = "1231123";
        BigDecimal firstNominal = DEFAULT_COIN_FIXED_NOMINAL;
        BigDecimal secondNominal = BigDecimal.valueOf(314);

        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generateFixedCoinProps(firstNominal, Collections.singleton(firstMsku)),
                generateTrigger(),
                generateCoinDescription()
        );

        long promoId = createPromo(coinPromoDto);

        coinService.create.createCoin(promoService.getPromo(promoId), defaultAuth(UID).build());

        CoinPromoDto fetchedDto = getPromo(promoId);
        CoinPropsDto coinProps = fetchedDto.getCoinProps();
        assertNotNull(coinProps);
        coinProps.setNominal(secondNominal);
        coinProps.getUsageRestrictions().setMskusRestriction(
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
    public void shouldCreateDynamicCoinPromo() throws Exception {
        CoinPromoDto coinPromoDto = createPromo(
                ORDER_DYNAMIC,
                null,
                generateTrigger(),
                null
        );

        Long promoId = createPromo(coinPromoDto);

        Promo promo = promoService.getPromo(promoId);
        coinService.create.createCoin(promo, defaultAuth(DEFAULT_UID)
                .setCoinProps(CoinProps.builder()
                        .setNominal(DEFAULT_COIN_FIXED_NOMINAL)
                        .setType(FIXED)
                        .setPromoData(promo)
                        .setExpirationPolicy(ExpirationPolicy.expireByDays(30))
                        .build())
                .setCoinDescription(CoinDescription.builder()
                        .setDescription("Обычная монетка")
                        .setInactiveDescription("Неактивная обычная монетка")
                        .setPromoId(promoId)
                        .setAvatarImageId(new AvatarImageId(0, "image.link"))
                        .setOutgoingLink("https://m.beru.ru/")
                        .build())
                .build()
        );

        Coin coin = coinService.search.getActiveCoinsByUid(CoinSearchRequest.forUserId(DEFAULT_UID)).get(0);
        assertThat(
                coin,
                allOf(
                        hasProperty("nominal", comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)),
                        hasProperty("type", comparesEqualTo(FIXED)),
                        hasProperty("promoId", equalTo(promoId)),
                        hasProperty("budgetAccountId", equalTo(promo.getBudgetAccountId())),
                        hasProperty("spendingAccountId", equalTo(promo.getSpendingAccountId()))
                )
        );

        CoinDescription description = coinDao.getCoinDescriptionById(coin.getCoinDescriptionId());
        assertThat(
                description,
                allOf(
                        hasProperty("description", equalTo("Обычная монетка")),
                        hasProperty("inactiveDescription", equalTo("Неактивная обычная монетка")),
                        hasProperty("promoId", equalTo(promoId))
                )
        );
    }


    @Test
    public void shouldNotAllowCreatePromoWithActionOnceRestrictionCheckActionPeriodAndSendCouponAction() throws Exception {
        String msku = "1231123";
        TriggerRestrictionDto orderStatusRestriction = createOrderStatusRestriction();
        TriggerRestrictionDto actionOnceRestriction = new TriggerRestrictionDto();
        orderStatusRestriction.setFactoryName(ACTION_ONCE_RESTRICTION.getFactoryName());
        orderStatusRestriction.setBody(objectMapper.writeValueAsString(new ActionOnceRestrictionBody(CHECK_ACTION_PERIOD)));

        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generatePercentCoinProps(DEFAULT_COIN_PERCENT_NOMINAL, Collections.singleton(msku)),
                generateTrigger(SEND_COUPON_BY_EMAIL_ACTION, "", asList(orderStatusRestriction, actionOnceRestriction)),
                generateCoinDescription()
        );

        createPromoWithFail(coinPromoDto);
    }

    @Test
    public void shouldAllowCreatePromoWitActionOnceRestrictionCheckActionPeriodAndCreateCoinAction() throws Exception {
        String msku = "1231123";
        TriggerRestrictionDto orderStatusRestriction = createOrderStatusRestriction();
        TriggerRestrictionDto actionOnceRestriction = new TriggerRestrictionDto();
        actionOnceRestriction.setFactoryName(ACTION_ONCE_RESTRICTION.getFactoryName());
        actionOnceRestriction.setBody(objectMapper.writeValueAsString(new ActionOnceRestrictionBody(CHECK_ACTION_PERIOD)));

        CoinPromoDto coinPromoDto = createPromo(
                ORDER, generatePercentCoinProps(DEFAULT_COIN_PERCENT_NOMINAL, Collections.singleton(msku)),
                generateTrigger(CREATE_COIN_ACTION, "{}", asList(orderStatusRestriction, actionOnceRestriction)),
                generateCoinDescription()
        );

        createPromo(coinPromoDto);
    }

    @Test
    public void shouldUpdateEndDateOnResetCoinProperties() throws Exception {
        Date firstEndDate = new GregorianCalendar(2030, Calendar.FEBRUARY, 1, 23, 59, 59).getTime();
        Date secondEndDate = new GregorianCalendar(2040, Calendar.FEBRUARY, 1, 23, 59, 59).getTime();

        SmartShoppingPromoBuilder<?> coinPromoBuilder = PromoUtils.SmartShopping.defaultFixed()
                .setEndDate(firstEndDate)
                .setExpiration(ExpirationPolicy.toEndOfPromo());

        Promo promo = promoManager.createSmartShoppingPromo(coinPromoBuilder);
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth(1).build());

        assertEquals(
                firstEndDate,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getRoundedEndDate()
        );

        promoManager.updateCoinPromo(coinPromoBuilder.setEndDate(secondEndDate));

        mockMvc
                .perform(post("/api/coin/promo/resetCoinProps")
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
    public void shouldExpireCoinOnResetCoinProperties() throws Exception {
        Date firstEndDate = new GregorianCalendar(2030, Calendar.FEBRUARY, 1, 23, 59, 59).getTime();

        SmartShoppingPromoBuilder<?> coinPromoBuilder = PromoUtils.SmartShopping.defaultFixed()
                .setEndDate(firstEndDate)
                .setExpiration(ExpirationPolicy.toEndOfPromo());

        Promo promo = promoManager.createSmartShoppingPromo(coinPromoBuilder);
        CoinKey coinKey = coinService.create.createCoin(promo, defaultAuth(1).build());

        assertEquals(
                firstEndDate,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getRoundedEndDate()
        );

        promoManager.updateCoinPromo(coinPromoBuilder.addCoinRule(MIN_ORDER_TOTAL_CUTTING_RULE, MIN_ORDER_TOTAL, BigDecimal.valueOf(1500)));

        mockMvc
                .perform(post("/api/coin/promo/resetCoinProps")
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
                CoreCoinStatus.EXPIRED,
                coinService.search.getCoin(coinKey).orElseThrow(AssertionError::new).getStatus()
        );
    }

    @Test
    public void shouldGetPromoByPromostorageId() throws Exception {
        final String PROMOSTORAGE_ID = "#6465";
        SmartShoppingPromoBuilder<?> builder = PromoUtils.SmartShopping.defaultFixed()
                .setPromoStorageId(PROMOSTORAGE_ID)
                .setExpiration(ExpirationPolicy.toEndOfPromo());
        promoManager.createSmartShoppingPromo(builder);

        String jsonResponse = mockMvc
                .perform(get("/api/coin/promo/promostorage/{id}",
                        URLEncoder.encode(PROMOSTORAGE_ID.substring(1), StandardCharsets.UTF_8)))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        CoinPromoDto promoDto = objectMapper.readValue(jsonResponse, CoinPromoDto.class);

        assertEquals(
                promoDto.getPromoStorageId(),
                PROMOSTORAGE_ID
        );
    }

    private TriggerDto generateTrigger() {
        return generateTrigger(List.of(
                createOrderStatusRestriction(),
                createActionOnceRestriction()
        ));
    }

    private TriggerRestrictionDto createActionOnceRestriction() {
        TriggerRestrictionDto actionOnceRestriction = new TriggerRestrictionDto();
        actionOnceRestriction.setFactoryName(ACTION_ONCE_RESTRICTION.getFactoryName());
        actionOnceRestriction.setBody(swallowException(() -> objectMapper.writeValueAsString(new ActionOnceRestrictionBody(CHECK_USER))));
        return actionOnceRestriction;
    }

    private static TriggerRestrictionDto createOrderStatusRestriction() {
        TriggerRestrictionDto orderStatusRestriction = new TriggerRestrictionDto();
        orderStatusRestriction.setFactoryName(ORDER_STATUS_RESTRICTION.getFactoryName());
        orderStatusRestriction.setBody(DEFAULT_ORDER_STATUS.getCode());
        return orderStatusRestriction;
    }

    private TriggerRestrictionDto createMskuRestriction(String... mskus) {
        TriggerRestrictionDto mskuRestriction = new TriggerRestrictionDto();
        mskuRestriction.setFactoryName(ORDER_MSKU_RESTRICTION.getFactoryName());
        SetWithRelationDto<String> setWithRelationDto = new SetWithRelationDto<>();
        setWithRelationDto.setGivenSet(Set.of(mskus));
        setWithRelationDto.setSetRelation(SetRelation.AT_LEAST_ONE_INCLUDED_IN_SET);
        mskuRestriction.setBody(swallowException(() -> objectMapper.writeValueAsString(setWithRelationDto)));
        return mskuRestriction;
    }

    private static TriggerDto generateTrigger(List<TriggerRestrictionDto> restrictions) {
        return generateTrigger(CREATE_COIN_ACTION, "{}", restrictions);
    }

    private static TriggerDto generateTrigger(
            TriggerActionType<?> actionType, String body, List<TriggerRestrictionDto> restrictions
    ) {
        TriggerDto triggerDto = new TriggerDto();
        TriggerActionDto action = new TriggerActionDto();
        action.setFactoryName(actionType.getFactoryName());
        action.setBody(body);
        triggerDto.setAction(action);
        triggerDto.setTriggerEventType(ORDER_STATUS_UPDATED);
        triggerDto.setTriggerGroupType(MANDATORY_TRIGGERS);
        triggerDto.setRestrictions(restrictions);
        return triggerDto;
    }

    private static CoinDescriptionDto generateCoinDescription() {
        return generateCoinDescription("ОЧЕНЬ выгодная монетка");
    }

    private static CoinDescriptionDto generateCoinDescription(String description) {
        CoinDescriptionDto coinDescriptionDto = new CoinDescriptionDto();
        coinDescriptionDto.setDescription(description);
        coinDescriptionDto.setImage(new AvatarImageWithLink(
                DEFAULT_COIN_IMAGE,
                "http://someUrl"
        ));
        coinDescriptionDto.setBackgroundColor("#aaaaaa");
        coinDescriptionDto.setOutgoingLink(DEFAULT_OUTGOING_LINK);
        return coinDescriptionDto;
    }

    private static CoinPropsDto generateFixedCoinProps() {
        return generateFixedCoinProps(BigDecimal.valueOf(300), Collections.emptySet());
    }

    private static @NotNull
    CoinPropsDto generateFixedCoinProps(BigDecimal nominal, @Nullable Set<String> mskusRestriction) {
        return createCoinPropsDto(nominal, mskusRestriction, FIXED);
    }

    private static @NotNull
    CoinPropsDto generatePercentCoinProps(@NotNull BigDecimal nominal, @Nullable Set<String> mskusRestriction) {
        return createCoinPropsDto(nominal, mskusRestriction, CoreCoinType.PERCENT);
    }

    private static @NotNull
    CoinPropsDto createCoinPropsDto(@Nullable BigDecimal nominal, @Nullable Set<String> mskusRestriction,
                                    @NotNull CoreCoinType percent) {
        CoinPropsDto coinPropsDto = new CoinPropsDto();
        coinPropsDto.setCoinType(percent);
        ExpirationPolicyDto expirationPolicyDto = new ExpirationPolicyDto(ExpirationPolicy.expireByDays(90));
        coinPropsDto.setExpirationPolicy(expirationPolicyDto);
        coinPropsDto.setNominal(nominal);
        UsageRestrictionsDto usageRestrictions = new UsageRestrictionsDto();
        if (mskusRestriction != null) {
            usageRestrictions.setMskusRestriction(new FilterRestriction<>(mskusRestriction, false));
        }
        coinPropsDto.setUsageRestrictions(usageRestrictions);
        return coinPropsDto;
    }

    private Long createPromo(CoinPromoDto coinPromoDto) throws Exception {
        String jsonResponse = mockMvc
                .perform(post("/api/coin/promo/create")
                        .content(objectMapper.writeValueAsString(coinPromoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.valueOf(objectMapper.readValue(jsonResponse, IdObject.class).getId());
    }

    private void createPromoWithFail(CoinPromoDto couponPromoDto) throws Exception {
        mockMvc
                .perform(post("/api/coin/promo/create")
                        .content(objectMapper.writeValueAsString(couponPromoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isUnprocessableEntity());
    }

    private void updatePromo(CoinPromoDto coinPromoDto) throws Exception {
        mockMvc
                .perform(put("/api/coin/promo/update")
                        .content(objectMapper.writeValueAsString(coinPromoDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
    }

    private CoinPromoDto getPromo(long promoId) throws Exception {
        String jsonResponse = mockMvc
                .perform(get("/api/coin/promo/{id}", promoId))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(jsonResponse, CoinPromoDto.class);
    }

    private static CoinPromoDto createPromo(
            @NotNull CoreCoinCreationReason reason, @Nullable CoinPropsDto coinPropsDto,
            @Nullable TriggerDto trigger,
            @Nullable CoinDescriptionDto coinDescriptionDto
    ) {
        return createPromo(reason, coinPropsDto, trigger, coinDescriptionDto,
                NominalStrategy.DefaultStrategy.instance());
    }

    private static CoinPromoDto createPromo(
            @NotNull CoreCoinCreationReason reason, @Nullable CoinPropsDto coinPropsDto,
            @Nullable TriggerDto trigger,
            @Nullable CoinDescriptionDto coinDescriptionDto,
            NominalStrategy nominalStrategy
    ) {
        CoinPromoDto promoDto = new CoinPromoDto();
        promoDto.setTrigger(trigger);
        promoDto.setCoinDescription(coinDescriptionDto);
        promoDto.setMarketPlatform(MarketPlatform.BLUE);
        promoDto.setPromoSubType(PromoSubType.MARKET_BONUS);
        promoDto.setName("some name");
        promoDto.setStatus(PromoStatus.ACTIVE);
        promoDto.setDescription("bla bla");
        promoDto.setTicketNumber(TEST_TICKET_OK);
        promoDto.setCurrentBudget(BigDecimal.valueOf(3000));
        promoDto.setCurrentEmissionBudget(BigDecimal.valueOf(3000));
        promoDto.setStartDate(Date.from(LocalDate.of(2017, 10, 10).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        promoDto.setEndDate(Date.from(LocalDate.of(3000, 10, 14).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        promoDto.setEmissionDateFrom(Date.from(LocalDate.of(2017, 10, 10).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        promoDto.setEmissionDateTo(Date.from(LocalDate.of(2900, 10, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        promoDto.setCanBeRestoredFromReserveBudget(false);
        promoDto.setConversion(BigDecimal.valueOf(100));
        promoDto.setCoinCreationReason(reason);
        if (reason.getAdminInfo().hasAnalyticName()) {
            promoDto.setAnalyticName("some analytic name");
        }

        if (coinPropsDto != null) {
            promoDto.setCoinProps(coinPropsDto);
        }
        promoDto.setPromoSource(LOYALTY_VALUE);

        return applyNominalStrategy(promoDto, coinPropsDto, nominalStrategy);
    }

    private static CoinPromoDto applyNominalStrategy(
            CoinPromoDto promoDto, @Nullable CoinPropsDto coinPropsDto, NominalStrategy nominalStrategy
    ) {
        promoDto.setNominalStrategy(nominalStrategy);

        return nominalStrategy.apply(new NominalStrategy.Visitor<>() {
            @Override
            public CoinPromoDto visit(NominalStrategy.DefaultStrategy defaultStrategy) {
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
            public CoinPromoDto visit(
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
