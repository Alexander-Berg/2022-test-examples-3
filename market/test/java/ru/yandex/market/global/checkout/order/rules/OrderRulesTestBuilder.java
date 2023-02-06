package ru.yandex.market.global.checkout.order.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yoomoney.tech.dbqueue.api.EnqueueResult;
import ru.yoomoney.tech.dbqueue.api.QueueProducer;

import ru.yandex.market.global.checkout.config.OrderRulesConfig;
import ru.yandex.market.global.checkout.domain.event.EventRepository;
import ru.yandex.market.global.checkout.domain.order.OrderRepository;
import ru.yandex.market.global.checkout.domain.order.OrderRulesService;
import ru.yandex.market.global.checkout.domain.order.rule.OrderRule;
import ru.yandex.market.global.checkout.domain.queue.base.OrderIdProducer;
import ru.yandex.market.global.checkout.domain.queue.task.CheckOrderTerminatedProducer;
import ru.yandex.market.global.checkout.domain.queue.task.CreateCheckDeliveredOrderTicketProducer;
import ru.yandex.market.global.checkout.domain.queue.task.CreateManualOrderTicketProducer;
import ru.yandex.market.global.checkout.domain.queue.task.DeliveryCancelProducer;
import ru.yandex.market.global.checkout.domain.queue.task.DeliveryCourierCancelProducer;
import ru.yandex.market.global.checkout.domain.queue.task.DeliveryCourierSearchProducer;
import ru.yandex.market.global.checkout.domain.queue.task.DeliveryPlaceOrderProducer;
import ru.yandex.market.global.checkout.domain.queue.task.FireEventProducer;
import ru.yandex.market.global.checkout.domain.queue.task.PrePaymentSuccessProducer;
import ru.yandex.market.global.checkout.domain.queue.task.RestorePromoProducer;
import ru.yandex.market.global.checkout.domain.queue.task.ScheduleOrderCourierSearchProducer;
import ru.yandex.market.global.checkout.domain.queue.task.ScheduleOrderOutdateProducer;
import ru.yandex.market.global.checkout.domain.queue.task.SendOrderNotificationProducer;
import ru.yandex.market.global.checkout.domain.queue.task.ShopPlaceOrderProducer;
import ru.yandex.market.global.checkout.domain.queue.task.deliveries.UpdateCargoClaimInfoProducer;
import ru.yandex.market.global.checkout.domain.queue.task.deliveries.UpdateCourierReceiveOrderCodeProducer;
import ru.yandex.market.global.checkout.domain.queue.task.deliveries.UpdateCourierReturnOrderCodeProducer;
import ru.yandex.market.global.checkout.domain.queue.task.notification.SendPushProducer;
import ru.yandex.market.global.checkout.domain.queue.task.notification.SendRecipientOrderNotificationProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.OrderPlusRewardTopUpProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentAuthorizeProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentCancelProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentClearProducer;
import ru.yandex.market.global.checkout.domain.queue.task.receipt.PaymentCancellationProducer;
import ru.yandex.market.global.checkout.domain.queue.task.receipt.PaymentConfirmationProducer;
import ru.yandex.market.global.checkout.domain.queue.task.tracker.CheckOrderCollectedInTimeProducer;
import ru.yandex.market.global.checkout.domain.queue.task.tracker.CheckOrderDeliveredInTimeProducer;
import ru.yandex.market.global.checkout.domain.queue.task.tracker.CheckOrderSeenProducer;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderEvent;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPaymentOrderState;
import ru.yandex.market.global.db.jooq.enums.EProcessingMode;
import ru.yandex.market.global.db.jooq.enums.EShopOrderState;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;

import static ru.yandex.market.global.checkout.order.rules.OrderRulesTestBuilder.TestEvent.te;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_CANCEL_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_DELIVERED;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_PICKUP;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_PLACE_ORDER_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_RETURN_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_SEARCH_COURIER_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_SEARCH_COURIER_START;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.ORDER_CHECK_OUTDATED;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.PAYMENT_AUTHORIZE_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.PAYMENT_CANCEL_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.PAYMENT_CLEAR_OK;

