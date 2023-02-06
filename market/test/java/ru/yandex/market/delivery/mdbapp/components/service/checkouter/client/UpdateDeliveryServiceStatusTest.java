package ru.yandex.market.delivery.mdbapp.components.service.checkouter.client;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Matchers;
import org.mockito.Mockito;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.util.MdbArgumentMatcher;
import ru.yandex.market.logistics.util.client.tvm.client.MockTvmClient;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientWrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.argThat;

@RunWith(Parameterized.class)
public class UpdateDeliveryServiceStatusTest {
    private static final long ORDER_ID = 888L;
    private static final long PARCEL_ID = 777L;
    private final CheckouterClient checkouterClientMock = Mockito.mock(CheckouterClient.class);
    @Parameterized.Parameter()
    public ParcelStatus status;
    @Parameterized.Parameter(1)
    public int numberOfParcels;
    private CheckouterServiceClient checkouterServiceClient;
    private Order expectedOrder;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> getParameters() {
        Collection<Object[]> parameters = new ArrayList<>();

        parameters.add(new Object[]{ParcelStatus.NEW, 1});
        parameters.add(new Object[]{ParcelStatus.NEW, 2});
        parameters.add(new Object[]{ParcelStatus.CREATED, 1});
        parameters.add(new Object[]{ParcelStatus.CREATED, 2});
        parameters.add(new Object[]{ParcelStatus.ERROR, 1});
        parameters.add(new Object[]{ParcelStatus.ERROR, 2});
        parameters.add(new Object[]{ParcelStatus.UNKNOWN, 1});
        parameters.add(new Object[]{ParcelStatus.UNKNOWN, 2});

        return parameters;
    }

    @Before
    public void before() {
        checkouterServiceClient = new CheckouterServiceClient(
            checkouterClientMock,
            new TvmClientWrapper(
                new MockTvmClient()),
            2010068,
            true
        );
        expectedOrder = OrderSteps.getFilledOrder(numberOfParcels);
        Order currentOrder = OrderSteps.getFilledOrder(numberOfParcels);
        expectedOrder.getDelivery().getParcels().get(0).setId(PARCEL_ID);
        expectedOrder.getDelivery().getParcels().get(0).setStatus(status);
        currentOrder.getDelivery().getParcels().get(0).setId(PARCEL_ID);
        for (int i = 1; i < numberOfParcels; i++) {
            expectedOrder.getDelivery().getParcels().get(i).setId(PARCEL_ID + i);
            expectedOrder.getDelivery().getParcels().get(i).setStatus(status);
            currentOrder.getDelivery().getParcels().get(i).setId(PARCEL_ID + i);
        }

        Mockito.when(checkouterClientMock.updateOrderDelivery(
            anyLong(),
            any(ClientRole.class),
            anyObject(),
            argThat(MdbArgumentMatcher.matcher(delivery -> {
                for (int i = 0; i < numberOfParcels; i++) {
                    if (delivery.getParcels().get(i).getId() != PARCEL_ID + i) {
                        return false;
                    }
                }
                return true;
            }))
        )).thenReturn(expectedOrder);

        Mockito.when(checkouterClientMock.getOrder(
            Matchers.anyLong(),
            Matchers.any(ClientRole.class),
            Matchers.anyObject()
        )).thenReturn(currentOrder);
    }

    @Test
    public void updateDeliveryServiceStatusTest() {
        Order actualOrder;
        if (numberOfParcels == 1) {
            actualOrder = checkouterServiceClient.updateDeliveryServiceStatusForSingleParcelOrder(ORDER_ID, status);
        } else {
            actualOrder = checkouterServiceClient.updateDeliveryServiceParcelStatus(ORDER_ID, PARCEL_ID, status);
        }

        Assert.assertNotNull("Empty order after updateDeliveryServiceParcelStatus", actualOrder);
        Assert.assertEquals("Unexpected order after updateDeliveryServiceParcelStatus", expectedOrder, actualOrder);
    }
}
