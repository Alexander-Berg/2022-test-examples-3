package ru.yandex.market.delivery.mdbapp.integration.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.components.notification.event.FailedOrderEvent;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.integration.payload.OrderErrorWrapper;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderErrorHandlerTest {
    public static final int ORDER_ID = 1;

    public static final long PARCEL_ID = 2;

    private OrderErrorHandler orderErrorHandler;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CheckouterServiceClient checkouterServiceClient;

    @Mock
    private ApplicationEventPublisher appEventPublisher;

    @InjectMocks
    private OrderErrorService orderErrorService;

    @Before
    public void before() {
        orderErrorHandler = new OrderErrorHandler(orderErrorService);
    }

    @Test
    public void orderErrorHandlingTest() {
        OrderErrorWrapper orderErrorWrapper = new OrderErrorWrapper(ORDER_ID, PARCEL_ID, "message");

        Order expectedOrder = new Order();
        when(checkouterServiceClient.updateDeliveryServiceParcelStatus(ORDER_ID, PARCEL_ID, ParcelStatus.ERROR))
            .thenReturn(expectedOrder);
        Order order = orderErrorHandler.handleOrderError(orderErrorWrapper);
        verify(appEventPublisher).publishEvent(Mockito.any(FailedOrderEvent.class));
        assertSame("Wrong order from orderErrorHandler", expectedOrder, order);
    }
}
