package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentReadingDao;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CardTypeFetchTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderHistoryEventsTestHelper eventsTestHelper;
    @Autowired
    private PaymentReadingDao paymentReadingDao;

    @Test
    public void testCardTypeParameterIsStoredAsOrderProperty() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payHelper.payForOrderWithoutNotification(order);
        Order paidOrder = orderService.getOrder(order.getId());
        Payment payment = paidOrder.getPayment();

        CheckBasketParams basketParams = CheckBasketParams.buildHoldCheckBasket();
        String paymentSystem = "MASTERCARD";
        basketParams.setCardType(paymentSystem);
        trustMockConfigurer.mockCheckBasket(basketParams);
        trustMockConfigurer.mockStatusBasket(basketParams, null);

        payHelper.notifyPayment(payment);

        Order updatedOrder = orderService.getOrder(order.getId());
        assertThat(updatedOrder.getProperty(OrderPropertyType.PAYMENT_SYSTEM), is(paymentSystem));

        OrderHistoryEvent event = eventsTestHelper.getAllEvents(order.getId()).iterator().next();
        assertThat(event.getOrderAfter().getProperty(OrderPropertyType.PAYMENT_SYSTEM), is(paymentSystem));
    }

    @Test
    public void testCardTypeAreSavedAfterNotify() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payHelper.payForOrderWithoutNotification(order);
        Order paidOrder = orderService.getOrder(order.getId());
        Payment payment = paidOrder.getPayment();

        CheckBasketParams basketParams = CheckBasketParams.buildHoldCheckBasket();
        String paymentSystem = "MASTERCARD";
        basketParams.setCardType(paymentSystem);
        trustMockConfigurer.mockCheckBasket(basketParams);
        trustMockConfigurer.mockStatusBasket(basketParams, null);

        payHelper.notifyPayment(payment);
        Payment paymentFromDB = paymentReadingDao.loadPayment(payment.getId());
        assertThat(paymentFromDB.getPaymentSystem(), is(paymentSystem));
    }

}
