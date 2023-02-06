package ru.yandex.market.checkout.checkouter.order.status;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;


public class OrderControllerStatusChangeStatusByUserTest extends AbstractWebTestBase {

    public static final OrderSubstatus[] VALID_SUBSTATUSES_FOR_USER = {
            OrderSubstatus.USER_CHANGED_MIND,
            OrderSubstatus.USER_REFUSED_DELIVERY,
            OrderSubstatus.USER_WANTED_ANOTHER_PAYMENT_METHOD,
            OrderSubstatus.USER_FORGOT_TO_USE_BONUS,
            OrderSubstatus.REPLACING_ORDER,
            OrderSubstatus.USER_BOUGHT_CHEAPER,
            OrderSubstatus.USER_WANTS_TO_CHANGE_ADDRESS,
            OrderSubstatus.USER_WANTS_TO_CHANGE_DELIVERY_DATE,
            OrderSubstatus.CUSTOM
    };
    private Order order;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(VALID_SUBSTATUSES_FOR_USER)
                .map(substatus -> new Object[]{OrderStatus.CANCELLED, substatus})
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    @BeforeEach
    public void setUp() {
        order = orderCreateHelper.createOrder(new Parameters());
        assertThat(order.getStatus(), is(PROCESSING));
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldAllowToCancelWithUserSubstatus(OrderStatus status, OrderSubstatus substatus) {
        orderStatusHelper.updateOrderStatus(
                order.getId(),
                new ClientInfo(ClientRole.USER, order.getBuyer().getUid()),
                status,
                substatus
        );
    }
}
