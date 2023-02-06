package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.balance.BalanceMockHelper;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author : poluektov
 * date: 2020-06-15.
 */
public class CashbackAmountFetchTest extends AbstractWebTestBase {

    @Autowired
    private BalanceMockHelper balanceMockHelper;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentOperations trustPaymentOperations;
    @Autowired
    private OrderHistoryEventsTestHelper eventsTestHelper;

    private Order order;
    private Payment payment;

    @BeforeEach
    public void before() {
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        payment = payHelper.payForOrder(order);
    }

    @Test
    public void testSpasiboPropertyInitialization() {
        balanceMockHelper.mockWholeBalance();
        CheckBasketParams basketParams = CheckBasketParams.buildPostAuth();
        BigDecimal cashbackValue = new BigDecimal("195.38");
        basketParams.setCashbackAmount(cashbackValue);
        trustMockConfigurer.mockCheckBasket(basketParams);
        trustMockConfigurer.mockStatusBasket(basketParams, null);
        transactionTemplate.execute(ts -> {
            trustPaymentOperations.updatePaymentStateFromPaymentSystem(payment, ClientInfo.SYSTEM);
            return null;
        });

        Order updatedOrder = orderService.getOrder(order.getId());
        assertThat(updatedOrder.getProperty(OrderPropertyType.SPASIBO_CASHBACK_AMOUNT), notNullValue());
        assertThat(updatedOrder.getProperty(OrderPropertyType.SPASIBO_CASHBACK_AMOUNT), equalTo(cashbackValue));

        OrderHistoryEvent event = eventsTestHelper.getAllEvents(order.getId()).iterator().next();
        assertThat(event.getOrderAfter().getProperty(OrderPropertyType.SPASIBO_CASHBACK_AMOUNT),
                equalTo(cashbackValue));
    }
}
