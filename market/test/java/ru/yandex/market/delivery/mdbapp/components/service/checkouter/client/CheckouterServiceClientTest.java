package ru.yandex.market.delivery.mdbapp.components.service.checkouter.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.util.ReflectionTestUtils;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.delivery.parcel.CancellationRequestStatus;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.logistics.util.client.tvm.client.MockTvmClient;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientWrapper;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.delivery.parcel.CancellationRequestStatus.CONFIRMED;

public class CheckouterServiceClientTest {
    private static final int BATCH_SIZE = 5;
    private static final long ORDER_ID = 999L;
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    private CheckouterServiceClient checkouterServiceClient;
    private CheckouterAPI checkouterClientMock;
    private CheckouterOrderHistoryEventsApi checkouterEventsAPIMock;

    @Before
    public void before() {
        checkouterClientMock = mock(CheckouterAPI.class);
        checkouterEventsAPIMock = mock(CheckouterOrderHistoryEventsApi.class);
        when(checkouterClientMock.orderHistoryEvents()).thenReturn(checkouterEventsAPIMock);
        checkouterServiceClient = new CheckouterServiceClient(
            checkouterClientMock,
            new TvmClientWrapper(new MockTvmClient()),
            2010068,
            true

        );
    }

    @Test
    public void batchSizeTest() {
        ReflectionTestUtils.setField(checkouterServiceClient, "batchSize", BATCH_SIZE);
        Mockito.when(checkouterEventsAPIMock.getOrderHistoryEvents(
            anyLong(),
            anyInt(),
            anySet(),
            anyBoolean(),
            nullable(Set.class),
            refEq(expectedFilter()),
            anySet()
        )).thenReturn(getOrderHistoryEvents());

        OrderHistoryEvents orderHistoryEvents = checkouterServiceClient.getOrderHistoryEvents(1L);

        Assert.assertNotNull("Empty orderHistoryEvent from CheckouterServiceClient", orderHistoryEvents);
        Assert.assertEquals(
            "Wrong batch size from CheckouterServiceClient",
            BATCH_SIZE,
            orderHistoryEvents.getContent().size()
        );
    }

    @Test
    public void emptyOrderShipmentTest() {
        Mockito.when(checkouterClientMock.getOrder(
            ORDER_ID,
            ClientRole.SYSTEM,
            null
        )).thenReturn(new Order());

        List<Parcel> parcel = checkouterServiceClient.getOrderParcels(ORDER_ID);

        Assert.assertEquals("Empty orderShipment from CheckouterServiceClient", parcel.size(), 0);
    }

    @Test
    public void filledOrderShipmentTest() {
        Order order = OrderSteps.getFilledOrder(ORDER_ID);

        Mockito.when(checkouterClientMock.getOrder(
            ORDER_ID,
            ClientRole.SYSTEM,
            null
        )).thenReturn(order);

        List<Parcel> parcels = checkouterServiceClient.getOrderParcels(ORDER_ID);

        Assert.assertNotNull("Empty orderShipment from CheckouterServiceClient", parcels);
        Assert.assertEquals(
            "Wrong shipment from CheckouterServiceClient",
            order.getDelivery().getParcels().get(0),
            parcels.get(0)
        );
    }

    @Test
    public void cancelParcelTest() {
        Mockito.when(checkouterClientMock.updateParcelCancellationRequestStatus(
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.any(CancellationRequestStatus.class),
                ArgumentMatchers.eq(ClientRole.SYSTEM),
                Mockito.any()
            ))
            .thenReturn(new Parcel());

        Parcel parcel = checkouterServiceClient.cancelParcel(1L, 12L, CONFIRMED);

        Assert.assertNotNull("Empty parcel after cancellation confirmation", parcel);
    }

    private OrderHistoryEvents getOrderHistoryEvents() {
        List<OrderHistoryEvent> eventsList = new ArrayList<>();
        for (int i = 0; i < BATCH_SIZE; i++) {
            eventsList.add(new OrderHistoryEvent());
        }
        return new OrderHistoryEvents(eventsList);
    }

    @Nonnull
    private OrderFilter expectedFilter() {
        OrderFilter filter = new OrderFilter();
        filter.setRgb(new Color[]{Color.BLUE, Color.RED, Color.WHITE});
        return filter;
    }
}
