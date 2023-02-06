package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.market.checkout.application.BaseStatusModelTest;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.ANTIFRAUD_ROBOT;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.CALL_CENTER_OPERATOR;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.CRM_ROBOT;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.USER;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_USER_UNREACHABLE_VALIDATION;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.AWAIT_CONFIRMATION;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.CUSTOM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.LATE_CONTACT;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.REPLACING_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_BOUGHT_CHEAPER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_FORGOT_TO_USE_BONUS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_FRAUD;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_PLACED_OTHER_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_RECEIVED_TECHNICAL_ERROR;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTED_ANOTHER_PAYMENT_METHOD;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.WAITING_USER_INPUT;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CASH_ON_DELIVERY;

/**
 * Проверяет возможность отмены заказа из статусов UNPAID/PENDING в различные подстатусы CANNCELLED для
 * основных типов заказов (FF, FBS, DBS).
 * На самом деле логика cancellationRequestProcessor.canBeAppliedNow очень обширна и по-хорошему все тестовые сценарии
 * должны быть объединены вместе в одном классе.
 * <p>
 * Например, у нас есть FF заказ и вся логика тестирования бизнес-логики отмены (из cancellationRequestProcessor)
 * должна быть в одном месте, а сейчас она вся размазана по куче тестовых классов, причём тестируется это
 * не мини-модульными тестами, а полным прогоном через ручку /checkout
 */
public class CancellationRequestUnpaidPendingByStatusesTest extends BaseStatusModelTest {

    private static final ClientInfo USER_CLIENT_INFO = new ClientInfo(USER, BuyerProvider.UID);
    private static final ClientInfo CALL_CENTER_OPERATOR_CLIENT_INFO = new ClientInfo(CALL_CENTER_OPERATOR, 123L
    );
    private static final ClientInfo CRM_ROBOT_CLIENT_INFO = new ClientInfo(CRM_ROBOT, 0L);
    private static final ClientInfo ANTIFRAUD_ROBOT_CLIENT_INFO = new ClientInfo(ANTIFRAUD_ROBOT, 0L);


    public static Stream<Arguments> parameterizedTestData() {
        List<Object[]> result = Stream.of(
                Stream.of(
                        USER_CHANGED_MIND,
                        USER_REFUSED_DELIVERY,
                        USER_BOUGHT_CHEAPER,
                        CUSTOM,
                        USER_WANTED_ANOTHER_PAYMENT_METHOD,
                        USER_FORGOT_TO_USE_BONUS,
                        REPLACING_ORDER
                ).map(ss -> new Object[]{USER_CLIENT_INFO, UNPAID, ss}),
                Stream.of(
                        USER_CHANGED_MIND,
                        USER_REFUSED_DELIVERY,
                        USER_PLACED_OTHER_ORDER,
                        USER_BOUGHT_CHEAPER,
                        REPLACING_ORDER
                ).map(ss -> new Object[]{CALL_CENTER_OPERATOR_CLIENT_INFO, UNPAID, ss}),
                Stream.of(
                        USER_CHANGED_MIND,
                        USER_REFUSED_DELIVERY,
                        USER_PLACED_OTHER_ORDER,
                        USER_BOUGHT_CHEAPER,
                        REPLACING_ORDER
                ).map(ss -> new Object[]{CRM_ROBOT_CLIENT_INFO, UNPAID, ss}),
                Stream.of(
                        USER_FRAUD
                ).map(ss -> new Object[]{ANTIFRAUD_ROBOT_CLIENT_INFO, UNPAID, ss}),
                Stream.of(
                        USER_CHANGED_MIND,
                        USER_REFUSED_DELIVERY,
                        USER_PLACED_OTHER_ORDER,
                        USER_BOUGHT_CHEAPER,
                        REPLACING_ORDER
                ).map(ss -> new Object[]{ANTIFRAUD_ROBOT_CLIENT_INFO, PENDING, ss}),
                Stream.of(
                        LATE_CONTACT,
                        USER_CHANGED_MIND,
                        USER_REFUSED_DELIVERY,
                        USER_BOUGHT_CHEAPER,
                        REPLACING_ORDER,
                        USER_WANTED_ANOTHER_PAYMENT_METHOD,
                        USER_FORGOT_TO_USE_BONUS,
                        CUSTOM
                ).map(ss -> new Object[]{USER_CLIENT_INFO, PENDING, ss}),
                Stream.of(
                        USER_CHANGED_MIND,
                        USER_REFUSED_DELIVERY,
                        USER_PLACED_OTHER_ORDER,
                        USER_BOUGHT_CHEAPER,
                        REPLACING_ORDER,
                        USER_WANTED_ANOTHER_PAYMENT_METHOD,
                        USER_RECEIVED_TECHNICAL_ERROR
                ).map(ss -> new Object[]{CALL_CENTER_OPERATOR_CLIENT_INFO, PENDING, ss}),
                Stream.of(
                        USER_CHANGED_MIND,
                        USER_REFUSED_DELIVERY,
                        USER_PLACED_OTHER_ORDER,
                        USER_BOUGHT_CHEAPER,
                        REPLACING_ORDER
                ).map(ss -> new Object[]{CRM_ROBOT_CLIENT_INFO, PENDING, ss})
        ).flatMap(Function.identity()).collect(Collectors.toList());
        return result.stream().map(Arguments::of);
    }

