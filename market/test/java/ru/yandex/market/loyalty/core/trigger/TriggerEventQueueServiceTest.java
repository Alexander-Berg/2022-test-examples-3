package ru.yandex.market.loyalty.core.trigger;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.core.dao.trigger.InsertResult;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResult;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResultDao;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResultStatus;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.Trigger;
import ru.yandex.market.loyalty.core.model.trigger.event.BaseTriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderPaidEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderTerminationEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.exception.ConflictException;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.trigger.actions.BrokenActionFactory;
import ru.yandex.market.loyalty.core.trigger.actions.ProcessResultUtils;
import ru.yandex.market.loyalty.core.utils.BrokenLoginEvent;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.lightweight.ExceptionUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.model.coin.EmissionRestriction.MANY_COINS;
import static ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult.ERROR;
import static ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult.IN_QUEUE;
import static ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult.SUCCESS;
import static ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult.WAITING;
import static ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes.ORDER_TERMINATION;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrderItem;
import static ru.yandex.market.loyalty.core.utils.EventFactory.DEFAULT_MULTI_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.EventFactory.DEFAULT_ORDER_STATUS_PREDICATE;
import static ru.yandex.market.loyalty.core.utils.EventFactory.createBrokenLoginEvent;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withMultiOrderId;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_EMISSION_BUDGET_IN_COINS;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class TriggerEventQueueServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ProcessResultUtils processResultUtils;
    @Autowired
    private PromoService promoService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TriggerEventDao triggerEventDao;
    @Autowired
    private TriggerActionResultDao triggerActionResultDao;
    @Autowired
    private DiscountUtils discountUtils;
    @Autowired
    private TriggerEventService triggerEventService;

    @After
    public void cleanUp() {
        BrokenActionFactory.cleanUp();
    }

    @Repeat(5)
    @Test
    public void shouldHandleParallelInsertEventWithProcess() throws Exception {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));

        OrderStatusUpdatedEvent event = EventFactory.orderStatusUpdated();
        final List<TriggerActionResult> result = new ArrayList<>();
        testConcurrency(() -> () -> {
            try {
                result.addAll(triggerEventQueueService.insertAndProcessEvent(event,
                        discountUtils.getRulesPayload(), BudgetMode.SYNC
                ));
            } catch (ConflictException ignore) {
            }
        });

        assertThat(processResultUtils.request(result, Coin.class), hasSize(1));
        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(DEFAULT_EMISSION_BUDGET_IN_COINS.subtract(BigDecimal.ONE))
        );

        assertTrue(jdbcTemplate.queryForList(
                "SELECT retry FROM discount WHERE uid = ?",
                Boolean.class,
                DEFAULT_UID
        ).stream().noneMatch(t -> t));
    }

    @Repeat(5)
    @Test
    public void shouldHandleManyCoinsByOneTime() {
        int promosCount = 50;
        for (int i = 0; i < promosCount; i++) {
            Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
            triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                    orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));
        }

        List<TriggerActionResult> result = triggerEventQueueService.insertAndProcessEvent(
                EventFactory.orderStatusUpdated(), discountUtils.getRulesPayload(), BudgetMode.SYNC);
        assertThat(processResultUtils.request(result, Coin.class), hasSize(promosCount));
    }

    @Repeat(5)
    @Test
    public void shouldHandleManyCoinsByOneTimeParallel() throws Exception {
        clock.useRealClock();
        int promosCount = 5;
        for (int i = 0; i < promosCount; i++) {
            Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
            triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                    orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));
        }

        testConcurrency(() -> () -> {
            List<TriggerActionResult> result = triggerEventQueueService.insertAndProcessEvent(
                    EventFactory.orderStatusUpdated(), discountUtils.getRulesPayload(), BudgetMode.SYNC);
            assertThat(processResultUtils.request(result, Coin.class), hasSize(promosCount));
        });
    }

    @Repeat(5)
    @Test
    public void shouldHandleParallelInsertEventWithProcessAndTmsCall() throws Exception {
        clock.useRealClock();
        BigDecimal bigEmissionBudget = BigDecimal.valueOf(1_000_000);
        Promo promo =
                promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(bigEmissionBudget));
        triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo, orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));

        testConcurrency((cpuCount) -> Stream
                .concat(
                        Stream.generate((Supplier<ExceptionUtils.RunnableWithException<Exception>>) () -> () -> {
                            List<TriggerActionResult> result = triggerEventQueueService.insertAndProcessEvent(
                                    EventFactory.orderStatusUpdated(), discountUtils.getRulesPayload(),
                                    BudgetMode.SYNC);
                            assertThat(processResultUtils.request(result, Coin.class), hasSize(1));
                        }).limit(cpuCount - 1),
                        Stream.of(() -> triggerEventQueueService.processEventsFromQueue(Duration.ZERO))
                ).collect(Collectors.toList())
        );

        assertThat(
                promoService.getPromo(promo.getId()).getCurrentEmissionBudget(),
                comparesEqualTo(bigEmissionBudget.subtract(BigDecimal.valueOf(CPU_COUNT - 1)))
        );

        assertTrue(jdbcTemplate.queryForList(
                "SELECT retry FROM discount WHERE uid = ?",
                Boolean.class,
                DEFAULT_UID
        ).stream().noneMatch(t -> t));
    }

    @Repeat(5)
    @Test
    public void shouldHandleMultiOrderManyCoinsByOneTimeParallel() throws Exception {
        clock.useRealClock();
        int promosCount = 10;
        for (int i = 0; i < promosCount; i++) {
            Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                    .setEmissionRestriction(MANY_COINS));
            triggersFactory.createOrderStatusUpdatedTriggerForCoin(promo,
                    orderRestriction(DEFAULT_ORDER_STATUS_PREDICATE));
        }

        testConcurrency(() -> () -> {
            List<TriggerActionResult> result = triggerEventQueueService.insertAndProcessEvent(
                    EventFactory.orderStatusUpdated(withMultiOrderId(DEFAULT_MULTI_ORDER_ID)),
                    discountUtils.getRulesPayload(), BudgetMode.SYNC);
            result.forEach(r -> assertNotEquals(TriggerActionResultStatus.ERROR, r.getStatus()));
        });

        triggerEventDao.getAll().forEach(e -> assertNotEquals(ERROR, e.getProcessedResult()));
    }

    @Repeat(5)
    @Test
    public void shouldHandlePartialErrorAndRetryIt() throws Exception {
        Trigger<BrokenLoginEvent> alwaysSuccessTrigger = triggersFactory.brokenLoginEventTrigger(
                promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed())
        );
        BrokenActionFactory.notFailForPromo(alwaysSuccessTrigger.getPromoId());

        Trigger<BrokenLoginEvent> notSuccessByFirstCallTrigger = triggersFactory.brokenLoginEventTrigger(
                promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed())
        );

        testConcurrency(() -> () -> triggerEventQueueService.insertAndProcessEvent(
                createBrokenLoginEvent(ThreadLocalRandom.current().nextLong(), CoreMarketPlatform.BLUE),
                discountUtils.getRulesPayload(), BudgetMode.SYNC
        ));

        Map<Long, List<TriggerActionResult>> resultsByTriggers = triggerEventDao.getAll().stream()
                .flatMap(event -> triggerActionResultDao.getTriggerEventProcessResults(event.getId()).stream())
                .collect(Collectors.groupingBy(TriggerActionResult::getTriggerId));

        assertThat(resultsByTriggers.get(alwaysSuccessTrigger.getId()), hasSize(CPU_COUNT));
        assertTrue(
                resultsByTriggers.get(alwaysSuccessTrigger.getId()).stream()
                        .allMatch(r -> r.getStatus() == TriggerActionResultStatus.SUCCESS)
        );
        assertThat(resultsByTriggers.get(notSuccessByFirstCallTrigger.getId()), hasSize(CPU_COUNT));
        assertTrue(
                resultsByTriggers.get(notSuccessByFirstCallTrigger.getId()).stream()
                        .allMatch(r -> r.getStatus() == TriggerActionResultStatus.ERROR)
        );

        BrokenActionFactory.notFailForPromo(notSuccessByFirstCallTrigger.getPromoId());

        clock.spendTime(1, ChronoUnit.HOURS);
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        List<TriggerEvent> eventsWithErrors = triggerEventDao.getAll().stream()
                .filter(event -> event.getProcessedResult() == ERROR)
                .collect(Collectors.toList());

        assertThat(eventsWithErrors, is(empty()));
    }

    @Test
    public void shouldMakeEventWaitIfRequiredEventsNotProcessed() {
        CheckouterUtils.OrderBuilder orderBuilder = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setOrderId(DEFAULT_ORDER_ID)
                .setDeliveryType(DeliveryType.DELIVERY)
                .setNoAuth(true)
                .addItem(defaultOrderItem().build());

        createOrderStatusUpdatedEvent(orderBuilder, ERROR);
        createOrderPaidEvent(orderBuilder, ERROR);
        createOrderTerminationEvent(orderBuilder, IN_QUEUE);

        triggerEventQueueService.processEventsFromQueue(Duration.ofSeconds(0));

        assertThat(
                triggerEventQueueService.findByUniqueKey(
                        ORDER_TERMINATION,
                        OrderTerminationEvent.toUniqueKey(DEFAULT_ORDER_ID)
                ),
                hasProperty("processedResult", equalTo(WAITING))
        );
    }

    @Test
    public void shouldReturnWaitingEventInQueueIfRequiredOrderStatusUpdatedEventProcessed() {
        CheckouterUtils.OrderBuilder orderBuilder = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setDeliveryType(DeliveryType.DELIVERY)
                .setNoAuth(true)
                .addItem(defaultOrderItem().build());

        createOrderStatusUpdatedEvent(orderBuilder, IN_QUEUE);
        createOrderPaidEvent(orderBuilder, SUCCESS);
        createOrderTerminationEvent(orderBuilder, WAITING);

        triggerEventQueueService.processEventsFromQueue(Duration.ofSeconds(0));


        assertThat(
                triggerEventQueueService.findByUniqueKey(
                        ORDER_TERMINATION,
                        OrderTerminationEvent.toUniqueKey(DEFAULT_ORDER_ID)
                ),
                hasProperty("processedResult", equalTo(IN_QUEUE))
        );
    }

    @Test
    public void shouldReturnWaitingEventInQueueIfRequiredOrderPaidEventProcessed() {
        CheckouterUtils.OrderBuilder orderBuilder = CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                .setDeliveryType(DeliveryType.DELIVERY)
                .setNoAuth(true)
                .addItem(defaultOrderItem().build());

        createOrderStatusUpdatedEvent(orderBuilder, SUCCESS);
        createOrderPaidEvent(orderBuilder, IN_QUEUE);
        createOrderTerminationEvent(orderBuilder, WAITING);

        triggerEventQueueService.processEventsFromQueue(Duration.ofSeconds(0));


        assertThat(
                triggerEventQueueService.findByUniqueKey(
                        ORDER_TERMINATION,
                        OrderTerminationEvent.toUniqueKey(DEFAULT_ORDER_ID)
                ),
                hasProperty("processedResult", equalTo(IN_QUEUE))
        );
    }

    private void createOrderPaidEvent(CheckouterUtils.OrderBuilder orderBuilder,
                                      TriggerEventProcessedResult processedResult) {
        Order order = orderBuilder
                .setOrderStatus(OrderStatus.PROCESSING)
                .build();

        InsertResult<BaseTriggerEvent.Builder<OrderPaidEvent>> result = triggerEventService.addNewEvent(
                OrderPaidEvent.createEvent(
                        CheckouterUtils.getEvent(order, order, HistoryEventType.ORDER_STATUS_UPDATED, clock),
                        null
                )
        );

        triggerEventDao.setProcessResult(
                result.getData().build(),
                processedResult,
                null
        );
    }

    private void createOrderTerminationEvent(CheckouterUtils.OrderBuilder orderBuilder,
                                             TriggerEventProcessedResult processedResult) {
        Order order = orderBuilder
                .setOrderStatus(OrderStatus.DELIVERED)
                .build();

        InsertResult<BaseTriggerEvent.Builder<OrderTerminationEvent>> result =
                triggerEventService.addNewEvent(
                        OrderTerminationEvent.createEvent(
                                CheckouterUtils.getEvent(order, order, HistoryEventType.ORDER_STATUS_UPDATED, clock),
                                null
                        )
                );

        if (processedResult != IN_QUEUE) {
            triggerEventDao.setProcessResult(
                    result.getData().build(),
                    processedResult,
                    null
            );
        }
    }

    private Order createOrderStatusUpdatedEvent(CheckouterUtils.OrderBuilder orderBuilder,
                                                TriggerEventProcessedResult status) {
        Order order = orderBuilder
                .setOrderStatus(OrderStatus.PROCESSING)
                .build();

        InsertResult<BaseTriggerEvent.Builder<OrderStatusUpdatedEvent>> result =
                triggerEventService.addNewEvent(
                        OrderStatusUpdatedEvent.createEvent(
                                null,
                                order,
                                null,
                                null)
                );

        triggerEventDao.setProcessResult(
                result.getData().build(),
                status,
                null
        );
        return order;
    }
}
