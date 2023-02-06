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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;


public class OrderControllerStatusSetInvalidStatusByShopTest extends AbstractWebTestBase {

    private static final OrderSubstatus[] INVALID_SUBSTATUSES_FOR_SHOP = new OrderSubstatus[]{
            OrderSubstatus.USER_NOT_PAID,
            OrderSubstatus.RESERVATION_EXPIRED,
            OrderSubstatus.PROCESSING_EXPIRED,
            OrderSubstatus.USER_REFUSED_QUALITY,
            OrderSubstatus.SHOP_PENDING_CANCELLED,
            OrderSubstatus.PENDING_EXPIRED,
            OrderSubstatus.PENDING_CANCELLED,
            OrderSubstatus.USER_FRAUD,
            OrderSubstatus.RESERVATION_FAILED,
            OrderSubstatus.USER_PLACED_OTHER_ORDER,
            OrderSubstatus.USER_BOUGHT_CHEAPER,
            OrderSubstatus.MISSING_ITEM,
            OrderSubstatus.BROKEN_ITEM,
            OrderSubstatus.WRONG_ITEM,
            OrderSubstatus.PICKUP_EXPIRED,
            OrderSubstatus.DELIVERY_PROBLEMS,
            OrderSubstatus.LATE_CONTACT,
            OrderSubstatus.CUSTOM,
            OrderSubstatus.DELIVERY_SERVICE_FAILED,
            OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP,
            OrderSubstatus.DELIVERY_SERVICE_UNDELIVERED,
            OrderSubstatus.BANK_REJECT_CREDIT_OFFER,
            OrderSubstatus.CREDIT_OFFER_FAILED,
            OrderSubstatus.CUSTOMER_REJECT_CREDIT_OFFER,
            OrderSubstatus.SERVICE_FAULT
    };
    private Order order;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(INVALID_SUBSTATUSES_FOR_SHOP)
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
                        new ClientInfo(ClientRole.SHOP, order.getShopId()),
                        OrderStatus.CANCELLED,
                        orderSubstatus
                )
                .andExpect(status().isForbidden());
    }
}
