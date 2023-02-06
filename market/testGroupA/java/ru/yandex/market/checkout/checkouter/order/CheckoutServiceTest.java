

package ru.yandex.market.checkout.checkouter.order;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.CallsRealMethods;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.backbone.order.reservation.OrderCompletionService;
import ru.yandex.market.checkout.backbone.order.reservation.OrderPlacingService;
import ru.yandex.market.checkout.backbone.order.reservation.OrderPlacingServiceImpl;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.providers.MultiOrderProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

/**
 * @author jkt on 12/05/2020.
 */
@Disabled
public class CheckoutServiceTest extends AbstractServicesTestBase {

    private static final int DEFAULT_PLACING_TIMEOUT_FOR_TESTS_MS = 10000;
    private static final String TEST_EXCEPTION = "Test Exception";
    private static final String SIBLING_FAILED_EXCEPTION = "Sibling basket failed";
    private static final String TIMEOUT_EXCEPTION = "All sync transactions failed to finish in 3000 ms";
    @Autowired
    private OrderPlacingService orderPlacingService;
    @Autowired
    private OrderCompletionService orderCompletionService;

    private OrderCompletionService orderCompletionServiceMock;

    @Autowired
    private OrderCreateService orderCreateService;

    private OrderCreateService orderCreateServiceMock;

    private OrderActionWrapper orderPlacerWrapper = new OrderActionWrapper();

    private OrderActionWrapper orderCompletionWrapper = new OrderActionWrapper();

    @BeforeEach
    public void initMocks() {
        orderCreateServiceMock = Mockito.spy(orderCreateService);
        ((OrderPlacingServiceImpl) orderPlacingService).setOrderCreateService(orderCreateServiceMock);
        ((OrderPlacingServiceImpl) orderPlacingService).setOrderPlacingTimeoutMs(DEFAULT_PLACING_TIMEOUT_FOR_TESTS_MS);
        orderCompletionServiceMock = Mockito.spy(orderCompletionService);
    }

