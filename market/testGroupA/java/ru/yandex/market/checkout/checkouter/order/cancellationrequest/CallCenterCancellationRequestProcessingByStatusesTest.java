package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.COURIER_SEARCH_NOT_STARTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_NOT_MANAGED_REGION;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_NOT_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DROPOFF_CLOSED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DROPOFF_LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.INCOMPLETE_CONTACT_INFORMATION;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.INCOMPLETE_MULTI_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.MISSING_ITEM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PACKAGING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.READY_TO_SHIP;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.REPLACING_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SERVICE_FAULT;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHIPPED_TO_WRONG_DELIVERY_SERVICE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SHOP_FAILED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.SORTING_CENTER_LOST;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.STARTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.TECHNICAL_ERROR;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_BOUGHT_CHEAPER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_FRAUD;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_PLACED_OTHER_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_RECEIVED_TECHNICAL_ERROR;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTED_ANOTHER_PAYMENT_METHOD;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP;

public class CallCenterCancellationRequestProcessingByStatusesTest extends BaseStatusModelTest {

    private static final ClientInfo CALL_CENTER_OPERATOR_CLIENT_INFO = new ClientInfo(
            ClientRole.CALL_CENTER_OPERATOR,
            123L
    );

    public static Stream<Arguments> parameterizedTestData() {
        List<Object[]> result = new ArrayList<>();
        for (Boolean isCreateByOrderEditApi : new Boolean[]{Boolean.FALSE, Boolean.TRUE}) {
            result.addAll(Stream.of(
                            SHOP_FAILED,
                            USER_CHANGED_MIND,
                            USER_REFUSED_DELIVERY,
                            USER_PLACED_OTHER_ORDER,
                            USER_BOUGHT_CHEAPER,
                            MISSING_ITEM,
                            WAREHOUSE_FAILED_TO_SHIP,
                            REPLACING_ORDER,
                            USER_FRAUD,
                            SERVICE_FAULT,
                            SORTING_CENTER_LOST,
                            LOST,
                            DELIVERY_NOT_MANAGED_REGION,
                            INCOMPLETE_CONTACT_INFORMATION,
                            INCOMPLETE_MULTI_ORDER,
                            TECHNICAL_ERROR,
                            USER_WANTED_ANOTHER_PAYMENT_METHOD,
                            USER_RECEIVED_TECHNICAL_ERROR,
                            DELIVERY_SERVICE_NOT_RECEIVED,
                            DELIVERY_SERVICE_LOST,
                            SHIPPED_TO_WRONG_DELIVERY_SERVICE,
                            COURIER_SEARCH_NOT_STARTED,
                            CANCELLED_COURIER_NOT_FOUND,
                            COURIER_NOT_COME_FOR_ORDER,
                            DROPOFF_LOST,
                            DROPOFF_CLOSED
                    ).map(substatus -> new Object[]{CALL_CENTER_OPERATOR_CLIENT_INFO, substatus,
                            isCreateByOrderEditApi})
                    .collect(Collectors.toList()));
        }
        return result.stream().map(Arguments::of);
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
