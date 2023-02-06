package ru.yandex.market.billing.checkout;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.OrderTrantimeDao;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.delivery.service.DeliveryEventTypeService;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.core.order.ParcelService;
import ru.yandex.market.core.order.model.MbiOrder;

import static org.hamcrest.CoreMatchers.describedAs;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Тест для {@link OrderStatusTransition}.
 *
 * @author ivmelnik
 * @since 17.10.18
 */
@DbUnitDataSet(before = "OrderStatusTransitionTest.before.csv")
@ExtendWith(MockitoExtension.class)
class OrderStatusTransitionTest extends FunctionalTest {

    private static final Date TRANTIME = Timestamp.valueOf(
            LocalDateTime.of(2018, 10, 17, 15, 0));

    private static final long ORDER_ZDRAV_CITY = 1000L;
    private static final long ORDER_ZDRAV_CITY_DROPSHIP = 1004L;
    private static final long ORDER_BLUE_FF = 1001L;
    private static final long ORDER_BLUE_NO_FF = 1002L;
    private static final long ORDER_BLUE_NO_FF_SERVICE_CENTER = 1003L;
    private static final long ORDER_BLUE_NO_FF_SERVICE_CENTER_DIFFERENT_SERVICE_TYPE = 1005L;

    private static final ImmutableList<Long> ALL_ORDER_IDS = ImmutableList.of(
            ORDER_ZDRAV_CITY, ORDER_BLUE_FF, ORDER_BLUE_NO_FF);

    private static final ImmutableList<OrderStatus> ALL_ORDER_STATUSES = ImmutableList.copyOf(OrderStatus.values());

    private static final Pair<Long, OrderStatusTransition> ZDRAV_CITY_PROCESSING_DELIVERY = createParams(ORDER_ZDRAV_CITY, OrderStatus.PROCESSING, OrderStatus.DELIVERY);
    private static final Pair<Long, OrderStatusTransition> ZDRAV_CITY_FEE_CANCELLATION = createParams(ORDER_ZDRAV_CITY, OrderStatus.DELIVERED, OrderStatus.CANCELLED);
    private static final Pair<Long, OrderStatusTransition> ZDRAV_CITY_PROCESSING_DELIVERY_DROPSHIP = createParams(ORDER_ZDRAV_CITY_DROPSHIP, OrderStatus.PROCESSING, OrderStatus.DELIVERY);
    private static final Pair<Long, OrderStatusTransition> BLUE_NO_FF_DROPSHIP_PROCESSING_DELIVERY = createParams(ORDER_BLUE_NO_FF_SERVICE_CENTER, OrderStatus.PROCESSING, OrderStatus.DELIVERY);
    private static final Pair<Long, OrderStatusTransition> BLUE_NO_FF_DROPSHIP_PROCESSING_DELIVERY_DIFFERENT_SERVICE_TYPE = createParams(ORDER_BLUE_NO_FF_SERVICE_CENTER_DIFFERENT_SERVICE_TYPE, OrderStatus.PROCESSING, OrderStatus.DELIVERY);
    private static final Pair<Long, OrderStatusTransition> BLUE_FF_PROCESSING_DELIVERY = createParams(ORDER_BLUE_FF, OrderStatus.PROCESSING, OrderStatus.DELIVERY);
    private static final Pair<Long, OrderStatusTransition> BLUE_FF_DELIVERY_DELIVERED = createParams(ORDER_BLUE_FF, OrderStatus.DELIVERY, OrderStatus.DELIVERED);
    private static final Pair<Long, OrderStatusTransition> BLUE_FF_PICKUP_DELIVERED = createParams(ORDER_BLUE_FF, OrderStatus.PICKUP, OrderStatus.DELIVERED);
    private static final Pair<Long, OrderStatusTransition> BLUE_FF_FEE_CANCELLATION = createParams(ORDER_BLUE_FF, OrderStatus.DELIVERED, OrderStatus.CANCELLED);
    private static final Pair<Long, OrderStatusTransition> BLUE_NO_FF_FEE_CANCELLATION = createParams(ORDER_BLUE_NO_FF, OrderStatus.DELIVERED, OrderStatus.CANCELLED);
    private static final Pair<Long, OrderStatusTransition> BLUE_NO_FF_PROCESSING_DELIVERY = createParams(ORDER_BLUE_NO_FF, OrderStatus.PROCESSING, OrderStatus.DELIVERY);
    private static final Pair<Long, OrderStatusTransition> BLUE_NO_FF_DELIVERY_DELIVERED = createParams(ORDER_BLUE_NO_FF, OrderStatus.DELIVERY, OrderStatus.DELIVERED);
    private static final Pair<Long, OrderStatusTransition> BLUE_NO_FF_PICKUP_DELIVERED = createParams(ORDER_BLUE_NO_FF, OrderStatus.PICKUP, OrderStatus.DELIVERED);
    private static final List<Pair<Long, OrderStatusTransition>> WITH_CHANGE_PARAMS = Stream.of(
            zdravCityParams(),
            zdravCityFeeCancellationParams(),
            zdravCityDropshipParams(),
            blueNoFfDropshipParams(),
            blueFfParams(),
            blueFfFeeCancellationParams(),
            blueNoFfFeeCancellationParams(),
            blueNoFfParams(),
            deliveryToCustomerParams()
    )
            .flatMap(Function.identity())
            .collect(Collectors.toList());
    @Mock
    private EventProcessorSupport eventProcessorSupport;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderTrantimeDao orderTrantimeDao;
    @Autowired
    private ParcelService parcelService;
    @Autowired
    private DeliveryInfoService deliveryInfoService;
    @Autowired
    private DeliveryEventTypeService deliveryEventTypeService;

