package ru.yandex.market.checkout.checkouter.order.status;

import java.util.function.Predicate;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;


public class OrderControllerStatusSetInvalidStatusByUserTest extends AbstractWebTestBase {

    private static final OrderSubstatus[] INVALID_SUBSTATUSES_FOR_USER = Stream.of(OrderSubstatus.values())
            .filter(substatus -> substatus.getStatus() == OrderStatus.CANCELLED)
            .filter(substatus -> Stream
                    .of(OrderControllerStatusChangeStatusByUserTest.VALID_SUBSTATUSES_FOR_USER)
                    .noneMatch(Predicate.isEqual(substatus)))
            .filter(os -> !os.isUnknown())
            .toArray(OrderSubstatus[]::new);
    private Order order;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(INVALID_SUBSTATUSES_FOR_USER)
                .map(substatus -> new Object[]{substatus})
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    @BeforeEach
    public void setUp() {
        order = orderCreateHelper.createOrder(new Parameters());
        assertThat(order.getStatus(), is(PROCESSING));
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldNotAllowToCancelOrderWithInvalidSubstatus(OrderSubstatus orderSubstatus) throws Exception {
        orderStatusHelper.updateOrderStatusForActions(
                order.getId(),
                new ClientInfo(ClientRole.USER, order.getBuyer().getUid()),
                OrderStatus.CANCELLED,
                orderSubstatus
        )
                .andExpect(status().isForbidden());
    }
}
