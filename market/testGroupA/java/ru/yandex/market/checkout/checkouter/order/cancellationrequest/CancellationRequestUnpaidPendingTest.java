package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;


public class CancellationRequestUnpaidPendingTest extends AbstractWebTestBase {

    private static final String NOTES = "notes";

    private static final ClientInfo USER_CLIENT_INFO = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;

    @Test
    public void createCancellationRequestFromUnpaid() throws Exception {
        ClientInfo clientInfo = USER_CLIENT_INFO;
        OrderStatus status = UNPAID;
        OrderSubstatus substatus = USER_CHANGED_MIND;

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.getOrder().getBuyer().setDontCall(false);
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, status);
        assertEquals(status, order.getStatus());
        CancellationRequest cancellationRequest = new CancellationRequest(substatus, NOTES);

        Order response;
        cancellationRequestHelper.createCancellationRequestByEditApi(
                order.getId(), cancellationRequest, clientInfo);
        response = orderGetHelper.getOrder(order.getId(), clientInfo);
        assertEquals(CANCELLED, response.getStatus());
        assertEquals(substatus, response.getSubstatus());
        assertNull(response.getCancellationRequest());

        Order orderFromDB = orderService.getOrder(order.getId());
        assertEquals(substatus, orderFromDB.getCancellationRequest().getSubstatus());
        Assertions.assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());
        assertEquals(CANCELLED, orderFromDB.getStatus());
        assertEquals(substatus, orderFromDB.getSubstatus());
    }

    @Test
    public void createCancellationRequestFromPending() throws Exception {
        ClientInfo clientInfo = USER_CLIENT_INFO;
        OrderStatus status = PENDING;
        OrderSubstatus substatus = USER_CHANGED_MIND;

        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.getOrder().getBuyer().setDontCall(false);
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, status);
        assertEquals(status, order.getStatus());
        CancellationRequest cancellationRequest = new CancellationRequest(substatus, NOTES);

        Order response;
        cancellationRequestHelper.createCancellationRequestByEditApi(
                order.getId(), cancellationRequest, clientInfo);
        response = orderGetHelper.getOrder(order.getId(), clientInfo);
        assertEquals(CANCELLED, response.getStatus());
        assertEquals(substatus, response.getSubstatus());
        assertNull(response.getCancellationRequest());

        Order orderFromDB = orderService.getOrder(order.getId());
        assertEquals(substatus, orderFromDB.getCancellationRequest().getSubstatus());
        Assertions.assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());
        assertEquals(CANCELLED, orderFromDB.getStatus());
        assertEquals(substatus, orderFromDB.getSubstatus());
    }
}
