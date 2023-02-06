package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PACKAGING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.READY_TO_SHIP;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class CallCenterCancellationRequestProcessingTest extends AbstractWebTestBase {

    protected static final String NOTES = "notes";
    private static final ClientInfo CALL_CENTER_OPERATOR_CLIENT_INFO = new ClientInfo(
            ClientRole.CALL_CENTER_OPERATOR,
            123L
    );
    @Autowired
    protected YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    protected CancellationRequestHelper cancellationRequestHelper;
    @Autowired
    protected OrderGetHelper orderGetHelper;

    @Test
    public void createCancellationRequestStarted() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        Order response;
        response = cancellationRequestHelper.createCancellationRequest(
                order.getId(), cancellationRequest, CALL_CENTER_OPERATOR_CLIENT_INFO);
        assertEquals(USER_CHANGED_MIND, response.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, response.getCancellationRequest().getNotes());

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertEquals(USER_CHANGED_MIND, orderFromDB.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());

        order = orderStatusHelper.updateOrderStatus(order.getId(), CALL_CENTER_OPERATOR_CLIENT_INFO, CANCELLED,
                USER_CHANGED_MIND);
        assertEquals(CANCELLED, order.getStatus());
        assertEquals(USER_CHANGED_MIND, order.getSubstatus());
    }

    @Test
    public void createCancellationRequestPackaging() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        orderStatusHelper.updateOrderStatus(order.getId(), PROCESSING, PACKAGING);
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        Order response;
        response = cancellationRequestHelper.createCancellationRequest(
                order.getId(), cancellationRequest, CALL_CENTER_OPERATOR_CLIENT_INFO);

        assertEquals(USER_CHANGED_MIND, response.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, response.getCancellationRequest().getNotes());

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertEquals(USER_CHANGED_MIND, orderFromDB.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());

        order = orderStatusHelper.updateOrderStatus(order.getId(), CALL_CENTER_OPERATOR_CLIENT_INFO, CANCELLED,
                USER_CHANGED_MIND);
        assertEquals(CANCELLED, order.getStatus());
        assertEquals(USER_CHANGED_MIND, order.getSubstatus());
    }

    @Test
    public void createCancellationRequestReadyToShip() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        orderStatusHelper.updateOrderStatus(order.getId(), PROCESSING, READY_TO_SHIP);
        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, NOTES);
        Order response;
        response = cancellationRequestHelper.createCancellationRequest(
                order.getId(), cancellationRequest, CALL_CENTER_OPERATOR_CLIENT_INFO);
        assertEquals(USER_CHANGED_MIND, response.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, response.getCancellationRequest().getNotes());

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertEquals(USER_CHANGED_MIND, orderFromDB.getCancellationRequest().getSubstatus());
        assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());

        order = orderStatusHelper.updateOrderStatus(order.getId(), CALL_CENTER_OPERATOR_CLIENT_INFO, CANCELLED,
                USER_CHANGED_MIND);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(USER_CHANGED_MIND, order.getSubstatus());
    }
}
