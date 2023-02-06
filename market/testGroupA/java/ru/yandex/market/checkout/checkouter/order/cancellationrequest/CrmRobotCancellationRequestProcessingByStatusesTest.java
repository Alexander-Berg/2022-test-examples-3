package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.checkout.application.BaseStatusModelTest;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.CANCELLED_COURIER_NOT_FOUND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_NOT_COME_FOR_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_NOT_DELIVER_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_RETURNED_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_RETURNS_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_SEARCH_NOT_STARTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.CUSTOM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DAMAGED_BOX;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_NOT_MANAGED_REGION;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_PROBLEMS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_NOT_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.FULL_NOT_RANSOM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.INAPPROPRIATE_WEIGHT_SIZE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.INCOMPLETE_CONTACT_INFORMATION;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.INCOMPLETE_MULTI_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.LATE_CONTACT;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.MISSING_ITEM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PACKAGING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PENDING_CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PICKUP_EXPIRED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.READY_TO_SHIP;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.REPLACING_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SERVICE_FAULT;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHIPPED_TO_WRONG_DELIVERY_SERVICE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SORTING_CENTER_LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.STARTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_BOUGHT_CHEAPER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_FRAUD;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_PLACED_OTHER_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_RECEIVED_TECHNICAL_ERROR;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_PRODUCT;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_QUALITY;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_UNREACHABLE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTS_TO_CHANGE_ADDRESS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTS_TO_CHANGE_DELIVERY_DATE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP;

public class CrmRobotCancellationRequestProcessingByStatusesTest extends BaseStatusModelTest {

    private static final ClientInfo CRM_ROBOT_CLIENT_INFO = new ClientInfo(ClientRole.CRM_ROBOT, 0L);

    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                        USER_CHANGED_MIND,
                        USER_REFUSED_DELIVERY,
                        USER_PLACED_OTHER_ORDER,
                        USER_BOUGHT_CHEAPER,
                        CANCELLED_COURIER_NOT_FOUND,
                        COURIER_NOT_COME_FOR_ORDER,
                        COURIER_NOT_DELIVER_ORDER,
                        COURIER_RETURNED_ORDER,
                        COURIER_RETURNS_ORDER,
                        COURIER_SEARCH_NOT_STARTED,
                        CUSTOM,
                        DELIVERY_NOT_MANAGED_REGION,
                        DELIVERY_PROBLEMS,
                        DELIVERY_SERVICE_FAILED,
                        DELIVERY_SERVICE_LOST,
                        DELIVERY_SERVICE_NOT_RECEIVED,
                        FULL_NOT_RANSOM,
                        INAPPROPRIATE_WEIGHT_SIZE,
                        INCOMPLETE_CONTACT_INFORMATION,
                        INCOMPLETE_MULTI_ORDER,
                        LATE_CONTACT,
                        LOST,
                        MISSING_ITEM,
                        WAREHOUSE_FAILED_TO_SHIP,
                        PENDING_CANCELLED,
                        PICKUP_EXPIRED,
                        REPLACING_ORDER,
                        USER_FRAUD,
                        SORTING_CENTER_LOST,
                        USER_RECEIVED_TECHNICAL_ERROR,
                        USER_REFUSED_DELIVERY,
                        USER_REFUSED_PRODUCT,
                        USER_REFUSED_QUALITY,
                        USER_UNREACHABLE,
                        USER_WANTS_TO_CHANGE_ADDRESS,
                        USER_WANTS_TO_CHANGE_DELIVERY_DATE,
                        WAREHOUSE_FAILED_TO_SHIP,
                        SHIPPED_TO_WRONG_DELIVERY_SERVICE,
                        SERVICE_FAULT,
                        DAMAGED_BOX
                ).map(substatus -> new Object[]{CRM_ROBOT_CLIENT_INFO, substatus})
                .map(Arguments::of);
    }

    @BeforeEach
    @Override
    public void beforeEach() {
        Mockito.when(checkouterFeatureReader.getBoolean(BooleanFeatureType.ENABLE_USER_UNREACHABLE_VALIDATION))
                .thenReturn(false);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void createCancellationRequestStarted(ClientInfo clientInfo, OrderSubstatus substatus) {
        Order order = createOrder(PROCESSING, STARTED);
        statusUpdateValidator.validateStatusUpdate(order, CANCELLED, substatus, clientInfo);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void createCancellationRequestPackaging(ClientInfo clientInfo, OrderSubstatus substatus) {
        Order order = createOrder(PROCESSING, PACKAGING);
        statusUpdateValidator.validateStatusUpdate(order, CANCELLED, substatus, clientInfo);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void createCancellationRequestReadyToShp(ClientInfo clientInfo, OrderSubstatus substatus) {
        Order order = createOrder(PROCESSING, READY_TO_SHIP);
        statusUpdateValidator.validateStatusUpdate(order, CANCELLED, substatus, clientInfo);
    }

    private Order createOrder(OrderStatus status, OrderSubstatus substatus) {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(status);
        order.setSubstatus(substatus);
        order.setRgb(Color.BLUE);
        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        delivery.setType(DeliveryType.DELIVERY);
        order.setDelivery(delivery);
        return order;
    }
}
