package ru.yandex.market.checkout.checkouter.pay;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.PAYMENT_CALL_BALANCE_UPDATE_PAYMENT;

/**
 * @author : poluektov
 * date: 2021-05-14.
 */
public class UpdatePaymentTest extends AbstractWebTestBase {

    @Autowired
    protected OrderHistoryEventsTestHelper eventsTestHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private CheckouterClient client;

    private Order order;

    @BeforeEach
    public void createOrder() {
        checkouterProperties.setEnableUpdatePaymentMode(true);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        order = orderCreateHelper.createOrder(parameters);
    }

    //    @Test
    public void testUpdatePaymentShouldSaveTimestamp() {
        Payment payment = orderPayHelper.payForOrder(order);
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);
        orderPayHelper.notifyPaymentClear(payment);
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertFalse(queuedCallService.existsQueuedCall(PAYMENT_CALL_BALANCE_UPDATE_PAYMENT, payment.getId()));

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        List<OrderHistoryEvent> events = eventsTestHelper.getEventsOfType(order.getId(),
                HistoryEventType.PAYMENT_PROCESSING_STARTED_IN_BALANCE);
        assertThat(events, hasSize(1));
        //проверяем что для нужного события проставлен платеж в orderAfter и есть заполненное поле balanceHandlingTime
        assertThat(
                Optional.ofNullable(events.get(0))
                        .map(OrderHistoryEvent::getOrderAfter)
                        .map(Order::getPayment)
                        .map(Payment::getBalanceHandlingTime)
                        .orElse(null),
                notNullValue()
        );
        // проверяем, что не заполнили payment для других событий
        List<OrderHistoryEvent> allEvents = eventsTestHelper.getAllEvents(order.getId());
        assertThat(allEvents.stream()
                        .filter(it -> it.getType() != HistoryEventType.PAYMENT_PROCESSING_STARTED_IN_BALANCE)
                        .map(OrderHistoryEvent::getOrderAfter)
                        .map(Order::getPayment)
                        .filter(Objects::nonNull)
                        .map(Payment::getBalanceHandlingTime)
                        .collect(Collectors.toList()),
                hasSize(0)
        );
    }

    //    @Test
    public void testUpdatePaymentShouldSaveTimestampOnlyOnce() throws InterruptedException {
        Order order2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());

        Payment payment = orderPayHelper.payForOrders(List.of(order, order2));
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);
        orderPayHelper.notifyPaymentClear(payment);
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertFalse(queuedCallService.existsQueuedCall(PAYMENT_CALL_BALANCE_UPDATE_PAYMENT, payment.getId()));

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        List<OrderHistoryEvent> events = eventsTestHelper.getEventsOfType(
                order.getId(),
                HistoryEventType.PAYMENT_PROCESSING_STARTED_IN_BALANCE
        );
        assertThat(events, hasSize(1));

        Thread.sleep(1000);

        order2 = orderService.getOrder(order2.getId());
        orderStatusHelper.proceedOrderToStatus(order2, OrderStatus.DELIVERED);

        assertTrue(queuedCallService.existsQueuedCall(PAYMENT_CALL_BALANCE_UPDATE_PAYMENT, payment.getId()));
        queuedCallService.executeQueuedCallSynchronously(PAYMENT_CALL_BALANCE_UPDATE_PAYMENT, payment.getId());
        assertFalse(queuedCallService.existsQueuedCall(PAYMENT_CALL_BALANCE_UPDATE_PAYMENT, payment.getId()));

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        Date secondBalanceHandlingTime = payment.getBalanceHandlingTime();
        assertNotNull(secondBalanceHandlingTime);

        events = eventsTestHelper.getEventsOfType(
                order.getId(),
                HistoryEventType.PAYMENT_PROCESSING_STARTED_IN_BALANCE
        );
        assertThat(events, hasSize(1));
    }
}
