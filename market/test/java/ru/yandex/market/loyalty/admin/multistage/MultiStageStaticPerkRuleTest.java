package ru.yandex.market.loyalty.admin.multistage;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.TriggerEventTmsProcessor;
import ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils;
import ru.yandex.market.loyalty.core.dao.OrderPaidDataDao;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerClient;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.cashback.BillingSchema;
import ru.yandex.market.loyalty.core.model.multistage.ResolvingState;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.perks.StaticPerkService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.math.BigDecimal;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.buildOrder;
import static ru.yandex.market.loyalty.api.model.PromoType.CASHBACK;
import static ru.yandex.market.loyalty.core.logbroker.EventType.CASHBACK_EMIT;
import static ru.yandex.market.loyalty.core.logbroker.EventType.CASHBACK_ERROR;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.CANCELLED;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.IN_QUEUE;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.PENDING;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.*;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class MultiStageStaticPerkRuleTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    public static final String REQUIRED_STATIC_PERK = "REQUIRED_STATIC_PERK";
    public static final String DISALLOWED_STATIC_PERK = "DISALLOWED_STATIC_PERK";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TskvLogBrokerClient logBrokerClient;
    @Autowired
    private MultiStageTestUtils multiStageTestUtils;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    private OrderPaidDataDao orderPaidDataDao;
    @Autowired
    private PromoService promoService;
    @Autowired
    private StaticPerkService staticPerkService;

    @Before
    public void setUp() {
        multiStageTestUtils.setUpCashback();
    }

    @Test
    public void testSingleOrderDirectFlowSuccess() {
        var orderId = 1L;
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackRule(RuleContainer.builder(RuleType.STATIC_PERKS_ON_CREATION_CUTTING_RULE)
                                .withSingleParam(RuleParameterName.REQUIRED_STATIC_PERK, REQUIRED_STATIC_PERK)
                                .withSingleParam(RuleParameterName.DISALLOWED_STATIC_PERK, DISALLOWED_STATIC_PERK))
                        .addCashbackRule(RuleContainer.builder(RuleType.STATIC_PERKS_ON_DELIVERY_CUTTING_RULE)
                                .withSingleParam(RuleParameterName.REQUIRED_STATIC_PERK, REQUIRED_STATIC_PERK)
                                .withSingleParam(RuleParameterName.DISALLOWED_STATIC_PERK, DISALLOWED_STATIC_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        var request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(Long.toString(orderId))
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

        staticPerkService.providePerkToUser(request.getOperationContext().getUid(), REQUIRED_STATIC_PERK);
        staticPerkService.revokePerkFromUser(request.getOperationContext().getUid(), DISALLOWED_STATIC_PERK);

        var discountResponse = multiStageTestUtils.spendRequest(request);

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        multiStageTestUtils.checkCalculations(2, ResolvingState.INTERMEDIATE, true);

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, orderId, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(orderPaidDataDao.findAll(), hasSize(1));
        multiStageTestUtils.checkYandexWalletTransactions(1, PENDING, 50L, true);
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(50)))
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_ERROR)),
                hasProperty("errorType", is("NO_SUITABLE_PROMO")),
                hasProperty("discount", is(nullValue())),
                hasProperty("uid", is(DEFAULT_UID)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(nullValue()))
        )));

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, orderId, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        multiStageTestUtils.checkYandexWalletTransactions(1, IN_QUEUE, 50L, true);
        multiStageTestUtils.checkCalculations(2, ResolvingState.FINAL, true);
    }

    @Test
    public void testSingleOrderDirectFlowRejectedOnInitialStage() {
        var orderId = 1L;
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackRule(RuleContainer.builder(RuleType.STATIC_PERKS_ON_CREATION_CUTTING_RULE)
                                .withSingleParam(RuleParameterName.REQUIRED_STATIC_PERK, REQUIRED_STATIC_PERK)
                                .withSingleParam(RuleParameterName.DISALLOWED_STATIC_PERK, DISALLOWED_STATIC_PERK)
                        )
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        var request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(Long.toString(orderId))
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

        staticPerkService.providePerkToUser(request.getOperationContext().getUid(), DISALLOWED_STATIC_PERK);

        var discountResponse = multiStageTestUtils.spendRequest(request);

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        assertThat(discountResponse.getCashback().getEmit().getAmount(),
                comparesEqualTo(BigDecimal.ZERO));
        multiStageTestUtils.checkCalculations(0, null, null);
        multiStageTestUtils.checkYandexWalletTransactions(0, null, null, null);
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000))
        );
    }

    @Test
    public void testSingleOrderDirectFlowRejectedOnTerminalStage() {
        var orderId = 1L;
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackRule(RuleContainer.builder(RuleType.STATIC_PERKS_ON_DELIVERY_CUTTING_RULE)
                                .withSingleParam(RuleParameterName.REQUIRED_STATIC_PERK, REQUIRED_STATIC_PERK)
                                .withSingleParam(RuleParameterName.DISALLOWED_STATIC_PERK, DISALLOWED_STATIC_PERK)
                        )
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        var request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(Long.toString(orderId))
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

        staticPerkService.providePerkToUser(request.getOperationContext().getUid(), DISALLOWED_STATIC_PERK);
        staticPerkService.revokePerkFromUser(request.getOperationContext().getUid(), REQUIRED_STATIC_PERK);

        var discountResponse = multiStageTestUtils.spendRequest(request);

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        multiStageTestUtils.checkCalculations(2, ResolvingState.INTERMEDIATE, true);

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, orderId, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(orderPaidDataDao.findAll(), hasSize(1));
        multiStageTestUtils.checkYandexWalletTransactions(1, PENDING, 50L, true);
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(50)))
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_ERROR)),
                hasProperty("errorType", is("NO_SUITABLE_PROMO")),
                hasProperty("discount", is(nullValue())),
                hasProperty("uid", is(DEFAULT_UID)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(nullValue()))
        )));


        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, orderId, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        multiStageTestUtils.checkYandexWalletTransactions(1, CANCELLED, 50L, true);
        multiStageTestUtils.checkCalculations(2, ResolvingState.CANCELLED, false);
    }

    @Test
    public void testSingleOrderDirectFlowSuccessWithNoRejectOnTerminationByCreationRule() {
        var orderId = 1L;
        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(10), CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE,
                                RuleParameterName.MIN_ORDER_TOTAL,
                                BigDecimal.ONE)
                        .addCashbackRule(RuleContainer.builder(RuleType.STATIC_PERKS_ON_CREATION_CUTTING_RULE)
                                .withSingleParam(RuleParameterName.DISALLOWED_STATIC_PERK, DISALLOWED_STATIC_PERK))
                        .addCashbackRule(RuleContainer.builder(RuleType.STATIC_PERKS_ON_DELIVERY_CUTTING_RULE)
                                .withSingleParam(RuleParameterName.REQUIRED_STATIC_PERK, REQUIRED_STATIC_PERK))
                        .setEmissionBudget(BigDecimal.valueOf(10_000_000))
                        .setPriority(1)
                        .setBillingSchema(BillingSchema.SOLID)
        );
        reloadPromoCache();

        var request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderId(Long.toString(orderId))
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

        var discountResponse = multiStageTestUtils.spendRequest(request);

        // мультистейдж промки в нефинальном статусе (INTERMEDIATE) не попадут в события
        verify(logBrokerClient, never()).pushEvent(argThat(allOf(
                hasProperty("eventType", is(CASHBACK_EMIT))
        )));

        multiStageTestUtils.checkCalculations(2, ResolvingState.INTERMEDIATE, true);

        //Отправляем событие оплаты
        processEvent(
                buildOrder(OrderStatus.PROCESSING, orderId, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(orderPaidDataDao.findAll(), hasSize(1));
        multiStageTestUtils.checkYandexWalletTransactions(1, PENDING, 50L, true);
        assertThat(promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(BigDecimal.valueOf(10_000_000).subtract(BigDecimal.valueOf(50)))
        );

        verify(logBrokerClient).pushEvent(argThat(allOf(
                hasProperty("platform", is(CoreMarketPlatform.BLUE)),
                hasProperty("httpMethod", is("spend")),
                hasProperty("eventType", is(CASHBACK_ERROR)),
                hasProperty("errorType", is("NO_SUITABLE_PROMO")),
                hasProperty("discount", is(nullValue())),
                hasProperty("uid", is(DEFAULT_UID)),
                hasProperty("promoType", is(CASHBACK)),
                hasProperty("promoKey", is(nullValue()))
        )));

        staticPerkService.providePerkToUser(request.getOperationContext().getUid(), REQUIRED_STATIC_PERK);
        staticPerkService.providePerkToUser(request.getOperationContext().getUid(), DISALLOWED_STATIC_PERK);

        //Отправляем событие доставки
        processEvent(
                buildOrder(OrderStatus.DELIVERED, orderId, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        multiStageTestUtils.checkYandexWalletTransactions(1, IN_QUEUE, 50L, true);
        multiStageTestUtils.checkCalculations(2, ResolvingState.FINAL, true);
    }
}