    @ParameterizedTest(name = "orderFromStatus={0},cancellationToSubstatus={1},role={2}")
    @MethodSource("parameterizedTestData")
    public void cancelDbsOrder(
            ClientInfo clientInfo,
            OrderStatus orderFromStatus,
            OrderSubstatus cancellationToSubstatus
    ) {
        doReturn(false).when(checkouterFeatureReader).getBoolean(ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE);
        doReturn(false).when(checkouterFeatureReader).getBoolean(ENABLE_USER_UNREACHABLE_VALIDATION);

        Order order = createDbsOrder(orderFromStatus);

        boolean canBeAppliedNow = cancellationRequestProcessor.canBeAppliedNow(order, null, clientInfo);
        assertTrue(canBeAppliedNow, "cancellationRequestProcessor.canBeAppliedNow should return true");
        statusUpdateValidator.validateStatusUpdate(order, CANCELLED, cancellationToSubstatus, clientInfo);
    }

    private Order createDbsOrder(OrderStatus status) {
        Order order = mock(Order.class);
        doReturn(8972358L).when(order).getId();
        doAnswer(InvocationOnMock::callRealMethod).when(order).isSelfDelivery();
        doReturn(status).when(order).getStatus();
        doReturn(status == UNPAID ? WAITING_USER_INPUT : AWAIT_CONFIRMATION).when(order).getSubstatus();
        doReturn(CASH_ON_DELIVERY).when(order).getPaymentMethod();
        Delivery delivery = mock(Delivery.class);
        doReturn(DeliveryPartnerType.SHOP).when(delivery).getDeliveryPartnerType();
        doReturn(delivery).when(order).getDelivery();
        doAnswer(InvocationOnMock::callRealMethod).when(order).isSelfDelivery();
        return order;
    }

    @ParameterizedTest(name = "orderFromStatus={0},cancellationToSubstatus={1},role={2}")
    @MethodSource("parameterizedTestData")
    public void cancelFbsOrder(
            ClientInfo clientInfo,
            OrderStatus orderFromStatus,
            OrderSubstatus cancellationToSubstatus
    ) {
        doReturn(false).when(checkouterFeatureReader).getBoolean(ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE);
        doReturn(false).when(checkouterFeatureReader).getBoolean(ENABLE_USER_UNREACHABLE_VALIDATION);

        Order order = createFbsOrder(orderFromStatus);

        boolean canBeAppliedNow = cancellationRequestProcessor.canBeAppliedNow(order, null, clientInfo);
        assertTrue(canBeAppliedNow, "cancellationRequestProcessor.canBeAppliedNow should return true");
        statusUpdateValidator.validateStatusUpdate(order, CANCELLED, cancellationToSubstatus, clientInfo);
    }

    private Order createFbsOrder(OrderStatus status) {
        Order order = mock(Order.class);
        doReturn(8972358L).when(order).getId();
        doAnswer(InvocationOnMock::callRealMethod).when(order).isSelfDelivery();
        doReturn(status).when(order).getStatus();
        doReturn(status == UNPAID ? WAITING_USER_INPUT : AWAIT_CONFIRMATION).when(order).getSubstatus();
        doReturn(CASH_ON_DELIVERY).when(order).getPaymentMethod();
        doReturn(false).when(order).isFulfilment();
        Delivery delivery = mock(Delivery.class);
        doReturn(DeliveryPartnerType.YANDEX_MARKET).when(delivery).getDeliveryPartnerType();
        doReturn(delivery).when(order).getDelivery();
        doAnswer(InvocationOnMock::callRealMethod).when(order).isSelfDelivery();
        return order;
    }

    @ParameterizedTest(name = "orderFromStatus={0},cancellationToSubstatus={1},role={2}")
    @MethodSource("parameterizedTestData")
    public void cancelFFOrder(
            ClientInfo clientInfo,
            OrderStatus orderFromStatus,
            OrderSubstatus cancellationToSubstatus
    ) {
        doReturn(false).when(checkouterFeatureReader).getBoolean(ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE);
        doReturn(false).when(checkouterFeatureReader).getBoolean(ENABLE_USER_UNREACHABLE_VALIDATION);

        Order order = createFFOrder(orderFromStatus);

        boolean canBeAppliedNow = cancellationRequestProcessor.canBeAppliedNow(order, null, clientInfo);
        assertTrue(canBeAppliedNow, "cancellationRequestProcessor.canBeAppliedNow should return true");
        statusUpdateValidator.validateStatusUpdate(order, CANCELLED, cancellationToSubstatus, clientInfo);
    }

    private Order createFFOrder(OrderStatus status) {
        Order order = mock(Order.class);
        doReturn(8972358L).when(order).getId();
        doAnswer(InvocationOnMock::callRealMethod).when(order).isSelfDelivery();
        doReturn(status).when(order).getStatus();
        doReturn(status == UNPAID ? WAITING_USER_INPUT : AWAIT_CONFIRMATION).when(order).getSubstatus();
        doReturn(CASH_ON_DELIVERY).when(order).getPaymentMethod();
        doReturn(true).when(order).isFulfilment();
        Delivery delivery = mock(Delivery.class);
        doReturn(DeliveryPartnerType.YANDEX_MARKET).when(delivery).getDeliveryPartnerType();
        doReturn(List.of()).when(delivery).getParcels();
        doReturn(delivery).when(order).getDelivery();
        doAnswer(InvocationOnMock::callRealMethod).when(order).isSelfDelivery();
        return order;
    }

}
