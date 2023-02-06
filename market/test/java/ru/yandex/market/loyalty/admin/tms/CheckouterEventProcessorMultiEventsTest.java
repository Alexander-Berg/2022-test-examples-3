package ru.yandex.market.loyalty.admin.tms;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.checkouter.CheckouterEventRestProcessor;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType.CREATION;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderAmountRestriction;
import static ru.yandex.market.loyalty.core.utils.TriggersFactory.orderRestriction;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 06.07.17
 */
@TestFor({CheckouterEventRestProcessor.class, TriggerEventTmsProcessor.class})
public class CheckouterEventProcessorMultiEventsTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    private static final long ORDER_ID = 4524543L;
    private static final long ANOTHER_ORDER_ID = ORDER_ID + 1;
    public static final String DEFAULT_MULTI_ORDER_ID = "multiOrder";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    private CoinService coinService;

    @Test
    public void shouldSetMultiEventComplete() {
        Promo promo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING)
        );

        processMultiOrderEvent(OrderStatus.PROCESSING, ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2);
        assertEquals(1, hasEvent("processing_" + DEFAULT_MULTI_ORDER_ID));
        assertEquals(false, isEventComplete("processing_" + DEFAULT_MULTI_ORDER_ID));
        processMultiOrderEvent(OrderStatus.PENDING, ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2);
        assertEquals(1, hasEvent("processing_" + DEFAULT_MULTI_ORDER_ID));
        assertEquals(true, isEventComplete("processing_" + DEFAULT_MULTI_ORDER_ID));
    }


    @Test
    @Ignore
    public void shouldSetMultiTerminationEventWithClickAndCollectOrderComplete() {
        Promo promo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING)
        );

        processMultiOrderEvent(OrderStatus.DELIVERY, ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2, true);
        assertEquals(1, hasEvent("termination_" + DEFAULT_MULTI_ORDER_ID));
        assertEquals(false, isEventComplete("termination_" + DEFAULT_MULTI_ORDER_ID));
        processMultiOrderEvent(OrderStatus.DELIVERED, ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2);
        assertEquals(1, hasEvent("termination_" + DEFAULT_MULTI_ORDER_ID));
        assertEquals(true, isEventComplete("termination_" + DEFAULT_MULTI_ORDER_ID));
    }

    @Test
    public void shouldCreateMultiOrderTerminationEvent() {
        Promo promo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed());

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                promo, orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING)
        );

        processMultiOrderEvent(OrderStatus.DELIVERED, ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2);
        assertEquals(1, hasEvent("termination_" + DEFAULT_MULTI_ORDER_ID));
        assertEquals(false, isEventComplete("termination_" + DEFAULT_MULTI_ORDER_ID));
        processMultiOrderEvent(OrderStatus.DELIVERED, ANOTHER_ORDER_ID, DEFAULT_MULTI_ORDER_ID, 2);
        assertEquals(1, hasEvent("termination_" + DEFAULT_MULTI_ORDER_ID));
        assertEquals(true, isEventComplete("termination_" + DEFAULT_MULTI_ORDER_ID));
    }

    @Test
    public void shouldRevokeCoinAndReissueCoinsOnCancelOneOrder() {
        Promo bigPromo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed(BigDecimal.valueOf(500)));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                bigPromo,
                TriggerGroupType.RANDOM_TRIGGERS,
                orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING),
                orderAmountRestriction(BigDecimal.valueOf(5000), null)
        );

        Promo smallPromo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed(BigDecimal.valueOf(100)));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                smallPromo,
                TriggerGroupType.RANDOM_TRIGGERS,
                orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING),
                orderAmountRestriction(BigDecimal.ZERO, BigDecimal.valueOf(5000))
        );

        OrderItem orderItem1 = CheckouterUtils.defaultOrderItem()
                .setItemKey(DEFAULT_ITEM_KEY)
                .setPrice(BigDecimal.valueOf(3000))
                .setCount(BigDecimal.ONE)
                .build();
        OrderItem orderItem2 = CheckouterUtils.defaultOrderItem()
                .setItemKey(ANOTHER_ITEM_KEY)
                .setPrice(BigDecimal.valueOf(3000))
                .setCount(BigDecimal.ONE)
                .build();

        processMultiOrderEvent(
                OrderStatus.PROCESSING,
                ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem1)
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processMultiOrderEvent(
                OrderStatus.PROCESSING,
                ANOTHER_ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem2)
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ANOTHER_ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getAllCoinsByMultiOrderId(DEFAULT_MULTI_ORDER_ID).get(
                        CREATION),
                contains(
                        allOf(
                                hasProperty("status", equalTo(CoreCoinStatus.INACTIVE)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(500)))
                        )
                )
        );

        processMultiOrderEvent(
                OrderStatus.CANCELLED,
                ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem1)
        );

        processMultiOrderEvent(
                OrderStatus.DELIVERED,
                ANOTHER_ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem2)
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ANOTHER_ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getAllCoinsByMultiOrderId(DEFAULT_MULTI_ORDER_ID).get(CREATION),
                containsInAnyOrder(
                        allOf(
                                hasProperty("status", equalTo(CoreCoinStatus.REVOKED)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(500)))
                        ),
                        allOf(
                                hasProperty("status", equalTo(CoreCoinStatus.ACTIVE)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(100)))
                        )
                )
        );
    }

    @Test
    public void shouldRevokeCoinAndReissueCoinsOnItemsUpdated() {
        Promo bigPromo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed(BigDecimal.valueOf(500)));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                bigPromo,
                TriggerGroupType.RANDOM_TRIGGERS,
                orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING),
                orderAmountRestriction(BigDecimal.valueOf(5000), null)
        );

        Promo smallPromo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed(BigDecimal.valueOf(100)));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                smallPromo,
                TriggerGroupType.RANDOM_TRIGGERS,
                orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING),
                orderAmountRestriction(BigDecimal.ZERO, BigDecimal.valueOf(5000))
        );

        OrderItem orderItem1 = CheckouterUtils.defaultOrderItem()
                .setItemKey(DEFAULT_ITEM_KEY)
                .setPrice(BigDecimal.valueOf(3000))
                .setCount(BigDecimal.ONE)
                .build();
        OrderItem orderItem2 = CheckouterUtils.defaultOrderItem()
                .setItemKey(ANOTHER_ITEM_KEY)
                .setPrice(BigDecimal.valueOf(3000))
                .setCount(BigDecimal.ONE)
                .build();

        OrderItem orderItem3 = CheckouterUtils.defaultOrderItem()
                .setItemKey(ANOTHER_ITEM_KEY)
                .setPrice(BigDecimal.ONE)
                .setCount(BigDecimal.ONE)
                .build();

        processMultiOrderEvent(
                OrderStatus.PROCESSING,
                ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                List.of(orderItem1, orderItem2)
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processMultiOrderEvent(
                OrderStatus.PROCESSING,
                ANOTHER_ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem3)
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ANOTHER_ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getAllCoinsByMultiOrderId(DEFAULT_MULTI_ORDER_ID).get(
                        CREATION),
                contains(
                        allOf(
                                hasProperty("status", equalTo(CoreCoinStatus.INACTIVE)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(500)))
                        )
                )
        );

        processMultiOrderEvent(
                OrderStatus.PROCESSING,
                ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem1),
                HistoryEventType.ITEMS_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processMultiOrderEvent(
                OrderStatus.DELIVERED,
                ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem1)
        );

        processMultiOrderEvent(
                OrderStatus.DELIVERED,
                ANOTHER_ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem3)
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ANOTHER_ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getAllCoinsByMultiOrderId(DEFAULT_MULTI_ORDER_ID).get(CREATION),
                containsInAnyOrder(
                        allOf(
                                hasProperty("status", equalTo(CoreCoinStatus.REVOKED)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(500)))
                        ),
                        allOf(
                                hasProperty("status", equalTo(CoreCoinStatus.ACTIVE)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(100)))
                        )
                )
        );
    }

    @Test
    public void shouldNotRevokeCoinAndReissueCoinsOnItemsUpdated() {
        Promo bigPromo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed(BigDecimal.valueOf(500)));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                bigPromo,
                TriggerGroupType.RANDOM_TRIGGERS,
                orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING),
                orderAmountRestriction(BigDecimal.valueOf(5000), null)
        );

        Promo smallPromo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed(BigDecimal.valueOf(100)));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                smallPromo,
                TriggerGroupType.RANDOM_TRIGGERS,
                orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING),
                orderAmountRestriction(BigDecimal.ZERO, BigDecimal.valueOf(5000))
        );

        OrderItem orderItem1 = CheckouterUtils.defaultOrderItem()
                .setItemKey(DEFAULT_ITEM_KEY)
                .setPrice(BigDecimal.valueOf(3000))
                .setCount(BigDecimal.ONE)
                .build();
        OrderItem orderItem2 = CheckouterUtils.defaultOrderItem()
                .setItemKey(ANOTHER_ITEM_KEY)
                .setPrice(BigDecimal.valueOf(3000))
                .setCount(BigDecimal.ONE)
                .build();

        OrderItem orderItem3 = CheckouterUtils.defaultOrderItem()
                .setItemKey(ANOTHER_ITEM_KEY)
                .setPrice(BigDecimal.ONE)
                .setCount(BigDecimal.ONE)
                .build();

        processMultiOrderEvent(
                OrderStatus.PROCESSING,
                ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                List.of(orderItem1, orderItem2)
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processMultiOrderEvent(
                OrderStatus.PROCESSING,
                ANOTHER_ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem3)
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ANOTHER_ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getAllCoinsByMultiOrderId(DEFAULT_MULTI_ORDER_ID).get(
                        CREATION),
                contains(
                        allOf(
                                hasProperty("status", equalTo(CoreCoinStatus.INACTIVE)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(500)))
                        )
                )
        );

        processMultiOrderEvent(
                OrderStatus.PROCESSING,
                ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem1),
                HistoryEventType.ITEMS_UPDATED,
                HistoryEventReason.ITEM_INSTANCES_UPDATED
        );

        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processMultiOrderEvent(
                OrderStatus.DELIVERED,
                ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem1)
        );

        processMultiOrderEvent(
                OrderStatus.DELIVERED,
                ANOTHER_ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem3)
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ANOTHER_ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getAllCoinsByMultiOrderId(DEFAULT_MULTI_ORDER_ID).get(CREATION),
                containsInAnyOrder(
                        allOf(
                                hasProperty("status", equalTo(CoreCoinStatus.ACTIVE)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(500)))
                        )
                )
        );
    }

    @Test
    public void shouldActivateCoinsOnDeliveryAllMultiOrderOrders() {
        Promo bigPromo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed(BigDecimal.valueOf(500)));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                bigPromo,
                TriggerGroupType.RANDOM_TRIGGERS,
                orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING),
                orderAmountRestriction(BigDecimal.valueOf(5000), null)
        );

        Promo smallPromo = promoManager.createSmartShoppingPromo(SmartShopping.defaultFixed(BigDecimal.valueOf(100)));

        triggersFactory.createOrderStatusUpdatedTriggerForCoin(
                smallPromo,
                TriggerGroupType.RANDOM_TRIGGERS,
                orderRestriction(OrderStatusPredicate.EFFECTIVELY_PROCESSING),
                orderAmountRestriction(BigDecimal.ZERO, BigDecimal.valueOf(5000))
        );

        OrderItem orderItem1 = CheckouterUtils.defaultOrderItem()
                .setItemKey(DEFAULT_ITEM_KEY)
                .setPrice(BigDecimal.valueOf(3000))
                .setCount(BigDecimal.ONE)
                .build();
        OrderItem orderItem2 = CheckouterUtils.defaultOrderItem()
                .setItemKey(ANOTHER_ITEM_KEY)
                .setPrice(BigDecimal.valueOf(3000))
                .setCount(BigDecimal.ONE)
                .build();

        processMultiOrderEvent(
                OrderStatus.PROCESSING,
                ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem1)
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        processMultiOrderEvent(
                OrderStatus.PROCESSING,
                ANOTHER_ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem2)
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);


        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ANOTHER_ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getAllCoinsByMultiOrderId(DEFAULT_MULTI_ORDER_ID).get(CREATION),
                contains(
                        allOf(
                                hasProperty("status", equalTo(CoreCoinStatus.INACTIVE)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(500)))
                        )
                )
        );

        processMultiOrderEvent(
                OrderStatus.DELIVERED,
                ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem1)
        );

        processMultiOrderEvent(
                OrderStatus.DELIVERED,
                ANOTHER_ORDER_ID,
                DEFAULT_MULTI_ORDER_ID,
                2,
                false,
                Collections.singletonList(orderItem2)
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getCreatedCoinsByOrderId(ANOTHER_ORDER_ID),
                empty()
        );
        assertThat(
                coinService.search.getAllCoinsByMultiOrderId(DEFAULT_MULTI_ORDER_ID).get(CREATION),
                contains(
                        allOf(
                                hasProperty("status", equalTo(CoreCoinStatus.ACTIVE)),
                                hasProperty("nominal", comparesEqualTo(BigDecimal.valueOf(500)))
                        )
                )
        );
    }

    private Boolean isEventComplete(String uniqueKey) {
        return jdbcTemplate.queryForObject(
                "/*validator=false*/SELECT COMPLETE FROM trigger_event WHERE unique_key='" + uniqueKey + '\'',
                Boolean.class
        );
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private int hasEvent(String uniqueKey) {
        return jdbcTemplate.queryForObject(
                "/*validator=false*/SELECT count(*) FROM trigger_event WHERE unique_key='" + uniqueKey + '\'',
                Integer.class
        );
    }

    private void processMultiOrderEvent(OrderStatus status, long orderId, String multiOrder, int count) {
        processMultiOrderEvent(status, orderId, multiOrder, count, false);
    }

    private void processMultiOrderEvent(OrderStatus status, long orderId, String multiOrder, int count,
                                        boolean isClickAndCollect) {
        processMultiOrderEvent(status, orderId, multiOrder, count, isClickAndCollect, Collections.emptyList());
    }

    private void processMultiOrderEvent(
            OrderStatus status, long orderId, String multiOrder, int count, boolean isClickAndCollect,
            List<OrderItem> items
    ) {
        processMultiOrderEvent(
                status, orderId, multiOrder, count, isClickAndCollect, items, HistoryEventType.ORDER_STATUS_UPDATED
        );
    }

    private void processMultiOrderEvent(
            OrderStatus status, long orderId, String multiOrder, int count, boolean isClickAndCollect,
            List<OrderItem> items, HistoryEventType historyEventType
    ) {
        processMultiOrderEvent(
                status, orderId, multiOrder, count, isClickAndCollect, items, historyEventType, null
        );
    }

    private void processMultiOrderEvent(
            OrderStatus status, long orderId, String multiOrder, int count, boolean isClickAndCollect,
            List<OrderItem> items, HistoryEventType historyEventType, HistoryEventReason reason
    ) {
        Order order = CheckouterUtils.defaultOrder(status)
                .setOrderId(orderId)
                .setMultiOrderId(multiOrder)
                .setPaymentType(PaymentType.PREPAID)
                .setProperty(OrderPropertyType.MULTI_ORDER_SIZE, count)
                .setDeliveryPartnerType(
                        isClickAndCollect ? DeliveryPartnerType.SHOP : DeliveryPartnerType.YANDEX_MARKET
                )
                .addItems(items)
                .build();
        OrderHistoryEvent event = CheckouterUtils.getEvent(order, historyEventType, clock);
        event.setReason(reason);
        processEvent(event);
    }
}
