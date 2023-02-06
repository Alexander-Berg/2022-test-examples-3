package ru.yandex.market.loyalty.back.controller.perk;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.perk.PerkStatResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkUnavailableReason;
import ru.yandex.market.loyalty.api.model.web.LoyaltyTag;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserOrder;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.monitor.CoreMonitorType;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.YdbAllUserOrdersService;
import ru.yandex.market.loyalty.core.service.perks.PerkService;
import ru.yandex.market.loyalty.core.service.perks.StaticPerkService;
import ru.yandex.market.loyalty.core.service.perks.StatusFeatureResults;
import ru.yandex.market.loyalty.core.service.perks.impl.GrowingCashbackPerkProcessor;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.monitoring.PushMonitor;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.GROWING_CASHBACK;
import static ru.yandex.market.loyalty.core.model.action.PromoActionParameterName.STATIC_PERK_NAME;
import static ru.yandex.market.loyalty.core.model.action.PromoActionType.STATIC_PERK_ADDITION_ACTION;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_CREATION;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_DELIVERED;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.GROWING_CASHBACK_ENABLED;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.GROWING_CASHBACK_EXPERIMENT_NEW_USERS_ALLOW;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.GROWING_CASHBACK_EXPERIMENT_STATIC_PERK_NAME;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.GROWING_CASHBACK_PROMO_KEYS;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.GROWING_CASHBACK_REARR;
import static ru.yandex.market.loyalty.core.stub.YdbAllUsersOrdersDaoStub.USER_WITH_ORDER;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.DEFAULT_REGION;
import static ru.yandex.market.loyalty.core.utils.PerkAdditionalParamsKey.ORDER_STAGE;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@ActiveProfiles({"monitor-mock-test"})
@TestFor(GrowingCashbackPerkProcessor.class)
public class GrowingCashbackPerkTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final String REARR = "test_rearr";
    private static final List<String> CREATED_STATIC_PERKS = List.of(
            "FIRST_CREATED", "SECOND_CREATED", "THIRD_CREATED"
    );
    private static final List<String> DELIVERED_STATIC_PERKS = List.of(
            "FIRST_DELIVERED", "SECOND_DELIVERED", "THIRD_DELIVERED"
    );
    private static final BigDecimal MIN_ORDER_TOTAL = BigDecimal.ONE;
    private static final BigDecimal CASHBACK_NOMINAL = BigDecimal.TEN;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private StaticPerkService staticPerkService;
    @Autowired
    private PushMonitor pushMonitor;
    @Autowired
    private PromoService promoService;
    @Autowired
    private GrowingCashbackPerkProcessor growingCashbackPerkProcessor;
    @Autowired
    private PerkService perkService;
    @Autowired
    private YdbAllUserOrdersService allUserOrdersService;

    private List<Promo> promos;
    private List<String> promoKeys;
    private BigDecimal maxCashbackTotal;

    @Before
    public void setUp() {
        promos = new ArrayList<>();
        promos.add(promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixedWithMinOrder(CASHBACK_NOMINAL, MIN_ORDER_TOTAL)
                        .setEmissionBudget(BigDecimal.TEN)
                        .addCashbackAction(
                                STATIC_PERK_ADDITION_ACTION, ORDER_CREATION, STATIC_PERK_NAME,
                                CREATED_STATIC_PERKS.get(0)
                        )
                        .addCashbackAction(
                                STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED, STATIC_PERK_NAME,
                                DELIVERED_STATIC_PERKS.get(0)
                        )
        ));
        promos.add(promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixedWithMinOrder(CASHBACK_NOMINAL, MIN_ORDER_TOTAL)
                        .setEmissionBudget(BigDecimal.TEN)
                        .addCashbackAction(
                                STATIC_PERK_ADDITION_ACTION, ORDER_CREATION, STATIC_PERK_NAME,
                                CREATED_STATIC_PERKS.get(1)
                        )
                        .addCashbackAction(
                                STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED, STATIC_PERK_NAME,
                                DELIVERED_STATIC_PERKS.get(1)
                        )
        ));
        promos.add(promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixedWithMinOrder(CASHBACK_NOMINAL, MIN_ORDER_TOTAL)
                        .setEmissionBudget(BigDecimal.TEN)
                        .addCashbackAction(
                                STATIC_PERK_ADDITION_ACTION, ORDER_CREATION, STATIC_PERK_NAME,
                                CREATED_STATIC_PERKS.get(2)
                        )
                        .addCashbackAction(
                                STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED, STATIC_PERK_NAME,
                                DELIVERED_STATIC_PERKS.get(2)
                        )
        ));
        reloadPromoCache();

        promoKeys = new ArrayList<>();
        maxCashbackTotal = BigDecimal.ZERO;
        promos.forEach(promo -> {
            promoKeys.add(promo.getPromoKey());
            maxCashbackTotal = maxCashbackTotal.add(CASHBACK_NOMINAL);
        });
        configurationService.set(GROWING_CASHBACK_EXPERIMENT_NEW_USERS_ALLOW, true);
        configurationService.set(GROWING_CASHBACK_EXPERIMENT_STATIC_PERK_NAME, "growing_exp");
    }

    @Test
    public void shouldPurchasePerkWhenZeroOrders() throws Exception {
        setGrowingCashbackConfig(true, REARR, promoKeys);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(maxCashbackTotal)),
                hasProperty("ordersCount", equalTo((long) promos.size())),
                hasProperty("minOrderTotal", equalTo(MIN_ORDER_TOTAL.longValue())),
                hasProperty("isGotFullReward", equalTo(false)),
                hasProperty("nextStepPromoKey", equalTo(promos.get(0).getPromoKey())),
                hasProperty("unavailableReason", nullValue()),
                hasProperty("promoEndDate",
                        equalTo(promos.get(0).getEndDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString()))
        ))));
    }

    @Test
    public void shouldPurchasePerkWhenOneGrowingOrderCreatedAndUserHasOrder() throws Exception {
        setGrowingCashbackConfig(true, REARR, promoKeys);

        staticPerkService.providePerkToUser(USER_WITH_ORDER, CREATED_STATIC_PERKS.get(0));

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(USER_WITH_ORDER))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(maxCashbackTotal)),
                hasProperty("ordersCount", equalTo((long) promos.size())),
                hasProperty("minOrderTotal", equalTo(MIN_ORDER_TOTAL.longValue())),
                hasProperty("isGotFullReward", equalTo(false)),
                hasProperty("nextStepPromoKey", equalTo(promos.get(1).getPromoKey())),
                hasProperty("unavailableReason", nullValue()),
                hasProperty("promoEndDate",
                        equalTo(promos.get(0).getEndDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString()))
        ))));
    }

    @Test
    public void shouldPurchaseFullRewardPerkWhenAllGrowingOrdersDelivered() throws Exception {
        setGrowingCashbackConfig(true, REARR, promoKeys);

        CREATED_STATIC_PERKS.forEach(perk -> staticPerkService.providePerkToUser(USER_WITH_ORDER, perk));
        DELIVERED_STATIC_PERKS.forEach(perk -> staticPerkService.providePerkToUser(USER_WITH_ORDER, perk));

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(USER_WITH_ORDER))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(maxCashbackTotal)),
                hasProperty("ordersCount", equalTo((long) promos.size())),
                hasProperty("minOrderTotal", equalTo(MIN_ORDER_TOTAL.longValue())),
                hasProperty("isGotFullReward", equalTo(true)),
                hasProperty("nextStepPromoKey", nullValue()),
                hasProperty("unavailableReason", nullValue()),
                hasProperty("promoEndDate",
                        equalTo(promos.get(0).getEndDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString()))
        ))));
    }

    @Test
    public void shouldNotPurchasePerkWhenThereIsNoRearr() throws Exception {
        setGrowingCashbackConfig(true, REARR, promoKeys);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                //NO REARR .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("unavailableReason", equalTo(PerkUnavailableReason.NOT_IN_EXPERIMENT))
        ))));
    }

    @Test
    public void shouldPurchasePerkWhenThereIsNoRearrButRearrCheckIsDisabled() throws Exception {
        //disabled rearr check
        setGrowingCashbackConfig(true, null, promoKeys);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                //NO REARR .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(maxCashbackTotal)),
                hasProperty("ordersCount", equalTo((long) promos.size())),
                hasProperty("minOrderTotal", equalTo(MIN_ORDER_TOTAL.longValue())),
                hasProperty("isGotFullReward", equalTo(false)),
                hasProperty("nextStepPromoKey", equalTo(promos.get(0).getPromoKey())),
                hasProperty("unavailableReason", nullValue()),
                hasProperty("promoEndDate",
                        equalTo(promos.get(0).getEndDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString()))
        ))));
    }

    @Test
    public void shouldNotPurchasePerkWhenUserHasCommonOrder() throws Exception {
        setGrowingCashbackConfig(true, REARR, promoKeys);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(USER_WITH_ORDER))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("unavailableReason", equalTo(PerkUnavailableReason.NOT_SUITABLE))
        ))));
    }

    @Test
    public void shouldNotPurchasePerkWhenNotLoggedIn() throws Exception {
        setGrowingCashbackConfig(true, REARR, promoKeys);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_MUID))    //Market UID - NotLoggedIn
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("unavailableReason", equalTo(PerkUnavailableReason.NOT_SUITABLE))
        ))));
    }

    @Test
    public void shouldNotPurchasePerkWhenNotEnabled() throws Exception {
        setGrowingCashbackConfig(false, REARR, promoKeys);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_MUID))    //Market UID - NotLoggedIn
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("unavailableReason", equalTo(PerkUnavailableReason.PERK_DISABLED))
        ))));
    }

    @Test
    public void shouldNotPurchasePerkWhenWrongConfigCreatedPerks() throws Exception {
        var promoWithoutCreatedPerk = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixedWithMinOrder(CASHBACK_NOMINAL, MIN_ORDER_TOTAL).setBudget(BigDecimal.TEN)
                        .addCashbackAction(
                                STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED, STATIC_PERK_NAME,
                                DELIVERED_STATIC_PERKS.get(2)
                        )
        );
        promoKeys.set(2, promoWithoutCreatedPerk.getPromoKey());
        reloadPromoCache();
        setGrowingCashbackConfig(true, REARR, promoKeys);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("unavailableReason", equalTo(PerkUnavailableReason.WRONG_CONFIG))
        ))));

        verify(pushMonitor, times(1))
                .addTemporaryCritical(
                        eq(CoreMonitorType.GROWING_CASHBACK),
                        eq("Некорректная конфигурация кампании Растущего кешбэка"),
                        eq(30L), eq(TimeUnit.MINUTES)
                );
    }

    @Test
    public void shouldNotPurchasePerkWhenWrongConfigDeliveredPerks() throws Exception {
        var promoWithoutDeliveredPerk = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixedWithMinOrder(CASHBACK_NOMINAL, MIN_ORDER_TOTAL).setBudget(BigDecimal.TEN)
                        .addCashbackAction(
                                STATIC_PERK_ADDITION_ACTION, ORDER_CREATION, STATIC_PERK_NAME,
                                CREATED_STATIC_PERKS.get(2)
                        )
        );
        promoKeys.set(2, promoWithoutDeliveredPerk.getPromoKey());
        reloadPromoCache();
        setGrowingCashbackConfig(true, REARR, promoKeys);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("unavailableReason", equalTo(PerkUnavailableReason.WRONG_CONFIG))
        ))));

        verify(pushMonitor, times(1))
                .addTemporaryCritical(
                        eq(CoreMonitorType.GROWING_CASHBACK),
                        eq("Некорректная конфигурация кампании Растущего кешбэка"),
                        eq(30L), eq(TimeUnit.MINUTES)
                );
    }

    @Test
    public void shouldNotPurchasePerkWhenNotActivePromo() throws Exception {
        setGrowingCashbackConfig(true, REARR, promoKeys.subList(0, 1));
        promoService.updateStatus(promos.get(0), PromoStatus.INACTIVE);
        reloadPromoCache();

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("unavailableReason", equalTo(PerkUnavailableReason.WRONG_CONFIG))
        ))));

        verify(pushMonitor, times(1))
                .addTemporaryCritical(
                        eq(CoreMonitorType.GROWING_CASHBACK),
                        eq("Некорректная конфигурация кампании Растущего кешбэка"),
                        eq(30L), eq(TimeUnit.MINUTES)
                );
    }

    @Test
    public void shouldNotPurchasePerkWhenPromoWithEmptyBudget() throws Exception {
        var promoWithEmptyBudget = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixedWithMinOrder(BigDecimal.TEN, MIN_ORDER_TOTAL)
                        .setBudget(BigDecimal.ZERO));
        promoKeys.set(2, promoWithEmptyBudget.getPromoKey());
        reloadPromoCache();
        setGrowingCashbackConfig(true, REARR, promoKeys);

        final PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("unavailableReason", equalTo(PerkUnavailableReason.WRONG_CONFIG))
        ))));

        verify(pushMonitor, times(1))
                .addTemporaryCritical(
                        eq(CoreMonitorType.GROWING_CASHBACK),
                        eq("Некорректная конфигурация кампании Растущего кешбэка"),
                        eq(30L), eq(TimeUnit.MINUTES)
                );
    }

    @Test
    public void shouldPurchasePerkWhenNewUsersAllowedFalseButUserAlreadyGotPerk() throws Exception {
        long uid = DEFAULT_UID + 100;
        setGrowingCashbackConfig(true, null, promoKeys);
        //Делаем вызов, чтобы для пользователя заполнился перк
        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                //NO REARR .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(uid))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(maxCashbackTotal)),
                hasProperty("ordersCount", equalTo((long) promos.size())),
                hasProperty("minOrderTotal", equalTo(MIN_ORDER_TOTAL.longValue())),
                hasProperty("isGotFullReward", equalTo(false)),
                hasProperty("nextStepPromoKey", equalTo(promos.get(0).getPromoKey())),
                hasProperty("unavailableReason", nullValue()),
                hasProperty("promoEndDate",
                        equalTo(promos.get(0).getEndDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString()))
        ))));
        configurationService.set(GROWING_CASHBACK_EXPERIMENT_NEW_USERS_ALLOW, false);

        //Делаем вызов после набора аудитории
        perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                //NO REARR .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(uid))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(maxCashbackTotal)),
                hasProperty("ordersCount", equalTo((long) promos.size())),
                hasProperty("minOrderTotal", equalTo(MIN_ORDER_TOTAL.longValue())),
                hasProperty("isGotFullReward", equalTo(false)),
                hasProperty("nextStepPromoKey", equalTo(promos.get(0).getPromoKey())),
                hasProperty("unavailableReason", nullValue()),
                hasProperty("promoEndDate",
                        equalTo(promos.get(0).getEndDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString()))
        ))));
    }

    @Test
    public void shouldNotPurchasePerkWhenNewUsersAllowedFalseAndUserDidNotGotPerk() throws Exception {
        long uid = DEFAULT_UID + 100;
        setGrowingCashbackConfig(true, null, promoKeys);
        configurationService.set(GROWING_CASHBACK_EXPERIMENT_NEW_USERS_ALLOW, false);

        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                .contentType(MediaType.APPLICATION_JSON)
                //NO REARR .header("X-Market-Rearrfactors", REARR)
                .queryParam(LoyaltyTag.UID, Long.toString(uid))
                .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(false)),
                hasProperty("maxCashbackTotal", nullValue()),
                hasProperty("ordersCount", nullValue()),
                hasProperty("minOrderTotal", nullValue()),
                hasProperty("isGotFullReward", nullValue()),
                hasProperty("nextStepPromoKey", nullValue()),
                hasProperty("unavailableReason", equalTo(PerkUnavailableReason.NOT_IN_EXPERIMENT)),
                hasProperty("promoEndDate",
                        equalTo(promos.get(0).getEndDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString()))
        ))));
    }

    @Test
    public void checkPerkType() {
        assertThat(growingCashbackPerkProcessor.getProcessedPerk(), equalTo(GROWING_CASHBACK));
    }

    @Test
    public void shouldPurchaseWithoutRearrOnDeliveryStage() throws Exception {
        var promoWithEmptyBudget = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixedWithMinOrder(BigDecimal.TEN, MIN_ORDER_TOTAL)
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, RuleParameterName.PERK_TYPE,
                                GROWING_CASHBACK)
                        .setBudget(BigDecimal.ZERO));
        reloadPromoCache();
        setGrowingCashbackConfig(true, REARR, promoKeys);

        staticPerkService.providePerkToUser(USER_WITH_ORDER, CREATED_STATIC_PERKS.get(0));

        StatusFeatureResults perks = perkService.getPerks(
                OperationContextDto.builderDto()
                        .setUid(USER_WITH_ORDER)
                        .setRegionId(DEFAULT_REGION)
                        .build(),
                Map.of(
                        ORDER_STAGE, Set.of(ORDER_DELIVERED)
                ),
                true
        ).fetch();

        final PerkStatResponse perkStatResponse = PerkStatResponse.builder()
                .addPerkStats(perks.getPerkOwnerships().values().stream()
                        .map(p -> p.toPerkStat(DEFAULT_REGION))
                        .collect(Collectors.toList()))
                .build();

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(maxCashbackTotal)),
                hasProperty("ordersCount", equalTo((long) promos.size())),
                hasProperty("minOrderTotal", equalTo(MIN_ORDER_TOTAL.longValue())),
                hasProperty("isGotFullReward", equalTo(false)),
                hasProperty("nextStepPromoKey", equalTo(promos.get(1).getPromoKey())),
                hasProperty("unavailableReason", nullValue()),
                hasProperty("promoEndDate",
                        equalTo(promos.get(0).getEndDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString()))
        ))));
    }

    @Test
    public void shouldPurchasePerkIfUserSawGrowingCashbackButCreateOrderLessThantTreshold() throws Exception {
        setGrowingCashbackConfig(true, REARR, promoKeys);

        //Getting perk without orders
        PerkStatResponse perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Market-Rearrfactors", REARR)
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                        .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(maxCashbackTotal)),
                hasProperty("ordersCount", equalTo((long) promos.size())),
                hasProperty("minOrderTotal", equalTo(MIN_ORDER_TOTAL.longValue())),
                hasProperty("isGotFullReward", equalTo(false)),
                hasProperty("nextStepPromoKey", equalTo(promos.get(0).getPromoKey())),
                hasProperty("unavailableReason", nullValue()),
                hasProperty("promoEndDate",
                        equalTo(promos.get(0).getEndDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString()))
        ))));

        //Add order to user
        allUserOrdersService.upsert(new UserOrder(DEFAULT_UID, OrderStatus.DELIVERED.name(),
                Instant.now(), "12345612", Platform.DESKTOP, 200L));

        //Get perk with orders but user has already got experiment static perk
        perkStatResponse = objectMapper.readValue(mockMvc.perform(get("/perk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Market-Rearrfactors", REARR)
                        .queryParam(LoyaltyTag.UID, Long.toString(DEFAULT_UID))
                        .queryParam(LoyaltyTag.NO_CACHE, Boolean.toString(true))
                        .queryParam(LoyaltyTag.REGION_ID, Long.toString(DEFAULT_REGION))
                        .queryParam(LoyaltyTag.PERK_TYPE, GROWING_CASHBACK.getCode()))
                .andReturn().getResponse().getContentAsString(), PerkStatResponse.class);

        assertThat(perkStatResponse, hasProperty("statuses", hasItem(allOf(
                hasProperty("type", equalTo(GROWING_CASHBACK)),
                hasProperty("purchased", equalTo(true)),
                hasProperty("maxCashbackTotal", comparesEqualTo(maxCashbackTotal)),
                hasProperty("ordersCount", equalTo((long) promos.size())),
                hasProperty("minOrderTotal", equalTo(MIN_ORDER_TOTAL.longValue())),
                hasProperty("isGotFullReward", equalTo(false)),
                hasProperty("nextStepPromoKey", equalTo(promos.get(0).getPromoKey())),
                hasProperty("unavailableReason", nullValue()),
                hasProperty("promoEndDate",
                        equalTo(promos.get(0).getEndDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString()))
        ))));
    }

    private void setGrowingCashbackConfig(boolean enable, String rearr, List<String> promoKeys) {
        configurationService.set(GROWING_CASHBACK_ENABLED, enable);
        configurationService.set(GROWING_CASHBACK_REARR, rearr);
        configurationService.set(GROWING_CASHBACK_PROMO_KEYS, Strings.join(promoKeys, ';'));
    }
}
