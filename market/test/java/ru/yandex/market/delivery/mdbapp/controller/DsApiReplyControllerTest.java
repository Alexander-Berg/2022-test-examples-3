package ru.yandex.market.delivery.mdbapp.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.dsmclient.payload.Task;
import ru.yandex.market.delivery.mdbapp.components.curator.managers.OrderEventManager;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.components.service.lgw.DsApiReplyLgwService;
import ru.yandex.market.delivery.mdbapp.exception.EntityNotFoundException;
import ru.yandex.market.delivery.mdbapp.exception.InvalidEntityException;
import ru.yandex.market.delivery.mdbapp.integration.gateway.DsmGateway;
import ru.yandex.market.delivery.mdbapp.integration.gateway.OrderEventsGateway;
import ru.yandex.market.delivery.mdbapp.integration.payload.OrderErrorWrapper;
import ru.yandex.market.delivery.mdbclient.model.request.CreateLgwDsOrder;
import ru.yandex.market.delivery.mdbclient.model.request.CreateLgwOrderError;
import ru.yandex.market.delivery.mdbclient.model.request.SetOrderDeliveryShipmentLabel;
import ru.yandex.market.logistics.util.client.tvm.client.MockTvmClient;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientWrapper;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DsApiReplyControllerTest {
    private static final long ID = 2L;
    private static final long PARCEL_ID = 3L;

    private CheckouterOrderHistoryEventsApi checkouterEventsAPIMock;
    private DsmGateway dsmGatewayMock;
    private OrderEventsGateway orderEventsGateway;
    private OrderEventManager orderEventManager;

    private CheckouterServiceClient checkouterServiceClient;
    private HealthManager healthManager;

    private DsApiReplyController dsmApiController;
    private DsApiReplyLgwService dsApiReplyLgwServiceMock;

    @Before
    public void setUp() {
        CheckouterAPI checkouterAPIMock = mock(CheckouterAPI.class);
        checkouterEventsAPIMock = mock(CheckouterOrderHistoryEventsApi.class);
        when(checkouterAPIMock.orderHistoryEvents()).thenReturn(checkouterEventsAPIMock);
        dsmGatewayMock = mock(DsmGateway.class);
        orderEventsGateway = mock(OrderEventsGateway.class);
        orderEventManager = mock(OrderEventManager.class);
        dsApiReplyLgwServiceMock = mock(DsApiReplyLgwService.class);

        healthManager = mock(HealthManager.class);

        checkouterServiceClient = new CheckouterServiceClient(
            checkouterAPIMock,
            new TvmClientWrapper(new MockTvmClient()),
            2010068,
            true
        );

        dsmApiController = new DsApiReplyController(
            dsmGatewayMock,
            null,
            healthManager,
            null,
            null,
            null,
            orderEventsGateway,
            checkouterServiceClient,
            Collections.singletonList(orderEventManager),
            Collections.emptyList(),
            dsApiReplyLgwServiceMock
        );
    }

    @After
    public void after() {
        verifyNoMoreInteractions(dsApiReplyLgwServiceMock);
    }

    @Test
    public void setOrderLabelTest() {
        SetOrderDeliveryShipmentLabel setOrderDeliveryShipmentLabel =
            new SetOrderDeliveryShipmentLabel(ID, null, "url");
        Order expectedOrder = new Order();
        when(dsmGatewayMock.setChannel(any(SetOrderDeliveryShipmentLabel.class))).thenReturn(expectedOrder);
        Order order = dsmApiController.setOrderLabel(ID, setOrderDeliveryShipmentLabel);

        assertSame("Wrong order from dsmApiController", expectedOrder, order);
    }

    @Test(expected = Exception.class)
    public void failSetOrderLabelTest() {
        SetOrderDeliveryShipmentLabel setOrderDeliveryShipmentLabel =
            new SetOrderDeliveryShipmentLabel(ID, null, "url");
        when(dsmGatewayMock.setChannel(any(SetOrderDeliveryShipmentLabel.class)))
            .thenThrow(new Exception());
        dsmApiController.setOrderLabel(ID, setOrderDeliveryShipmentLabel);
    }

    @Test
    public void setOrderErrorTest() {
        Order expectedOrder = new Order();
        when(dsmGatewayMock.setChannel(any(OrderErrorWrapper.class))).thenReturn(expectedOrder);
        Order order = dsmApiController.setOrderError(ID);

        assertSame("Wrong order from dsmApiController", expectedOrder, order);
    }

    @Test(expected = Exception.class)
    public void failSetOrderErrorTest() {
        when(checkouterServiceClient.updateDeliveryServiceStatusForSingleParcelOrder(eq(ID), any(ParcelStatus.class)))
            .thenThrow(new Exception());
        dsmApiController.setOrderError(ID);
    }

    @Test(expected = Exception.class)
    public void failSetLgwOrderErrorTest() {
        when(checkouterServiceClient.updateDeliveryServiceParcelStatus(eq(ID), eq(PARCEL_ID), any(ParcelStatus.class)))
            .thenThrow(new Exception());
        dsmApiController.setOrderParcelError(ID, new CreateLgwOrderError(ID, PARCEL_ID));
    }

    @Test
    public void processEventSetTaskTest() throws Exception {
        when(orderEventManager.getId()).thenReturn(Long.MAX_VALUE);
        when(orderEventManager.getEventManagerId()).thenReturn(1L);

        Order orderAfter = new Order();
        orderAfter.setId(1L);

        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderAfter(orderAfter);

        OrderHistoryEvents orderHistoryEvents = new OrderHistoryEvents(Collections.singletonList(orderHistoryEvent));
        whenGetOrderHistoryEventsIsCalled().thenReturn(orderHistoryEvents);

        Task expectedTask = new Task();
        when(orderEventsGateway.pushEvent(any(OrderHistoryEvent.class)))
            .thenReturn(expectedTask);

        Task task = dsmApiController.processEvent(ID);

        assertSame("Wrong task from dsmApiController", expectedTask, task);
    }

    @Test(expected = InvalidEntityException.class)
    public void emptyProcessEventTest() throws Exception {
        when(orderEventManager.getId()).thenReturn(Long.MAX_VALUE);
        when(orderEventManager.getEventManagerId()).thenReturn(1L);

        Order orderAfter = new Order();
        orderAfter.setId(1L);

        OrderHistoryEvent e = new OrderHistoryEvent();
        e.setOrderAfter(orderAfter);

        Collection<OrderHistoryEvent> orderHistoryEventList = new ArrayList<>();
        orderHistoryEventList.add(e);
        OrderHistoryEvents orderHistoryEvents = new OrderHistoryEvents(orderHistoryEventList);
        whenGetOrderHistoryEventsIsCalled().thenReturn(orderHistoryEvents);

        dsmApiController.processEvent(ID);
    }

    @Test(expected = EntityNotFoundException.class)
    public void processEventNotFoundTest() throws Exception {
        when(orderEventManager.getId()).thenReturn(Long.MAX_VALUE);
        whenGetOrderHistoryEventsIsCalled().thenReturn(new OrderHistoryEvents());

        dsmApiController.processEvent(ID);
    }

    @Test(expected = InvalidEntityException.class)
    public void processNotFailedEventTest() throws Exception {
        when(orderEventManager.getId()).thenReturn(Long.MIN_VALUE);
        when(orderEventManager.getEventManagerId()).thenReturn(1L);

        Order orderAfter = new Order();
        orderAfter.setId(1L);

        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderAfter(orderAfter);

        OrderHistoryEvents orderHistoryEvents = new OrderHistoryEvents(Collections.singletonList(orderHistoryEvent));

        whenGetOrderHistoryEventsIsCalled().thenReturn(orderHistoryEvents);

        dsmApiController.processEvent(ID);
    }

    @Test(expected = Exception.class)
    public void processEventFailSetTaskTest() throws Exception {
        when(orderEventManager.getId()).thenReturn(Long.MAX_VALUE);
        when(orderEventManager.getEventManagerId()).thenReturn(1L);

        Order orderAfter = new Order();
        orderAfter.setId(1L);

        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderAfter(orderAfter);

        OrderHistoryEvents orderHistoryEvents = new OrderHistoryEvents(Collections.singletonList(orderHistoryEvent));

        whenGetOrderHistoryEventsIsCalled().thenReturn(orderHistoryEvents);
        when(orderEventsGateway.pushEvent(any(OrderHistoryEvent.class)))
            .thenThrow(new Exception());

        dsmApiController.processEvent(ID);
    }

    @Test
    public void setLgwCreateDropshipOrderSuccess() {
        long orderId = 1L;
        CreateLgwDsOrder createLgwDsOrder = new CreateLgwDsOrder("1", "2", "3");

        dsmApiController.setLgwCreateDropshipOrderSuccess(orderId, createLgwDsOrder);
        verify(dsApiReplyLgwServiceMock).setLgwCreateDropshipOrderSuccess(orderId, createLgwDsOrder);
    }

    @Test
    public void setOrderParcelError() {
        long orderId = 1L;
        CreateLgwOrderError orderError = new CreateLgwOrderError(orderId, 2L);

        dsmApiController.setOrderParcelError(orderId, orderError);
        verify(dsApiReplyLgwServiceMock).setOrderParcelError(orderId, orderError);
    }

    private OngoingStubbing<OrderHistoryEvents> whenGetOrderHistoryEventsIsCalled() {
        return when(checkouterEventsAPIMock.getOrderHistoryEvents(
            anyLong(),
            anyInt(),
            anySet(),
            anyBoolean(),
            nullable(Set.class),
            any(OrderFilter.class),
            any(Set.class)
        ));
    }
}
