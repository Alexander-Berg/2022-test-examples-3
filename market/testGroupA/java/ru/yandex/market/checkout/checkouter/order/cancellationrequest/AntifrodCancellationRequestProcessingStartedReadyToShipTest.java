package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.READY_TO_SHIP;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class AntifrodCancellationRequestProcessingStartedReadyToShipTest
        extends AbstractAntifrodCancellationRequestProcessingTest {

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void createCancellationRequestReadyToShip(ClientInfo clientInfo, OrderSubstatus substatus,
                                                     boolean isCreateByOrderEditApi) throws Exception {
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
        CancellationRequest cancellationRequest = new CancellationRequest(substatus, NOTES);
        Order response;
        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);
            response = orderGetHelper.getOrder(order.getId(), clientInfo);
        } else {
            response = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);
        }
        assertEquals(substatus, response.getCancellationRequest().getSubstatus());
        Assertions.assertEquals(NOTES, response.getCancellationRequest().getNotes());

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertEquals(substatus, orderFromDB.getCancellationRequest().getSubstatus());
        Assertions.assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());

        order = orderStatusHelper.updateOrderStatus(order.getId(), clientInfo, CANCELLED, substatus);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(substatus, order.getSubstatus());
    }
}
