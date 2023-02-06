package ru.yandex.market.loyalty.admin.multistage;

import java.math.BigDecimal;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.dao.OrderActionExecutionDao;
import ru.yandex.market.loyalty.core.dao.OrderCashbackCalculationNoMultistageDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.dao.ydb.model.StaticPerk;
import ru.yandex.market.loyalty.core.model.action.PromoActionContainer;
import ru.yandex.market.loyalty.core.model.cashback.BillingSchema;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.trigger.event.PromoActionEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.perks.StaticPerkService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.admin.multistage.MultiStageStaticPerkRuleTest.DISALLOWED_STATIC_PERK;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.buildOrder;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.preparePagedReturns;
import static ru.yandex.market.loyalty.api.model.perk.StaticPerkStatus.PROVIDED;
import static ru.yandex.market.loyalty.core.model.action.PromoActionParameterName.STATIC_PERK_NAME;
import static ru.yandex.market.loyalty.core.model.action.PromoActionType.STATIC_PERK_ADDITION_ACTION;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_CREATION;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_DELIVERED;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_PAID;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.GROWING_CASHBACK_ENABLED;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.GROWING_CASHBACK_EXPERIMENT_NEW_USERS_ALLOW;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.GROWING_CASHBACK_PROMO_KEYS;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.GROWING_CASHBACK_REARR;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.RearrUtils.REARR_FACTORS_HEADER;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class MultiStagePromoActionSingleOrderTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    private static final String TEST_PERK_1 = "test_perk_1";
    private static final String TEST_PERK_2 = "test_perk_2";
    private static final String TEST_PERK = "test_perk";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private MultiStageTestUtils multiStageTestUtils;
    @Autowired
    private TriggerEventDao triggerEventDao;
    @Autowired
    private TriggerEventService triggerEventService;
    @Autowired
    private StaticPerkService staticPerkService;
    @Autowired
    private OrderActionExecutionDao orderActionExecutionDao;
    @Autowired
    private OrderCashbackCalculationNoMultistageDao orderCashbackCalculationNoMultistageDao;

    private MultiCartWithBundlesDiscountRequest request;

    @Before
    public void setUp() {
        multiStageTestUtils.setUpCashback();
        configurationService.set(ConfigurationService.PROMO_ACTION_EVENT_EXECUTION_ENABLED, true);
        request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(DEFAULT_ORDER_ID)
                        .withOrderItem(
                                warehouse(MARKET_WAREHOUSE_ID),
                                itemKey(DEFAULT_ITEM_KEY),
                                price(500),
                                quantity(3)
                        )
                        .withDeliveries(courierDelivery(
                                withPrice(BigDecimal.valueOf(350)),
                                builder -> builder.setSelected(true)
                        ))
                        .build()
                )
                .build();
    }

    @Test
    public void shouldPurchaseOnlyOnePerkIfTwoPromosMatchedButPriority() {
        var promo1 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(-1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        var promo2 = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK + "_2"))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(-2)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(1));
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
        assertTrue("User hasn't test perk after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        Set<StaticPerk> perksForUser = staticPerkService.getPerksForUser(DEFAULT_UID);
        assertThat(perksForUser, hasSize(1));

        //Отправляем событие оплаты
        processPaidEvent();

        //Отправляем событие доставки
        processDeliveredEvent();
        assertTrue("User hasn't test perk after order delivery",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));
        perksForUser = staticPerkService.getPerksForUser(DEFAULT_UID);
        assertThat(perksForUser, hasSize(1));
    }

    @Test
    public void shouldExecuteActionAfterSingleOrderPaid() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_PAID)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(0));

        //Отправляем событие оплаты
        processPaidEvent();
        assertTrue("User hasn't test perk after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        //Отправляем событие доставки
        processDeliveredEvent();
        assertTrue("User hasn't test perk after order delivery",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldExecuteActionAfterSingleOrderDelivered() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(0));

        //Отправляем событие оплаты
        processPaidEvent();
        assertFalse("User has test perk before order delivered",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        //Отправляем событие доставки
        processDeliveredEvent();
        assertTrue("User hasn't test perk after order delivery",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));
    }


    @Test
    public void shouldNotEmitGrowingCashback() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE,
                                RuleParameterName.PERK_TYPE,
                                PerkType.GROWING_CASHBACK)
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK_1))
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK_2))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
                        .setCalculateOnDeliveryOnly(true)
        );

        configurationService.set(GROWING_CASHBACK_ENABLED, true);
        configurationService.set(GROWING_CASHBACK_EXPERIMENT_NEW_USERS_ALLOW, true);
        configurationService.set(GROWING_CASHBACK_PROMO_KEYS, promo.getPromoKey());
        configurationService.set(GROWING_CASHBACK_REARR, "rearr");
        reloadPromoCache();

        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(0));

        //Отправляем событие оплаты
        processPaidEvent();
        assertFalse("User has test_1 perk before order paid",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK_1, PROVIDED));
        assertFalse("User has test_2 perk after order paid",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK_2, PROVIDED));

        //Отправляем событие доставки
        processDeliveredEvent();
        assertFalse("User has test_1 perk before order delivered",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK_1, PROVIDED));
        assertFalse("User has test_2 perk after order delivered",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK_2, PROVIDED));
    }

    @Test
    public void shouldEmitGrowingCashback() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE,
                                RuleParameterName.PERK_TYPE,
                                PerkType.GROWING_CASHBACK)
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK_1))
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK_2))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
                        .setCalculateOnDeliveryOnly(true)
        );

        configurationService.set(GROWING_CASHBACK_ENABLED, true);
        configurationService.set(GROWING_CASHBACK_EXPERIMENT_NEW_USERS_ALLOW, true);
        configurationService.set(GROWING_CASHBACK_PROMO_KEYS, promo.getPromoKey());
        configurationService.set(GROWING_CASHBACK_REARR, "rearr");
        setupUserExperiment("rearr");
        reloadPromoCache();

        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(1));
        processAllEvents();

        //Отправляем событие оплаты
        processPaidEvent();
        assertTrue("User has test_1 perk before order paid",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK_1, PROVIDED));
        assertFalse("User has test_2 perk after order paid",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK_2, PROVIDED));

        //Отправляем событие доставки
        processDeliveredEvent();
        assertTrue("User hasn't test_1 perk before order delivered",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK_1, PROVIDED));
        assertTrue("User hasn't test_2 perk after order delivered",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK_2, PROVIDED));
    }

    private void processAllEvents() {
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
    }

    @Test
    public void shouldExecuteActionOnCreationAndThenCancelOnPaid() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackRule(
                                RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                                RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM,
                                PaymentSystem.UNKNOWN
                        )
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        multiStageTestUtils.processTriggerEventsUntilAllFinished();
        assertTrue("User hasn't test perk after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        //Отправляем события
        processPaidEvent();
        assertFalse("User has test perk after action cancellation",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        processDeliveredEvent();
        assertFalse("User has test perk after action cancellation",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldExecuteActionOnCreationAndThenCancelOnDelivered() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackRule(
                                RuleType.STATIC_PERKS_ON_DELIVERY_CUTTING_RULE,
                                RuleParameterName.DISALLOWED_STATIC_PERK,
                                DISALLOWED_STATIC_PERK
                        )
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        multiStageTestUtils.processTriggerEventsUntilAllFinished();
        assertTrue("User hasn't test perk after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        staticPerkService.providePerkToUser(DEFAULT_UID, DISALLOWED_STATIC_PERK);
        //Отправляем события
        processPaidEvent();
        processDeliveredEvent();

        assertFalse("User has test perk after action cancellation",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldExecuteActionOnPaidAndThenCancelOnDelivered() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackRule(
                                RuleType.STATIC_PERKS_ON_DELIVERY_CUTTING_RULE,
                                RuleParameterName.DISALLOWED_STATIC_PERK,
                                DISALLOWED_STATIC_PERK
                        )
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_PAID)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(0));

        //Отправляем события
        processPaidEvent();
        assertTrue("User hasn't test perk after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        staticPerkService.providePerkToUser(DEFAULT_UID, DISALLOWED_STATIC_PERK);
        processDeliveredEvent();

        assertFalse("User has test perk after action cancellation",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldNotExecuteActionWhenCancelOnSameStage() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackRule(
                                RuleType.STATIC_PERKS_ON_DELIVERY_CUTTING_RULE,
                                RuleParameterName.DISALLOWED_STATIC_PERK,
                                DISALLOWED_STATIC_PERK
                        )
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(0));

        //Отправляем события
        processPaidEvent();

        staticPerkService.providePerkToUser(DEFAULT_UID, DISALLOWED_STATIC_PERK);
        processDeliveredEvent();

        var executions = orderActionExecutionDao.findAll();
        assertThat(executions, allOf(
                iterableWithSize(1),
                hasItem(allOf(
                        hasProperty("orderId", equalTo(Long.valueOf(DEFAULT_ORDER_ID))),
                        hasProperty("promoId", equalTo(promo.getPromoId().getId())),
                        hasProperty("result", equalTo(ResolvingState.CANCELLED))
                ))
        ));

        var uniqueKey = PromoActionEvent.createUniqueKey(Long.valueOf(DEFAULT_ORDER_ID), null,
                promo.getPromoId().getId(), executions.get(0).getActionId(), false);
        assertThat(triggerEventService.findByUniqueKey(TriggerEventTypes.PROMO_ACTION_EVENT, uniqueKey), nullValue());

        assertFalse("User has test perk after action cancellation",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldExecuteAndThenCancelOnOrderCancellation() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        multiStageTestUtils.processTriggerEventsUntilAllFinished();
        assertTrue("User hasn't test perk after action execution",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        //Отправляем события
        processCancelEvent();

        assertFalse("User has test perk after action cancellation",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldExecuteActionsForOnDeliveryOnlyPromo() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .setPromoBucketName("bucket1")
                        .setCalculateOnDeliveryOnly(true)
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(0));

        //Отправляем событие оплаты
        processPaidEvent();
        assertFalse("User has test perk before order delivered",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        //Отправляем событие доставки
        processDeliveredEvent();
        assertTrue("User hasn't test perk after order delivery",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldRemoveCashbackCalculationsNoMultistageOnDelivery() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(1))
                        .setCalculateOnDeliveryOnly(true)
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
        );
        reloadPromoCache();

        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(0));

        //Отправляем событие оплаты
        processPaidEvent();
        assertThat(orderCashbackCalculationNoMultistageDao.findAll(), is(not(empty())));

        //Отправляем событие доставки
        processDeliveredEvent();
        assertThat(orderCashbackCalculationNoMultistageDao.findAll(), is(empty()));

    }

    @Test
    public void shouldCancelActionsForOnDeliveryOnlyPromo() {
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .setPromoBucketName("bucket1")
                        .setCalculateOnDeliveryOnly(true)
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        var discountResponse = multiStageTestUtils.spendRequest(request);

        multiStageTestUtils.processTriggerEventsUntilAllFinished();
        assertTrue("User hasn't test perk after order creation",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        //Отправляем событие отмены
        processCancelEvent();
        assertFalse("User has test perk after order cancellation",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldNotUndoActionAfterRefundIfPromoStillMatches() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(1));
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
        assertTrue("User hasn't test perk after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        //Отправляем событие оплаты
        processPaidEvent();

        //Отправляем событие доставки
        processDeliveredEvent();
        assertTrue("User hasn't test perk after order delivery",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        processRefundEvent(1);
        assertTrue("User hasn't test perk after partial refund",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

    }

    @Test
    public void shouldUndoActionAfterRefundIfPromoNotMatches() {
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                        .addCashbackRule(
                                RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.valueOf(1499))
                        .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                                .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(1));
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
        assertTrue("User hasn't test perk after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        //Отправляем событие оплаты
        processPaidEvent();

        //Отправляем событие доставки
        processDeliveredEvent();
        assertTrue("User hasn't test perk after order delivery",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        processRefundEvent(1);
        assertFalse("User has test perk after partial refund",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

    }

    private void processCancelEvent() {
        processEvent(
                buildOrder(
                        OrderStatus.CANCELLED, Long.valueOf(DEFAULT_ORDER_ID), null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
    }

    private void processPaidEvent() {
        processEvent(
                buildOrder(
                        OrderStatus.PROCESSING, Long.valueOf(DEFAULT_ORDER_ID), null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
    }

    private void processDeliveredEvent() {
        processEvent(
                buildOrder(OrderStatus.DELIVERED, Long.valueOf(DEFAULT_ORDER_ID), null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
    }

    private void processRefundEvent(int count) {
        var orderDelivered = buildOrder(OrderStatus.DELIVERED, Long.valueOf(DEFAULT_ORDER_ID), null,
                DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build();

        when(checkouterClient.returns().getOrderReturns(any(), any()))
                .thenReturn(preparePagedReturns(orderDelivered, count));
        when(checkouterClient.getOrderItems(any(), any()))
                .thenReturn(new OrderItems(orderDelivered.getItems()));

        processEvent(orderDelivered, HistoryEventType.REFUND);
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
    }

    private void setupUserExperiment(String rearr) {
        HttpServletRequest requestMock = Mockito.mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestMock));
        Mockito.when(((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest().getHeader(eq(REARR_FACTORS_HEADER))).thenReturn(rearr);
    }

    @After
    public void resetContext() {
        HttpServletRequest requestMock = Mockito.mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestMock));
    }
}
