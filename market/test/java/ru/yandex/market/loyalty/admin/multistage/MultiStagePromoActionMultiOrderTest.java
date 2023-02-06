package ru.yandex.market.loyalty.admin.multistage;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.core.dao.OrderActionExecutionDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.admin.multistage.MultiStageStaticPerkRuleTest.DISALLOWED_STATIC_PERK;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.buildOrder;
import static ru.yandex.market.loyalty.api.model.perk.StaticPerkStatus.PROVIDED;
import static ru.yandex.market.loyalty.core.model.action.PromoActionParameterName.STATIC_PERK_NAME;
import static ru.yandex.market.loyalty.core.model.action.PromoActionType.STATIC_PERK_ADDITION_ACTION;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_CREATION;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_DELIVERED;
import static ru.yandex.market.loyalty.core.model.order.OrderStage.ORDER_PAID;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_MULTI_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class MultiStagePromoActionMultiOrderTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    private static final String TEST_PERK = "test_perk";
    private static final String SECOND_TEST_PERK = "second_test_perk";

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

    private MultiCartWithBundlesDiscountRequest request;

    @Before
    public void setUp() {
        multiStageTestUtils.setUpCashback();
        configurationService.set(ConfigurationService.PROMO_ACTION_EVENT_EXECUTION_ENABLED, true);
        request = DiscountRequestWithBundlesBuilder
                .builder(
                        orderRequestWithBundlesBuilder()
                                .withOrderId(DEFAULT_ORDER_ID)
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(500),
                                        quantity(3))
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)))
                                .build(),
                        orderRequestWithBundlesBuilder()
                                .withOrderId(ANOTHER_ORDER_ID)
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(ANOTHER_ITEM_KEY),
                                        price(500),
                                        quantity(3))
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)))
                                .build()
                )
                .withMultiOrderId(DEFAULT_MULTI_ORDER_ID)
                .build();
    }

    @Test
    public void shouldExecuteBothActionsAfterMultiOrderCreation() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                        .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setBillingSchema(BillingSchema.SOLID));
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                        .withSingleParam(STATIC_PERK_NAME, SECOND_TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setPromoBucketName("non-default")
                .setBillingSchema(BillingSchema.SOLID));
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(2));
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
        assertTrue("User hasn't test perks after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));

        //Отправляем событие оплаты
        processPaidEvent();

        //Отправляем событие доставки
        processDeliveredEvent();
        assertTrue("User hasn't test perk after order delivery",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldExecuteOneActionOnCreationAndAnotherAfterPaid() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                        .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setBillingSchema(BillingSchema.SOLID));
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_PAID)
                        .withSingleParam(STATIC_PERK_NAME, SECOND_TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setPromoBucketName("non-default")
                .setBillingSchema(BillingSchema.SOLID));
        reloadPromoCache();

        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(1));
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
        assertTrue("User hasn't test perk after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && !staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));

        //Отправляем событие оплаты
        processPaidEvent();
        assertTrue("User hasn't second test perk after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));

        //Отправляем событие доставки
        processDeliveredEvent();
        assertTrue("User hasn't test perks after order delivery",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldExecuteOneActionOnCreationAndAnotherAfterDelivered() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                        .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setBillingSchema(BillingSchema.SOLID));
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_DELIVERED)
                        .withSingleParam(STATIC_PERK_NAME, SECOND_TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setPromoBucketName("non-default")
                .setBillingSchema(BillingSchema.SOLID));
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(1));
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
        assertTrue("User hasn't test perk after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && !staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));

        //Отправляем событие оплаты
        processPaidEvent();
        assertTrue("User has test perk before order delivered",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && !staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));

        //Отправляем событие доставки
        processDeliveredEvent();
        assertTrue("User hasn't test perk after order delivery",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldExecuteActionsOnCreationAndPaidAndThenCancelOneOnDelivered() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackRule(
                        RuleType.STATIC_PERKS_ON_DELIVERY_CUTTING_RULE,
                        RuleParameterName.DISALLOWED_STATIC_PERK,
                        DISALLOWED_STATIC_PERK)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_CREATION)
                        .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setBillingSchema(BillingSchema.SOLID));
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_PAID)
                        .withSingleParam(STATIC_PERK_NAME, SECOND_TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setPromoBucketName("non-default")
                .setBillingSchema(BillingSchema.SOLID));
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);


        assertThat(triggerEventDao.getAll(), hasSize(1));
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
        assertTrue("User hasn't test perks after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && !staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));

        staticPerkService.providePerkToUser(DEFAULT_UID, DISALLOWED_STATIC_PERK);
        //Отправляем события
        processPaidEvent();
        assertTrue("User hasn't test perk before action cancellation",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));

        processDeliveredEvent();
        assertTrue("User has test perk after action cancellation",
                !staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldExecuteActionsOnPaidAndThenCancelOnOrderCancelled() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackRule(
                        RuleType.STATIC_PERKS_ON_DELIVERY_CUTTING_RULE,
                        RuleParameterName.DISALLOWED_STATIC_PERK,
                        DISALLOWED_STATIC_PERK)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_PAID)
                        .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setBillingSchema(BillingSchema.SOLID));
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_PAID)
                        .withSingleParam(STATIC_PERK_NAME, SECOND_TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setPromoBucketName("non-default")
                .setBillingSchema(BillingSchema.SOLID));
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);


        assertThat(triggerEventDao.getAll(), hasSize(0));

        //Отправляем события
        processPaidEvent();
        assertTrue("User hasn't test perks after action execution",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));
        processCancelEvent();
        assertTrue("User has test perks after order cancellation",
                !staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && !staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldExecuteActionOnPaidAndThenPartiallyCancelOnDelivered() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_PAID)
                        .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setBillingSchema(BillingSchema.SOLID));
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.valueOf(2900))
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_PAID)
                        .withSingleParam(STATIC_PERK_NAME, SECOND_TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setPromoBucketName("non-default")
                .setBillingSchema(BillingSchema.SOLID));
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(0));

        //Отправляем события
        processPaidEvent();
        assertTrue("User hasn't test perk after action exec",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));

        processPartiallyDeliveredEvent();

        assertTrue("User has test perk after action cancellation",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED)
                        && !staticPerkService.userHasPerkWithStatus(DEFAULT_UID, SECOND_TEST_PERK, PROVIDED));
    }

    @Test
    public void shouldNotExecuteActionsWhenCancelOnSameStage() {
        var promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_PAID)
                        .withSingleParam(STATIC_PERK_NAME, TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setBillingSchema(BillingSchema.SOLID));
        var secondPromo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.valueOf(10))
                .addCashbackRule(
                        RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                        RuleParameterName.MIN_ORDER_TOTAL,
                        BigDecimal.ONE)
                .addCashbackAction(PromoActionContainer.builder(STATIC_PERK_ADDITION_ACTION, ORDER_PAID)
                        .withSingleParam(STATIC_PERK_NAME, SECOND_TEST_PERK))
                .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                .setPriority(1)
                .setPromoBucketName("non-default")
                .setBillingSchema(BillingSchema.SOLID));
        reloadPromoCache();
        var discountResponse = multiStageTestUtils.spendRequest(request);

        assertThat(triggerEventDao.getAll(), hasSize(0));
        //Отправляем события
        processCancelEvent();

        var executions = orderActionExecutionDao.findAll();
        assertThat(executions, allOf(
                iterableWithSize(2),
                containsInAnyOrder(
                        allOf(
                                hasProperty("multiOrderId", equalTo(DEFAULT_MULTI_ORDER_ID)),
                                hasProperty("promoId", equalTo(promo.getPromoId().getId())),
                                hasProperty("result", equalTo(ResolvingState.CANCELLED))
                        ),
                        allOf(
                                hasProperty("multiOrderId", equalTo(DEFAULT_MULTI_ORDER_ID)),
                                hasProperty("promoId", equalTo(secondPromo.getPromoId().getId())),
                                hasProperty("result", equalTo(ResolvingState.CANCELLED))
                        )
                )
        ));

        var uniqueKey = PromoActionEvent.createUniqueKey(null, DEFAULT_MULTI_ORDER_ID,
                executions.get(0).getPromoId(), executions.get(0).getActionId(), false);
        var secondUniqueKey = PromoActionEvent.createUniqueKey(null, DEFAULT_MULTI_ORDER_ID,
                executions.get(1).getPromoId(), executions.get(1).getActionId(), false);
        assertThat(triggerEventService.findByUniqueKey(TriggerEventTypes.PROMO_ACTION_EVENT, uniqueKey), nullValue());
        assertThat(triggerEventService.findByUniqueKey(TriggerEventTypes.PROMO_ACTION_EVENT, secondUniqueKey),
                nullValue());

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

        //Отправляем событие оплаты
        processPaidEvent();
        assertTrue("User hasn't test perk after order paid",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));

        //Отправляем событие отмены
        processCancelEvent();
        assertFalse("User has test perk after order cancellation",
                staticPerkService.userHasPerkWithStatus(DEFAULT_UID, TEST_PERK, PROVIDED));
    }

    private void processCancelEvent() {
        processEvent(
                buildOrder(
                        OrderStatus.CANCELLED, Long.valueOf(DEFAULT_ORDER_ID), DEFAULT_MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(
                        OrderStatus.CANCELLED, Long.valueOf(ANOTHER_ORDER_ID), DEFAULT_MULTI_ORDER_ID,
                        ANOTHER_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
    }

    private void processPendEvent() {
        processEvent(
                buildOrder(
                        OrderStatus.PENDING, Long.valueOf(DEFAULT_ORDER_ID), DEFAULT_MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(
                        OrderStatus.PENDING, Long.valueOf(ANOTHER_ORDER_ID), DEFAULT_MULTI_ORDER_ID,
                        ANOTHER_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
    }

    private void processPaidEvent() {
        processEvent(
                buildOrder(
                        OrderStatus.PROCESSING, Long.valueOf(DEFAULT_ORDER_ID), DEFAULT_MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(
                        OrderStatus.PROCESSING, Long.valueOf(ANOTHER_ORDER_ID), DEFAULT_MULTI_ORDER_ID,
                        ANOTHER_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
    }

    private void processDeliveredEvent() {
        processEvent(
                buildOrder(
                        OrderStatus.DELIVERED, Long.valueOf(DEFAULT_ORDER_ID), DEFAULT_MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(
                        OrderStatus.DELIVERED, Long.valueOf(ANOTHER_ORDER_ID), DEFAULT_MULTI_ORDER_ID,
                        ANOTHER_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
    }

    private void processPartiallyDeliveredEvent() {
        processEvent(
                buildOrder(
                        OrderStatus.DELIVERED, Long.valueOf(DEFAULT_ORDER_ID), DEFAULT_MULTI_ORDER_ID,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(
                        OrderStatus.CANCELLED, Long.valueOf(ANOTHER_ORDER_ID), DEFAULT_MULTI_ORDER_ID,
                        ANOTHER_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 2).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        multiStageTestUtils.processTriggerEventsUntilAllFinished();
    }
}
