package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.BaseStatusModelTest;
import ru.yandex.market.checkout.backbone.validation.order.status.StatusUpdateValidator;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.BROKEN_ITEM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.CUSTOM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_PROBLEMS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_NOT_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PICKUP_EXPIRED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHIPPED_TO_WRONG_DELIVERY_SERVICE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_PLACED_OTHER_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.WRONG_ITEM;


public class CancellationRequestDeliveryPickupByStatusesTest extends BaseStatusModelTest {

    private static final ClientInfo USER_CLIENT_INFO = new ClientInfo(ClientRole.USER, BuyerProvider.UID);
    private static final ClientInfo CALL_CENTER_OPERATOR_CLIENT_INFO = new ClientInfo(
            ClientRole.CALL_CENTER_OPERATOR, 123L
    );
    private static final ClientInfo CRM_ROBOT_CLIENT_INFO = new ClientInfo(ClientRole.CRM_ROBOT, 0L);
    private static final ClientInfo ANTIFRAUD_ROBOT_CLIENT_INFO = new ClientInfo(ClientRole.ANTIFRAUD_ROBOT, 0L);

    @Autowired
    private StatusUpdateValidator statusUpdateValidator;

    @BeforeEach
    @Override
    public void beforeEach() {
        Mockito.when(checkouterFeatureReader.getBoolean(BooleanFeatureType.ENABLE_USER_UNREACHABLE_VALIDATION))
                .thenReturn(false);
    }

    public static Stream<Arguments> parameterizedTestData() {
        List<Object[]> result = Stream.of(
                Stream.of(USER_CHANGED_MIND, CUSTOM)
                        .map(ss -> new Object[]{USER_CLIENT_INFO, OrderStatus.DELIVERY, ss}),
                Stream.of(USER_CHANGED_MIND, CUSTOM)
                        .map(ss -> new Object[]{USER_CLIENT_INFO, OrderStatus.PICKUP, ss}),

                Stream.of(USER_CHANGED_MIND, WRONG_ITEM, DELIVERY_SERVICE_FAILED,
                                DELIVERY_SERVICE_NOT_RECEIVED, DELIVERY_SERVICE_LOST,
                                SHIPPED_TO_WRONG_DELIVERY_SERVICE)
                        .map(ss -> new Object[]{CALL_CENTER_OPERATOR_CLIENT_INFO, OrderStatus.DELIVERY, ss}),
                Stream.of(USER_CHANGED_MIND, PICKUP_EXPIRED, WRONG_ITEM, DELIVERY_SERVICE_NOT_RECEIVED,
                                DELIVERY_SERVICE_LOST, SHIPPED_TO_WRONG_DELIVERY_SERVICE)
                        .map(ss -> new Object[]{CALL_CENTER_OPERATOR_CLIENT_INFO, OrderStatus.PICKUP, ss}),

                Stream.of(USER_REFUSED_DELIVERY, BROKEN_ITEM)
                        .map(ss -> new Object[]{CRM_ROBOT_CLIENT_INFO, OrderStatus.DELIVERY, ss}),
                Stream.of(USER_REFUSED_DELIVERY, BROKEN_ITEM, PICKUP_EXPIRED)
                        .map(ss -> new Object[]{CRM_ROBOT_CLIENT_INFO, OrderStatus.PICKUP, ss}),

                Stream.of(USER_PLACED_OTHER_ORDER, DELIVERY_PROBLEMS)
                        .map(ss -> new Object[]{ANTIFRAUD_ROBOT_CLIENT_INFO, OrderStatus.DELIVERY, ss}),
                Stream.of(USER_PLACED_OTHER_ORDER, PICKUP_EXPIRED)
                        .map(ss -> new Object[]{ANTIFRAUD_ROBOT_CLIENT_INFO, OrderStatus.PICKUP, ss})
        ).flatMap(Function.identity()).collect(Collectors.toList());
        return result.stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void createCancellationRequest(ClientInfo clientInfo, OrderStatus status, OrderSubstatus substatus) {
        doReturn(false).when(checkouterFeatureReader).getBoolean(ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE);

        Order order = new Order();
        order.setId(1L);
        order.setStatus(status);
        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        delivery.setType(DeliveryType.PICKUP);
        order.setDelivery(delivery);
        statusUpdateValidator.validateStatusUpdate(order, CANCELLED, substatus, clientInfo);
    }
}