@ParametersAreNonnullByDefault
public class OrderRulesTestBuilder {
    private static final int MAX_ORDER_PROCESSING_MINUTES = 60 * 48;
    private Map<Class<? extends QueueProducer<?>>, QueueProducer<?>> producers = new HashMap<>();

    @SuppressWarnings("unchecked")
    private final List<Queue<EOrderEvent>> timeline = Arrays.asList(new Queue[MAX_ORDER_PROCESSING_MINUTES]);
    private final ArrayList<Class<? extends QueueProducer<?>>> invocations = new ArrayList<>();
    private final AtomicInteger minute = new AtomicInteger();

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final ConfigurationService configurationService;

    private final TestOrderFactory testOrderFactory;

    private EProcessingMode processingMode = EProcessingMode.AUTO;
    private EOrderState orderState = EOrderState.NEW;
    private EDeliveryOrderState deliveryOrderState = EDeliveryOrderState.NEW;
    private EPaymentOrderState paymentOrderState = EPaymentOrderState.NEW;
    private EShopOrderState shopOrderState = EShopOrderState.NEW;
    private long shopId = 1;

    public OrderRulesTestBuilder(
            OrderRepository orderRepository,
            EventRepository eventRepository,
            ConfigurationService configurationService,
            TestOrderFactory testOrderFactory
    ) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.testOrderFactory = testOrderFactory;
        this.configurationService = configurationService;

        producers.put(FireEventProducer.class, createFireEventProducerMock());

        setOrderIdProducer(DeliveryPlaceOrderProducer.class, List.of(
                te(DELIVERY_PLACE_ORDER_OK, 1),
                te(DELIVERY_PLACE_ORDER_OK, 1)
        ));
        setOrderIdProducer(DeliveryCourierSearchProducer.class, List.of(
                te(DELIVERY_SEARCH_COURIER_OK, 5),
                te(DELIVERY_PICKUP, 5),
                te(DELIVERY_DELIVERED, 25)
        ));
        setOrderIdProducer(DeliveryCancelProducer.class, List.of(
                te(DELIVERY_CANCEL_OK, 20)
        ));
        setOrderIdProducer(DeliveryCourierCancelProducer.class, List.of(
                te(DELIVERY_RETURN_OK, 2)
        ));

        setOrderIdProducer(PaymentAuthorizeProducer.class, List.of(
                te(PAYMENT_AUTHORIZE_OK)
        ));
        setOrderIdProducer(PaymentClearProducer.class, List.of(
                te(PAYMENT_CLEAR_OK)
        ));
        setOrderIdProducer(PaymentCancelProducer.class, List.of(
                te(PAYMENT_CANCEL_OK)
        ));

        setOrderIdProducer(PaymentConfirmationProducer.class);
        setOrderIdProducer(PaymentCancellationProducer.class);
        setOrderIdProducer(OrderPlusRewardTopUpProducer.class);

        setOrderIdProducer(CheckOrderSeenProducer.class);
        setOrderIdProducer(CheckOrderCollectedInTimeProducer.class);
        setOrderIdProducer(CheckOrderDeliveredInTimeProducer.class);

        producers.put(SendOrderNotificationProducer.class,
                createNoEventProducerMock(SendOrderNotificationProducer.class)
        );
        producers.put(CreateCheckDeliveredOrderTicketProducer.class,
                createNoEventProducerMock(CreateCheckDeliveredOrderTicketProducer.class)
        );
        producers.put(CreateManualOrderTicketProducer.class,
                createNoEventProducerMock(CreateManualOrderTicketProducer.class)
        );
        producers.put(RestorePromoProducer.class,
                createNoEventProducerMock(RestorePromoProducer.class)
        );

        setOrderIdProducer(ScheduleOrderCourierSearchProducer.class, List.of(
                te(DELIVERY_SEARCH_COURIER_START, 15)
        ));
        setOrderIdProducer(ScheduleOrderOutdateProducer.class, List.of(
                te(ORDER_CHECK_OUTDATED, 35)
        ));

