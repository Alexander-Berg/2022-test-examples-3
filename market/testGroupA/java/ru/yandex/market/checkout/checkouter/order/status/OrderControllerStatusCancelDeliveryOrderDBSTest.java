package ru.yandex.market.checkout.checkouter.order.status;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DescribedSubstatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderControllerStatusCancelDeliveryOrderDBSTest extends AbstractWebTestBase {

    public static final ImmutableSet<OrderSubstatus> CANCEL_SUBSTATUSES_AVAILABLE_FOR_CALLCENTER_OPERATOR =
            ImmutableSet.of(
                    OrderSubstatus.SHOP_FAILED,
                    OrderSubstatus.SERVICE_FAULT,
                    OrderSubstatus.SORTING_CENTER_LOST,
                    OrderSubstatus.LOST,
                    OrderSubstatus.COURIER_SEARCH_NOT_STARTED,
                    OrderSubstatus.CANCELLED_COURIER_NOT_FOUND,
                    OrderSubstatus.COURIER_NOT_COME_FOR_ORDER,
                    OrderSubstatus.DELIVERY_NOT_MANAGED_REGION,
                    OrderSubstatus.INAPPROPRIATE_WEIGHT_SIZE,
                    OrderSubstatus.INCOMPLETE_CONTACT_INFORMATION,
                    OrderSubstatus.INCOMPLETE_MULTI_ORDER,
                    OrderSubstatus.TECHNICAL_ERROR,
                    OrderSubstatus.USER_FRAUD,
                    OrderSubstatus.USER_CHANGED_MIND,
                    OrderSubstatus.USER_UNREACHABLE,
                    OrderSubstatus.USER_REFUSED_DELIVERY,
                    OrderSubstatus.USER_REFUSED_PRODUCT,
                    OrderSubstatus.USER_REFUSED_QUALITY,
                    OrderSubstatus.DELIVERY_SERVICE_NOT_RECEIVED,
                    OrderSubstatus.DELIVERY_SERVICE_LOST,
                    OrderSubstatus.SHIPPED_TO_WRONG_DELIVERY_SERVICE,
                    OrderSubstatus.DELIVERY_SERVICE_FAILED,
                    OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP,
                    OrderSubstatus.MISSING_ITEM,
                    OrderSubstatus.USER_CHANGED_MIND,
                    OrderSubstatus.USER_REFUSED_DELIVERY,
                    OrderSubstatus.USER_PLACED_OTHER_ORDER,
                    OrderSubstatus.USER_BOUGHT_CHEAPER,
                    OrderSubstatus.WRONG_ITEM,
                    OrderSubstatus.BROKEN_ITEM,
                    OrderSubstatus.DELIVERY_PROBLEMS,
                    OrderSubstatus.USER_WANTS_TO_CHANGE_ADDRESS,
                    OrderSubstatus.USER_WANTS_TO_CHANGE_DELIVERY_DATE,
                    OrderSubstatus.COURIER_NOT_DELIVER_ORDER,
                    OrderSubstatus.DAMAGED_BOX
            );

    public static final ImmutableSet<OrderSubstatus> CANCEL_SUBSTATUSES_AVAILABLE_FOR_SYSTEM =
            ImmutableSet.of(
                    OrderSubstatus.MISSING_ITEM,
                    OrderSubstatus.AS_PART_OF_MULTI_ORDER,
                    OrderSubstatus.SERVICE_FAULT,
                    OrderSubstatus.SORTING_CENTER_LOST,
                    OrderSubstatus.LOST,
                    OrderSubstatus.COURIER_SEARCH_NOT_STARTED,
                    OrderSubstatus.CANCELLED_COURIER_NOT_FOUND,
                    OrderSubstatus.COURIER_NOT_COME_FOR_ORDER,
                    OrderSubstatus.DELIVERY_NOT_MANAGED_REGION,
                    OrderSubstatus.INAPPROPRIATE_WEIGHT_SIZE,
                    OrderSubstatus.INCOMPLETE_CONTACT_INFORMATION,
                    OrderSubstatus.INCOMPLETE_MULTI_ORDER,
                    OrderSubstatus.TECHNICAL_ERROR,
                    OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP,
                    OrderSubstatus.COURIER_NOT_DELIVER_ORDER,
                    OrderSubstatus.DAMAGED_BOX
            );

    private Order order;

    public static Stream<Arguments> parameterizedTestData() {

        return CANCEL_SUBSTATUSES_AVAILABLE_FOR_CALLCENTER_OPERATOR.stream()
                .map(osub -> new Object[]{osub})
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    @BeforeEach
    public void prepareOrder() {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), is(OrderStatus.PROCESSING));
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testCancelByCallCenterOperatorWithoutSubstatusesMap(OrderSubstatus orderSubstatus) {
        Order cancelled = orderStatusHelper.updateOrderStatus(
                this.order.getId(),
                new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, 1L),
                OrderStatus.CANCELLED,
                orderSubstatus
        );

        assertThat(cancelled.getStatus(), is(OrderStatus.CANCELLED));
        assertThat(cancelled.getSubstatus(), is(orderSubstatus));
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testCancelBySystemWithoutSubstatusesMap(OrderSubstatus orderSubstatus) {
        Order cancelled = orderStatusHelper.updateOrderStatus(
                this.order.getId(),
                new ClientInfo(ClientRole.SYSTEM, 1L),
                OrderStatus.CANCELLED,
                orderSubstatus
        );

        assertThat(cancelled.getStatus(), is(OrderStatus.CANCELLED));
        assertThat(cancelled.getSubstatus(), is(orderSubstatus));
    }

    @Test
    public void getRulesForCallCenterOperatorWithoutSubstatusesMap() {
        var rules = client.getCancellationRules(ClientRole.CALL_CENTER_OPERATOR);
        var receivedSubstatuses = rules.getCancellationRulesMap().keySet()
                .stream().filter(s -> s == OrderStatus.DELIVERY)
                .flatMap(s -> rules.getCancellationRulesMap().get(s).getSubstatuses().stream())
                .map(DescribedSubstatus::getOrderSubstatus)
                .collect(Collectors.toSet());
        assertEquals(CANCEL_SUBSTATUSES_AVAILABLE_FOR_CALLCENTER_OPERATOR.size(), receivedSubstatuses.size());
        assertTrue(CANCEL_SUBSTATUSES_AVAILABLE_FOR_CALLCENTER_OPERATOR.containsAll(receivedSubstatuses));
        assertTrue(receivedSubstatuses.containsAll(CANCEL_SUBSTATUSES_AVAILABLE_FOR_CALLCENTER_OPERATOR));
    }

    @Test
    public void getRulesForSystemWithoutSubstatusesMap() {
        var rules = client.getCancellationRules(ClientRole.SYSTEM);
        var receivedSubstatuses = rules.getCancellationRulesMap().keySet()
                .stream().filter(s -> s == OrderStatus.DELIVERY)
                .flatMap(s -> rules.getCancellationRulesMap().get(s).getSubstatuses().stream())
                .map(DescribedSubstatus::getOrderSubstatus)
                .collect(Collectors.toSet());
        assertEquals(CANCEL_SUBSTATUSES_AVAILABLE_FOR_SYSTEM.size(), receivedSubstatuses.size());
        assertTrue(CANCEL_SUBSTATUSES_AVAILABLE_FOR_SYSTEM.containsAll(receivedSubstatuses));
        assertTrue(receivedSubstatuses.containsAll(CANCEL_SUBSTATUSES_AVAILABLE_FOR_SYSTEM));
    }
}