    private static Pair<Long, OrderStatusTransition> createParams(long orderId, OrderStatus statusFrom, OrderStatus statusTo) {
        return Pair.of(orderId, createTransition(statusFrom, statusTo, true));
    }

    private static OrderStatusTransition createTransition(OrderStatus statusFrom, OrderStatus statusTo, boolean mandatory) {
        OrderStatusTransition transition = OrderStatusTransition.create(statusFrom, statusTo);
        if (mandatory) {
            assertThat(transition,
                    describedAs("Должен создаться переход статусов %0->%1",
                            notNullValue(), statusFrom.toString(), statusTo.toString()));
        }
        return transition;
    }

    private static Stream<Pair<Long, OrderStatusTransition>> zdravCityParams() {
        return Stream.of(ZDRAV_CITY_PROCESSING_DELIVERY);
    }

    private static Stream<Pair<Long, OrderStatusTransition>> zdravCityFeeCancellationParams() {
        return Stream.of(ZDRAV_CITY_FEE_CANCELLATION);
    }

    private static Stream<Pair<Long, OrderStatusTransition>> zdravCityDropshipParams() {
        return Stream.of(ZDRAV_CITY_PROCESSING_DELIVERY_DROPSHIP);
    }

    private static Stream<Pair<Long, OrderStatusTransition>> blueNoFfDropshipParams() {
        return Stream.of(BLUE_NO_FF_DROPSHIP_PROCESSING_DELIVERY);
    }

    private static Stream<Pair<Long, OrderStatusTransition>> blueNoFfDropshipDifferentServiceTypeParams() {
        return Stream.of(BLUE_NO_FF_DROPSHIP_PROCESSING_DELIVERY_DIFFERENT_SERVICE_TYPE);
    }

    private static Stream<Pair<Long, OrderStatusTransition>> blueFfParams() {
        return Stream.of(BLUE_FF_DELIVERY_DELIVERED, BLUE_FF_PICKUP_DELIVERED);
    }

    private static Stream<Pair<Long, OrderStatusTransition>> blueFfFeeCancellationParams() {
        return Stream.of(BLUE_FF_FEE_CANCELLATION);
    }

    private static Stream<Pair<Long, OrderStatusTransition>> blueNoFfFeeCancellationParams() {
        return Stream.of(BLUE_NO_FF_FEE_CANCELLATION);
    }

    private static Stream<Pair<Long, OrderStatusTransition>> blueNoFfParams() {
        return Stream.of(BLUE_NO_FF_DELIVERY_DELIVERED, BLUE_NO_FF_PICKUP_DELIVERED);
    }

    private static Stream<Pair<Long, OrderStatusTransition>> deliveryToCustomerParams() {
        return Stream.of(BLUE_FF_PROCESSING_DELIVERY, BLUE_NO_FF_PROCESSING_DELIVERY);
    }

