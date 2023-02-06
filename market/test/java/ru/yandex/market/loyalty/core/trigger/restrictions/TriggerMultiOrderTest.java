package ru.yandex.market.loyalty.core.trigger.restrictions;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.core.dao.trigger.InsertResult;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.CoinSearchRequest;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType;
import ru.yandex.market.loyalty.core.model.trigger.event.CoreOrderStatus;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderEventInfo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.RulePayloads;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.lightweight.ExceptionUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent.builderMultiOrderEventFromOrderInfo;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.createEvent;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrder;
import static ru.yandex.market.loyalty.core.utils.EventFactory.orderStatusUpdated;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withMultiOrderId;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.actionOnceRestriction;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.groupType;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class TriggerMultiOrderTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final String MULTI_ORDER_ID = "1";

    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private TriggerEventDao triggerEventDao;
    @Autowired
    private CoinService coinService;
    @Autowired
    private DiscountUtils discountUtils;

    @Test
    public void shouldCreateCoinWhenStatusIsDeliverOnlyForOneOrderInOneProcess() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                actionOnceRestriction(),
                orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING)
        );

        InsertResult<OrderStatusUpdatedEvent> event1 = triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withMultiOrderId(MULTI_ORDER_ID)
        ));
        InsertResult<OrderStatusUpdatedEvent> event2 = triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withMultiOrderId(MULTI_ORDER_ID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        List<TriggerEvent> events = triggerEventDao.getAll();
        TriggerEvent event1Stored = events.stream()
                .filter(event -> event.getId().equals(event1.getData().getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No event stored"));
        TriggerEvent event2Stored = events.stream()
                .filter(event -> event.getId().equals(event2.getData().getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No event stored"));

        assertTrue(event1Stored.getProcessedResult() == TriggerEventProcessedResult.NO_TRIGGERS ^
                event2Stored.getProcessedResult() == TriggerEventProcessedResult.NO_TRIGGERS);
        assertTrue(event1Stored.getProcessedResult() == TriggerEventProcessedResult.SUCCESS ^
                event2Stored.getProcessedResult() == TriggerEventProcessedResult.SUCCESS);

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    public void shouldCreateCoinWhenStatusIsDeliverOnlyForOneOrderInSequentialProcesses() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                actionOnceRestriction(),
                orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING)
        );

        InsertResult<OrderStatusUpdatedEvent> event1 = triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withMultiOrderId(MULTI_ORDER_ID)
        ));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        InsertResult<OrderStatusUpdatedEvent> event2 = triggerEventQueueService.addEventToQueue(orderStatusUpdated(
                withMultiOrderId(MULTI_ORDER_ID)
        ));

        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        List<TriggerEvent> events = triggerEventDao.getAll();
        TriggerEvent event1Stored = events.stream()
                .filter(event -> event.getId().equals(event1.getData().getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No event stored"));
        TriggerEvent event2Stored = events.stream()
                .filter(event -> event.getId().equals(event2.getData().getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No event stored"));

        assertEquals(TriggerEventProcessedResult.SUCCESS, event1Stored.getProcessedResult());
        assertEquals(TriggerEventProcessedResult.NO_TRIGGERS, event2Stored.getProcessedResult());

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );
    }

    @Test
    @Repeat(5)
    public void shouldRunAllMandatoryTriggersForEachOrderWithinMultiOrder() throws InterruptedException {
        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo1);

        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo2);

        Promo promo3 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo3);

        RulePayloads<?> rulesPayload = discountUtils.getRulesPayload();
        testConcurrency(LongStream.rangeClosed(0, CPU_COUNT - 1)
                .mapToObj(cpuNum ->
                        (ExceptionUtils.RunnableWithException<RuntimeException>) (() -> {
                            final Order order = defaultOrder(OrderStatus.DELIVERED)
                                    .setOrderId(cpuNum)
                                    .setProperty(OrderPropertyType.MULTI_ORDER_ID, MULTI_ORDER_ID)
                                    .setProperty(OrderPropertyType.MARKET_REQUEST_ID, "test")
                                    .build();
                            triggerEventQueueService.insertAndProcessEvent(
                                    createEvent(
                                            OrderEventInfo.createOrderEventInfoFromOrder(order)
                                                    .setOrderStatus(CoreOrderStatus.PROCESSING)
                                                    .build(),
                                            null
                                    ), rulesPayload, BudgetMode.SYNC
                            );
                        })
                )
                .collect(Collectors.toList())
        );
        assertEquals(CPU_COUNT * 3, coinService.search.getActiveInactiveCoinsCount(
                CoinSearchRequest.forUserId(DEFAULT_UID)
                        .platform(CoreMarketPlatform.BLUE)
        ));
    }

    @Test
    @Repeat(5)
    public void shouldRunAllMandatoryActionOnceTriggersForSingleOrderWithinMultiOrder() throws InterruptedException {
        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo1, actionOnceRestriction());

        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo2, actionOnceRestriction());

        Promo promo3 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo3, actionOnceRestriction());

        RulePayloads<?> rulesPayload = discountUtils.getRulesPayload();
        testConcurrency(LongStream.rangeClosed(0, CPU_COUNT - 1)
                .mapToObj(cpuNum ->
                        (ExceptionUtils.RunnableWithException<RuntimeException>) (() -> {
                            final Order order = defaultOrder(OrderStatus.DELIVERED)
                                    .setOrderId(cpuNum)
                                    .setProperty(OrderPropertyType.MULTI_ORDER_ID, MULTI_ORDER_ID)
                                    .setProperty(OrderPropertyType.MARKET_REQUEST_ID, "test")
                                    .build();
                            triggerEventQueueService.insertAndProcessEvent(
                                    createEvent(
                                            OrderEventInfo.createOrderEventInfoFromOrder(order)
                                                    .setOrderStatus(CoreOrderStatus.PROCESSING)
                                                    .build(),
                                            null
                                    ), rulesPayload, BudgetMode.SYNC);
                        })
                )
                .collect(Collectors.toList())
        );
        assertEquals(3, coinService.search.getActiveInactiveCoinsCount(
                CoinSearchRequest.forUserId(DEFAULT_UID)
                        .platform(CoreMarketPlatform.BLUE)
        ));
    }

    @Test
    @Repeat(5)
    @Ignore
    public void shouldRunSingleRandomTriggerForSingleOrderWithinMultiOrder() throws InterruptedException {
        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo1,
                groupType(TriggerGroupType.RANDOM_TRIGGERS)
        );

        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo2,
                groupType(TriggerGroupType.RANDOM_TRIGGERS)
        );

        Promo promo3 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo3,
                groupType(TriggerGroupType.RANDOM_TRIGGERS)
        );

        AtomicLong orderId = new AtomicLong(0);

        RulePayloads<?> rulesPayload = discountUtils.getRulesPayload();
        testConcurrency(() -> () -> {
                    final Order order = defaultOrder(OrderStatus.DELIVERED)
                            .setOrderId(orderId.getAndAdd(1))
                            .setProperty(OrderPropertyType.MULTI_ORDER_ID, MULTI_ORDER_ID)
                            .setProperty(OrderPropertyType.MARKET_REQUEST_ID, "test")
                            .build();
                    triggerEventQueueService.insertAndProcessEvent(
                            builderMultiOrderEventFromOrderInfo(
                                    OrderEventInfo.createOrderEventInfoFromOrder(order).build(),
                                    Runtime.getRuntime().availableProcessors(),
                                    null,
                                    "request_id").build(), rulesPayload, BudgetMode.SYNC
                    );
                }
        );
        assertEquals(1, coinService.search.getActiveInactiveCoinsCount(
                CoinSearchRequest.forUserId(DEFAULT_UID)
                        .platform(CoreMarketPlatform.BLUE)
        ));
    }
}
