package ru.yandex.market.checkout.checkouter.order.status;

import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;


public class OrderControllerStatusChangeOrderStatusTest extends AbstractWebTestBase {


    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                new Case(OrderStatus.PENDING, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED)),
                new Case(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.DELIVERY, OrderStatus.CANCELLED)),
                new Case(OrderStatus.DELIVERY, EnumSet.of(OrderStatus.PICKUP, OrderStatus.CANCELLED,
                        OrderStatus.DELIVERED)),
                new Case(OrderStatus.PICKUP, EnumSet.of(OrderStatus.CANCELLED, OrderStatus.DELIVERED))
        )
                .flatMap(c -> c.nextStatuses.stream().map(nextStatus -> new Object[]{c.status, nextStatus}))
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testProceedToCorrectStatus(OrderStatus orderStatus, OrderStatus nextStatus) {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, orderStatus);
        assertThat(order.getStatus(), is(orderStatus));

        OrderSubstatus substatus;
        if (nextStatus == OrderStatus.CANCELLED) {
            substatus = OrderSubstatus.USER_CHANGED_MIND;
        } else {
            substatus = null;
        }
        if (orderStatus == OrderStatus.DELIVERY || orderStatus == OrderStatus.PICKUP) {
            trustMockConfigurer.mockCheckBasket(buildPostAuth());
            trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        }

        Order updated = orderStatusHelper.updateOrderStatus(order.getId(), nextStatus, substatus);
        assertThat(updated.getStatus(), is(nextStatus));
    }

    private static class Case {

        private final OrderStatus status;
        private final EnumSet<OrderStatus> nextStatuses;

        Case(OrderStatus status, EnumSet<OrderStatus> nextStatuses) {
            this.status = status;
            this.nextStatuses = nextStatuses;
        }
    }
}