        producers.put(SendRecipientOrderNotificationProducer.class,
                createNoEventProducerMock(SendRecipientOrderNotificationProducer.class)
        );
        producers.put(UpdateCourierReceiveOrderCodeProducer.class,
                createNoEventProducerMock(UpdateCourierReceiveOrderCodeProducer.class)
        );
        producers.put(UpdateCourierReturnOrderCodeProducer.class,
                createNoEventProducerMock(UpdateCourierReturnOrderCodeProducer.class)
        );

        setOrderIdProducer(CheckOrderTerminatedProducer.class, List.of(
                te(ORDER_CHECK_OUTDATED, 1440)
        ));

        producers.put(SendPushProducer.class,
                createNoEventProducerMock(SendPushProducer.class)
        );

        producers.put(UpdateCargoClaimInfoProducer.class,
                createNoEventProducerMock(UpdateCargoClaimInfoProducer.class)
        );

        setOrderIdProducer(PrePaymentSuccessProducer.class, List.of(
                te(EOrderEvent.ORDER_NEW)
        ));

        producers.put(ShopPlaceOrderProducer.class,
                createNoEventProducerMock(ShopPlaceOrderProducer.class)
        );
    }

    public OrderRulesTestBuilder setInitialOrderState(
            EProcessingMode processingMode,
            EOrderState orderState,
            EDeliveryOrderState deliveryOrderState,
            EPaymentOrderState paymentOrderState,
            EShopOrderState shopOrderState
    ) {
        this.processingMode = processingMode;
        this.orderState = orderState;
        this.deliveryOrderState = deliveryOrderState;
        this.paymentOrderState = paymentOrderState;
        this.shopOrderState = shopOrderState;
        return this;
    }

    public OrderRulesTestBuilder setShopId(long shopId) {
        this.shopId = shopId;
        return this;
    }

    public OrderRulesTestBuilder setExternalEvents(
            List<TestEvent> events
    ) {
        events.forEach(e -> getTimelineSlot(e.delayMinutes).add(e.event));
        return this;
    }

    public <T extends OrderIdProducer> OrderRulesTestBuilder setOrderIdProducer(
            Class<T> clazz, List<TestEvent> events
    ) {
        producers.put(clazz, createOrderIdProducerMock(clazz, events));
        return this;
    }

    public <T extends OrderIdProducer> OrderRulesTestBuilder setOrderIdProducer(Class<T> clazz) {
        producers.put(clazz, createOrderIdProducerMock(clazz));
        return this;
    }

    public OrderRulesTest build() {
        OrderRulesConfig config = new OrderRulesConfig(
                getMock(FireEventProducer.class),
                getMock(DeliveryPlaceOrderProducer.class),
                getMock(DeliveryCourierSearchProducer.class),
                getMock(DeliveryCancelProducer.class),

                getMock(UpdateCargoClaimInfoProducer.class),
                getMock(UpdateCourierReceiveOrderCodeProducer.class),
                getMock(UpdateCourierReturnOrderCodeProducer.class),

                getMock(PaymentAuthorizeProducer.class),
                getMock(PaymentClearProducer.class),
                getMock(PaymentCancelProducer.class),
                getMock(OrderPlusRewardTopUpProducer.class),
                getMock(PaymentConfirmationProducer.class),
                getMock(PaymentCancellationProducer.class),
                getMock(SendOrderNotificationProducer.class),
                getMock(CreateCheckDeliveredOrderTicketProducer.class),
                getMock(CreateManualOrderTicketProducer.class),
                getMock(RestorePromoProducer.class),

                getMock(CheckOrderCollectedInTimeProducer.class),
                getMock(CheckOrderDeliveredInTimeProducer.class),
                getMock(CheckOrderSeenProducer.class),

                getMock(ScheduleOrderCourierSearchProducer.class),
                getMock(ScheduleOrderOutdateProducer.class),
                getMock(SendRecipientOrderNotificationProducer.class),
                getMock(CheckOrderTerminatedProducer.class),
                getMock(PrePaymentSuccessProducer.class),
                getMock(ShopPlaceOrderProducer.class),
                configurationService
        );
        config.init();

        List<OrderRule> rules = config.orderRules();
        OrderRulesService orderRulesService = new OrderRulesService(orderRepository, eventRepository, rules);

        Order order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setProcessingMode(processingMode)
                        .setOrderState(orderState)
                        .setDeliveryState(deliveryOrderState)
                        .setPaymentState(paymentOrderState)
                        .setShopState(shopOrderState)
                        .setShopId(shopId)
                )
                .build()
        ).getOrder();

        return new OrderRulesTest(minute, timeline, invocations, orderRulesService, orderRepository, order.getId());
    }

    private <T extends QueueProducer<?>> T getMock(Class<T> clazz) {
        //noinspection unchecked
        return Objects.requireNonNull((T) producers.get(clazz));
    }

    private <T extends QueueProducer<?>> T createNoEventProducerMock(Class<T> clazz) {
        T instance = Mockito.mock(clazz);
        Mockito.when(instance.enqueue(Mockito.any())).thenAnswer((Answer<EnqueueResult>) invocation -> {
            invocations.add(clazz);
            return null;
        });

        if (OrderIdProducer.class.isAssignableFrom(clazz)) {
            OrderIdProducer orderIdProducer = (OrderIdProducer) instance;
            Mockito.when(orderIdProducer.enqueue(Mockito.anyLong())).thenAnswer(invocation -> {
                invocations.add(clazz);
                return null;
            });
            Mockito.when(orderIdProducer.enqueueOrderId(Mockito.any())).thenAnswer(invocation -> {
                invocations.add(clazz);
                return null;
            });
        }

        return instance;
    }

    private <T extends OrderIdProducer> T createOrderIdProducerMock(Class<T> clazz, List<TestEvent> events) {
        T instance = Mockito.mock(clazz);
        Mockito.when(instance.enqueueOrderId(Mockito.any())).thenAnswer(invocation -> {
            events.forEach(e -> getTimelineSlot(minute.get() + e.delayMinutes).add(e.event));
            invocations.add(clazz);
            return null;
        });
        return instance;
    }

    private <T extends OrderIdProducer> T createOrderIdProducerMock(Class<T> clazz) {
        return createOrderIdProducerMock(clazz, Collections.emptyList());
    }

    private FireEventProducer createFireEventProducerMock() {
        FireEventProducer instance = Mockito.mock(FireEventProducer.class);

        Mockito.when(instance.enqueue(Mockito.any(), Mockito.any()))
                .thenAnswer((Answer<EnqueueResult>) this::fireEventInvocation);

        Mockito.when(instance.enqueue(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer((Answer<EnqueueResult>) this::fireEventInvocation);

        Mockito.when(instance.enqueue(Mockito.anyLong(), Mockito.any()))
                .thenAnswer((Answer<EnqueueResult>) this::fireEventInvocation);

        return instance;
    }

    private EnqueueResult fireEventInvocation(InvocationOnMock invocation) {
        EOrderEvent event = invocation.getArgument(1);
        getTimelineSlot(minute.get()).add(event);
        invocations.add(FireEventProducer.class);
        return null;
    }

    private Queue<EOrderEvent> getTimelineSlot(int eventMinute) {
        Queue<EOrderEvent> queue = timeline.get(eventMinute);
        if (queue == null) {
            queue = new LinkedList<>();
            timeline.set(eventMinute, queue);
        }
        return queue;
    }

    @AllArgsConstructor
    @Getter
    public static final class TestEvent {
        private final EOrderEvent event;
        private final int delayMinutes;

        public static TestEvent te(EOrderEvent event) {
            return new TestEvent(event, 0);
        }

        public static TestEvent te(EOrderEvent event, int delayMinutes) {
            return new TestEvent(event, delayMinutes);
        }
    }
}