    @Test
    public void whenPlacingConsecutivelyWithoutProblemsShouldSucceed() {
        doAsPlaceCall(new CallsRealMethods());

        Collection<Long> ordersBefore = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        int initialMultiOrderSize = 2;
        MultiOrder multiOrder = generateMultiOrder(initialMultiOrderSize);
        boolean result = orderPlacingService.placeMultiOrder(multiOrder);

        Collection<Long> ordersAfter = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isTrue();
            softly.assertThat(multiOrder.getOrderFailures()).isEmpty();
            softly.assertThat(multiOrder.size()).isEqualTo(initialMultiOrderSize);
            softly.assertThat(ordersAfter).hasSize(ordersBefore.size() + initialMultiOrderSize);
        });
    }

    @Test
    public void whenAnyOrderThrowsExceptionInConsecutivePlacingShouldRollbackALL() {
        doOnSecondPlaceCall(call -> {
            throw new IllegalStateException(TEST_EXCEPTION);
        });
        int initialMultiOrderSize = 2;
        MultiOrder multiOrder = generateMultiOrder(initialMultiOrderSize);

        Collection<Long> ordersBefore = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        boolean result = orderPlacingService.placeMultiOrder(multiOrder);

        Collection<Long> ordersAfter = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isFalse();
            softly.assertThat(multiOrder.getOrderFailures()).hasSize(initialMultiOrderSize);
            softly.assertThat(multiOrder.getOrderFailures())
                    .extracting(OrderFailure::getErrorDetails)
                    .containsExactlyInAnyOrder(TEST_EXCEPTION, SIBLING_FAILED_EXCEPTION);
            softly.assertThat(multiOrder.getOrders()).hasSize(0);
            softly.assertThat(ordersAfter).containsExactlyInAnyOrderElementsOf(ordersBefore);
        });
    }

    @Test
    public void whenPlacingOrdersShouldRunAsyncPlacing() {
        OrderPlacingService orderPlacingServiceSpy = Mockito.spy(orderPlacingService);

        doAsPlaceCall(new CallsRealMethods());

        MultiOrder singleOrder = generateMultiOrder(1);
        orderPlacingServiceSpy.placeMultiOrder(singleOrder);
        Mockito.verify(orderPlacingServiceSpy).placeMultiOrder(singleOrder);

        MultiOrder multiOrder = generateMultiOrder(2);
        orderPlacingServiceSpy.placeMultiOrder(multiOrder);
        Mockito.verify(orderPlacingServiceSpy).placeMultiOrder(multiOrder);
    }

    @Test
    public void whenPlacingWithoutProblemsShouldSucceed() {
        doAsPlaceCall(new CallsRealMethods());

        Collection<Long> ordersBefore = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        int initialMultiOrderSize = 2;
        MultiOrder multiOrder = generateMultiOrder(initialMultiOrderSize);
        boolean result = orderPlacingService.placeMultiOrder(multiOrder);

        Collection<Long> ordersAfter = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isTrue();
            softly.assertThat(multiOrder.getOrderFailures()).isEmpty();
            softly.assertThat(multiOrder.size()).isEqualTo(initialMultiOrderSize);
            softly.assertThat(ordersAfter).hasSize(ordersBefore.size() + initialMultiOrderSize);
        });
    }

    @Test
    public void whenPlacingWithoutProblemsShouldWaitTransactionFinish() {
        doAsPlaceCall(new CallsRealMethods());

        Assertions.assertThatCode(() -> {
            for (int i = 0; i < 100; i++) {
                MultiOrder multiOrder = generateMultiOrder(2);
                orderPlacingService.placeMultiOrder(multiOrder);
                orderService.transaction(multiOrder.getOrders().get(0).getId(), orderId -> {
                    orderUpdateService.addOrderProperty(OrderPropertyType.POSTPONED_STOCK_FREEZE.create(orderId, true));
                    return null;
                });
            }
        }).doesNotThrowAnyException();
    }

    @Test
    public void whenOneMultiOrderOverflowingThreadPoolSizeShouldTimeOutGracefully() {
        doAsPlaceCall(new CallsRealMethods());

        Collection<Long> ordersBefore = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        int initialMultiOrderSize = 10; // тредпул для тестов = 5 пропертя checkout.threadPool.size, ставим больше
        MultiOrder multiOrder = generateMultiOrder(initialMultiOrderSize);

        ((OrderPlacingServiceImpl) orderPlacingService).setOrderPlacingTimeoutMs(3000);
        boolean result = orderPlacingService.placeMultiOrder(multiOrder);

        waitTransactionsToFinish();

        Collection<Long> ordersAfter = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isFalse();
            softly.assertThat(multiOrder.getOrderFailures())
                    .extracting(OrderFailure::getErrorDetails)
                    .contains(TIMEOUT_EXCEPTION, SIBLING_FAILED_EXCEPTION);
            softly.assertThat(multiOrder.getOrders()).hasSize(0);
            softly.assertThat(ordersAfter).containsExactlyInAnyOrderElementsOf(ordersBefore);
        });
    }

    @Test
    public void whenPlacingHangsShouldRollbackAll() {
        int timeoutForPlacing = 500;
        doAsPlaceCall(new AnswersWithDelay(timeoutForPlacing * 2, new CallsRealMethods()));

        Collection<Long> ordersBefore = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        MultiOrder multiOrder = generateMultiOrder();

        ((OrderPlacingServiceImpl) orderPlacingService).setOrderPlacingTimeoutMs(timeoutForPlacing);
        boolean result = orderPlacingService.placeMultiOrder(multiOrder);

        waitTransactionsToFinish();

        Collection<Long> ordersAfter = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isFalse();
            softly.assertThat(multiOrder.getOrderFailures())
                    .extracting(OrderFailure::getErrorCode)
                    .containsExactlyInAnyOrder(OrderFailure.Code.UNKNOWN_ERROR, OrderFailure.Code.UNKNOWN_ERROR);
            softly.assertThat(multiOrder.getOrders()).hasSize(0);
            softly.assertThat(ordersAfter).containsExactlyInAnyOrderElementsOf(ordersBefore);
        });
    }

    @Test
    public void whenOneOrderTimeOutsInPlacingShouldRollbackALL() {
        int timeoutForPlacing = 3000;
        doOnSecondPlaceCall(call -> sleep(timeoutForPlacing * 2));
        MultiOrder multiOrder = generateMultiOrder();

        Collection<Long> ordersBefore = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        ((OrderPlacingServiceImpl) orderPlacingService).setOrderPlacingTimeoutMs(timeoutForPlacing);
        boolean result = orderPlacingService.placeMultiOrder(multiOrder);
        waitTransactionsToFinish();

        Collection<Long> ordersAfter = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isFalse();
            softly.assertThat(multiOrder.getOrderFailures())
                    .extracting(OrderFailure::getErrorDetails)
                    .containsExactlyInAnyOrder(TIMEOUT_EXCEPTION, SIBLING_FAILED_EXCEPTION);
            softly.assertThat(multiOrder.getOrders()).hasSize(0);
            softly.assertThat(ordersAfter).containsExactlyInAnyOrderElementsOf(ordersBefore);
        });
    }

    @Test
    public void whenAnyOrderThrowsExceptionInPlacingShouldRollbackALL() {
        doOnSecondPlaceCall(call -> {
            throw new IllegalStateException(TEST_EXCEPTION);
        });
        int initialMultiOrderSize = 2;
        MultiOrder multiOrder = generateMultiOrder(initialMultiOrderSize);

        Collection<Long> ordersBefore = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        boolean result = orderPlacingService.placeMultiOrder(multiOrder);

        Collection<Long> ordersAfter = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isFalse();
            softly.assertThat(multiOrder.getOrderFailures()).hasSize(initialMultiOrderSize);
            softly.assertThat(multiOrder.getOrderFailures())
                    .extracting(OrderFailure::getErrorDetails)
                    .containsExactlyInAnyOrder(TEST_EXCEPTION, SIBLING_FAILED_EXCEPTION);
            softly.assertThat(multiOrder.getOrders()).hasSize(0);
            softly.assertThat(ordersAfter).containsExactlyInAnyOrderElementsOf(ordersBefore);
        });
    }

    @Test
    public void whenFinishingOrderConsecutivelyWithoutProblemsShouldSucceed() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CONCURRENT_MULTI_ORDER_COMPLETION, true);

        doAsCompleteOrderCall(new CallsRealMethods());

        Collection<Long> ordersBefore = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        int initialMultiOrderSize = 2;
        ReservationContext reservationContext = generateReservationContext(initialMultiOrderSize);

        orderCompletionService.completeMultiOrderAsync(reservationContext);

        Collection<Long> ordersAfter = orderService.getOrderIds(new OrderSearchRequest(), ClientInfo.SYSTEM);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(reservationContext.getMultiOrder().size()).isEqualTo(initialMultiOrderSize);
            softly.assertThat(reservationContext.getMultiOrder().getOrderFailures()).isEmpty();
            softly.assertThat(reservationContext.getMultiOrder().isValid()).isTrue();
            softly.assertThat(ordersAfter).hasSize(ordersBefore.size() + initialMultiOrderSize);
        });
    }

    @Test
    public void whenFinishingOrderConcurrentlyShouldFailOnlyFailedOrders() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CONCURRENT_MULTI_ORDER_COMPLETION, true);

        doOnSecondCompleteOrderCall(call -> {
            throw new IllegalStateException(TEST_EXCEPTION);
        });

        int initialMultiOrderSize = 2;
        ReservationContext reservationContext = generateReservationContext(initialMultiOrderSize);
        List<Long> orderIds = reservationContext.getMultiOrder().getOrders().stream()
                .map(Order::getId)
                .collect(Collectors.toList());

        orderCompletionServiceMock.completeMultiOrderAsync(reservationContext);

        Map<Long, Order> ordersAfter = orderService.getOrders(orderIds, ClientInfo.SYSTEM);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(reservationContext.getMultiOrder().size()).isEqualTo(1);
            softly.assertThat(reservationContext.getMultiOrder().getOrderFailures().size()).isEqualTo(1);
            softly.assertThat(reservationContext.getMultiOrder().isValid()).isFalse();
            softly.assertThat(ordersAfter.values())
                    .extracting(Order::getStatus)
                    .containsExactlyInAnyOrder(OrderStatus.PENDING, OrderStatus.RESERVED);
        });
    }

    @NotNull
    private ReservationContext generateReservationContext(int initialMultiOrderSize) {
        MultiOrder multiOrder = generateMultiOrder(initialMultiOrderSize);
        List<Order> beforePlacingOrders = multiOrder.getOrders();
        orderPlacingService.placeMultiOrder(multiOrder);

        List<Order> reservedOrders = new ArrayList<>();
        multiOrder.getOrders().forEach(order ->
                reservedOrders.add(orderUpdateService.reserveOrder(order.getId(), order.getShopOrderId(),
                        order.getDelivery()))
        );
        var multiCartContext = MultiCartContext.createBy(
                ImmutableMultiCartParameters.builder().build(),
                multiOrder
        );
        var context = ImmutableMultiCartContext.from(multiCartContext, multiOrder);
        ReservationContext reservationContext = new ReservationContext(multiOrder, false, context.getApiSettings(),
                context.getActionId(), context.getExperiments(), beforePlacingOrders);
        multiOrder.setOrders(reservedOrders);
        return reservationContext;
    }

    private MultiOrder generateMultiOrder() {
        return generateMultiOrder(2);
    }

    private MultiOrder generateMultiOrder(int size) {
        List<Order> orders = new ArrayList<>();
        while (orders.size() < size) {
            Order order = OrderProvider.getPostPaidOrder();
            order.setDelivery(DeliveryProvider.getYandexMarketDelivery(true));
            order.getDelivery().setHash(DeliveryProvider.DELIVERY_HASH);
            orders.add(order);
        }
        return MultiOrderProvider.buildMultiOrder(orders);
    }

    private void doAsPlaceCall(Answer answer) {
        Mockito.doAnswer(answer).when(orderCreateServiceMock).placeOrder(Mockito.any(Order.class));
    }

    private void doOnSecondPlaceCall(Consumer<?> action) {
        AtomicBoolean isSecondCall = new AtomicBoolean(false);
        orderPlacerWrapper.setBeforeAction(() -> {
            if (isSecondCall.getAndSet(true)) {
                action.accept(null);
            }
            return null;
        });
        doAsPlaceCall(orderPlacerWrapper);
    }

    private void doAsCompleteOrderCall(Answer answer) {
        Mockito.doAnswer(answer)
                .when(orderCompletionServiceMock).completeOrder(Mockito.any(Order.class),
                Mockito.any(ClientInfo.class));
    }

    private void doOnSecondCompleteOrderCall(Consumer<?> action) {
        AtomicBoolean isSecondCall = new AtomicBoolean(false);
        orderCompletionWrapper.setBeforeAction(() -> {
            if (isSecondCall.getAndSet(true)) {
                action.accept(null);
            }
            return null;
        });
        doAsCompleteOrderCall(orderCompletionWrapper);
    }

    private void sleep(int timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private void waitTransactionsToFinish() {
        sleep(1000);
    }


    private static class OrderActionWrapper extends CallsRealMethods {

        private Supplier<?> beforeAction = () -> null;

        private Supplier<?> afterAction = () -> null;

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            beforeAction.get();
            Object answer = super.answer(invocation);
            afterAction.get();
            return answer;
        }

        public void setBeforeAction(Supplier<?> beforeAction) {
            this.beforeAction = beforeAction;
        }

        public void setAfterAction(Supplier<?> afterAction) {
            this.afterAction = afterAction;
        }
    }
}