    private static Stream<Pair<Long, OrderStatusTransition>> noChangesParams() {
        List<OrderStatusTransition> allNotProhibitedTransitions = Lists.cartesianProduct(
                ImmutableList.of(ALL_ORDER_STATUSES, ALL_ORDER_STATUSES))
                .stream()
                .map(l -> createTransition(l.get(0), l.get(1), false))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return Lists.cartesianProduct(
                ImmutableList.of(ALL_ORDER_IDS, allNotProhibitedTransitions)).stream()
                .map(l -> Pair.of((Long) l.get(0), (OrderStatusTransition) l.get(1)))
                .filter(p -> !WITH_CHANGE_PARAMS.contains(p));
    }

    @BeforeEach
    void setUp() {
        doReturn(orderTrantimeDao).when(eventProcessorSupport).getOrderTrantimeDao();
        doReturn(orderService).when(eventProcessorSupport).getOrderService();
        doReturn(parcelService).when(eventProcessorSupport).getParcelService();
        doReturn(deliveryInfoService).when(eventProcessorSupport).getDeliveryInfoService();
        doReturn(deliveryEventTypeService).when(eventProcessorSupport).getDeliveryEventTypeService();
    }

    private OrderHistoryEvent createOrderStatusUpdatedEvent(long orderId) {
        final Order orderBefore = mock(Order.class);
        final Order orderAfter = mock(Order.class);

        doReturn(orderId).when(orderAfter).getId();

        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        event.setOrderBefore(orderBefore);
        event.setOrderAfter(orderAfter);
        return event;
    }

    private void doUpdateStatus(Pair<Long, OrderStatusTransition> params) {
        long orderId = params.getLeft();
        OrderStatusTransition transition = params.getRight();
        MbiOrder order = orderService.getOrder(orderId);
        order = order.toBuilder()
                .setStatusExpiryDate(null)
                .build();
        OrderHistoryEvent event = createOrderStatusUpdatedEvent(orderId);
        EventContext ctx = new EventContext(event, eventProcessorSupport, TRANTIME);
        transition.doUpdateStatus(ctx, order);
    }

    @ParameterizedTest
    @MethodSource("zdravCityParams")
    @DbUnitDataSet(after = "OrderStatusTransitionZdravCityTest.after.csv")
    void zdravCity(Pair<Long, OrderStatusTransition> params) {
        doUpdateStatus(params);
    }

    @ParameterizedTest
    @MethodSource("zdravCityDropshipParams")
    @DbUnitDataSet(after = "OrderStatusTransitionZdravCityDropshipTest.after.csv")
    void zdravCityDropship(Pair<Long, OrderStatusTransition> params) {
        doUpdateStatus(params);
    }

    @ParameterizedTest
    @MethodSource("blueFfParams")
    @DbUnitDataSet(after = "OrderStatusTransitionBlueFfTest.after.csv")
    void blueFf(Pair<Long, OrderStatusTransition> params) {
        doUpdateStatus(params);
    }

    @ParameterizedTest
    @MethodSource("blueFfFeeCancellationParams")
    @DbUnitDataSet(after = "OrderStatusTransitionBlueFfFeeCancellationTest.after.csv")
    void blueFfFeeCancellation(Pair<Long, OrderStatusTransition> params) {
        doUpdateStatus(params);
    }

    @ParameterizedTest
    @MethodSource("blueNoFfParams")
    @DbUnitDataSet(after = "OrderStatusTransitionBlueNoFfTest.after.csv")
    void blueNoFf(Pair<Long, OrderStatusTransition> params) {
        doUpdateStatus(params);
    }

    @ParameterizedTest
    @MethodSource("blueNoFfDropshipParams")
    @DbUnitDataSet(after = "OrderStatusTransitionBlueNoFfDropshipTest.after.csv")
    void blueNoFfDropship(Pair<Long, OrderStatusTransition> params) {
        doUpdateStatus(params);
    }

    @ParameterizedTest
    @MethodSource("blueNoFfDropshipDifferentServiceTypeParams")
    @DbUnitDataSet(after = "OrderStatusTransitionBlueNoFfDropshipDifferentServiceTypeTest.after.csv")
    void blueNoFfDropshipDifferentServiceType(Pair<Long, OrderStatusTransition> params) {
        doUpdateStatus(params);
    }

    @ParameterizedTest
    @MethodSource("noChangesParams")
    @DbUnitDataSet(before = "OrderStatusTransitionZdravCityTest.after.csv", after = "OrderStatusTransitionZdravCityTest.after.csv")
    void noChanges(Pair<Long, OrderStatusTransition> params) {
        doUpdateStatus(params);
    }
}
