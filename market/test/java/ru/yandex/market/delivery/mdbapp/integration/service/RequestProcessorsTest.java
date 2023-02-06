package ru.yandex.market.delivery.mdbapp.integration.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import steps.RequestsSteps;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.request.ParcelPatchRequest;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.request.CreateOrder;
import ru.yandex.market.delivery.mdbclient.model.request.SetOrderDeliveryShipmentLabel;
import ru.yandex.market.logistics.util.client.tvm.client.MockTvmClient;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientWrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestProcessorsTest {
    private CheckouterClient checkouterClientMock;
    private CreateOrder createOrderRequest;
    private OrderDeliveryShipmentLabelUpdater orderDeliveryShipmentLabelUpdater;

    @Before
    public void before() throws Exception {
        checkouterClientMock = Mockito.mock(CheckouterClient.class);
        createOrderRequest = RequestsSteps.getCreateOrderRequest();
        CheckouterServiceClient serviceClient =
            new CheckouterServiceClient(
                checkouterClientMock,
                new TvmClientWrapper(new MockTvmClient()),
                2010068,
                true
            );
        orderDeliveryShipmentLabelUpdater = new OrderDeliveryShipmentLabelUpdater(serviceClient);
    }

    @Test
    public void updateOrderDeliveryShipmentLabelTest() {
        SetOrderDeliveryShipmentLabel request = RequestsSteps.getSetOrderDeliveryShipmentLabelRequest();
        Order order = OrderSteps.getFilledOrder(createOrderRequest.getOrderId());

        when(checkouterClientMock.getOrder(request.getOrderId(), ClientRole.SYSTEM, null))
            .thenReturn(order);

        orderDeliveryShipmentLabelUpdater.updateOrderDeliveryShipmentLabel(request);

        verify(checkouterClientMock, times(1))
            .updateParcel(
                eq(request.getOrderId()),
                eq(order.getDelivery().getParcels().get(0).getId()),
                any(ParcelPatchRequest.class),
                eq(ClientRole.SYSTEM),
                isNull()
            );
    }
}
