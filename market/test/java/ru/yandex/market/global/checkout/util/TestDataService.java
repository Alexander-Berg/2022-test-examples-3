package ru.yandex.market.global.checkout.util;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

import ru.yandex.market.global.checkout.domain.order.OrderDeliveryRepository;
import ru.yandex.market.global.checkout.domain.order.OrderItemRepository;
import ru.yandex.market.global.checkout.domain.order.OrderPaymentRepository;
import ru.yandex.market.global.checkout.domain.order.OrderRepository;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPaymentOrderState;
import ru.yandex.market.global.db.jooq.enums.EShopOrderState;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderDelivery;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderPayment;

import static ru.yandex.market.global.db.jooq.Checkout.CHECKOUT;
import static ru.yandex.market.global.db.jooq.enums.EProcessingMode.AUTO;

@RequiredArgsConstructor
@Deprecated
@Slf4j
public class TestDataService {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestDataService.class).build();
    private static final int ORDERS_COUNT = 5;
    private static final int ITEMS_COUNT = 5;

    private final DSLContext ctx;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final OrderPaymentRepository orderPaymentRepository;

    public TestData saveTestData() {
        List<Order> orders = IntStream.range(0, ORDERS_COUNT)
                .mapToObj(i -> this.insertOrder())
                .collect(Collectors.toList());

        Order newOrder = insertOrderWithCustomOrder(o -> o
                .setProcessingMode(AUTO)
                .setOrderState(EOrderState.PROCESSING)
                .setPaymentState(EPaymentOrderState.NEW)
                .setDeliveryState(EDeliveryOrderState.NEW)
                .setShopState(EShopOrderState.NEW)
        );

        Order deliveringOrder = insertOrderWithCustomOrder(o -> o
                .setProcessingMode(AUTO)
                .setOrderState(EOrderState.PROCESSING)
                .setPaymentState(EPaymentOrderState.AUTHORIZED)
                .setDeliveryState(EDeliveryOrderState.DELIVERING_ORDER)
                .setShopState(EShopOrderState.READY)
        );

        Order deliveredOrder = insertOrderWithCustomOrder(o -> o
                .setProcessingMode(AUTO)
                .setOrderState(EOrderState.FINISHED)
                .setPaymentState(EPaymentOrderState.CLEARED)
                .setDeliveryState(EDeliveryOrderState.ORDER_DELIVERED)
                .setShopState(EShopOrderState.READY)
        );

        Order cancelingOrder = insertOrderWithCustomOrder(o -> o
                .setProcessingMode(AUTO)
                .setOrderState(EOrderState.CANCELING)
                .setPaymentState(EPaymentOrderState.NOT_AUTHORIZED)
                .setDeliveryState(EDeliveryOrderState.COURIER_FOUND)
                .setShopState(EShopOrderState.READY)
        );

        Order canceledOrder = insertOrderWithCustomOrder(o -> o
                .setProcessingMode(AUTO)
                .setOrderState(EOrderState.CANCELED)
                .setPaymentState(EPaymentOrderState.CANCELED)
                .setDeliveryState(EDeliveryOrderState.ORDER_CANCELED)
                .setShopState(EShopOrderState.READY)
        );

        return new TestData()
                .setOrders(orders)
                .setNewOrder(newOrder)
                .setDeliveredOrder(deliveredOrder)
                .setDeliveringOrder(deliveringOrder)
                .setCanceledOrder(canceledOrder)
                .setCancelingOrder(cancelingOrder);
    }

    public Order insertOrderWithCustomOrder(
            Function<Order, Order> orderUpdater
    ) {
        return insertOrder(orderUpdater, Function.identity());
    }

    public Order insertOrderWithCustomDelivery(
            Function<OrderDelivery, OrderDelivery> deliveryUpdater
    ) {
        return insertOrder(Function.identity(), deliveryUpdater);
    }

    public Order insertOrder() {
        return insertOrder(Function.identity(), Function.identity());
    }

    public Order insertOrder(
            Function<Order, Order> orderUpdater,
            Function<OrderDelivery, OrderDelivery> deliveryUpdater
    ) {
        Order order = orderUpdater.apply(RANDOM.nextObject(Order.class).setLocale("en"));
        orderRepository.insert(order);

        OrderDelivery delivery = deliveryUpdater.apply(RANDOM.nextObject(OrderDelivery.class))
                .setOrderId(order.getId());
        orderDeliveryRepository.insert(delivery);

        OrderPayment payment = RANDOM.nextObject(OrderPayment.class)
                .setOrderId(order.getId());
        orderPaymentRepository.insert(payment);

        List<OrderItem> items = RANDOM.objects(OrderItem.class, ITEMS_COUNT)
                .peek(i -> i.setOrderId(order.getId()))
                .collect(Collectors.toList());
        orderItemRepository.insert(items);

        return order;
    }

    public void cleanTestData() {
        for (Table<?> table : CHECKOUT.getTables()) {
            if (UpdatableRecord.class.isAssignableFrom(table.getRecordType())) {
                log.info("Truncate {}", table.getName());
                ctx.truncate(table).cascade().execute();
            } else {
                log.info("Skip truncate {}", table.getName());
            }
        }
    }

    @Data
    @Accessors(chain = true)
    public static class TestData {
        private List<Order> orders;
        private Order newOrder;
        private Order deliveringOrder;
        private Order deliveredOrder;
        private Order cancelingOrder;
        private Order canceledOrder;
    }
}
