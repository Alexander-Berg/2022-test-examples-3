package ru.yandex.market.delivery.mdbapp.integration.service;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.request.ParcelPatchRequest;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbclient.model.delivery.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.request.SetOrderDeliveryShipmentLabel;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderDeliveryShipmentLabelUpdaterTest {

    private static final long ORDER_ID = 1;
    private static final long PARCEL_ID = 11;
    private static final String LABEL_URL = "123abc";

    @Rule
    public JUnitSoftAssertions assertions = new JUnitSoftAssertions();

    private ArgumentCaptor<ParcelPatchRequest> requestCaptor;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private OrderDeliveryShipmentLabelUpdater orderDeliveryShipmentLabelUpdater;

    @Mock
    private CheckouterServiceClient checkouterServiceClient;

    @Before
    public void setup() {
        Order order = OrderSteps.getFilledOrder(ORDER_ID);
        order.getDelivery().getParcels().get(0).setId(PARCEL_ID);
        Parcel parcel = new Parcel();
        parcel.setId(PARCEL_ID);

        requestCaptor = ArgumentCaptor.forClass(ParcelPatchRequest.class);

        when(checkouterServiceClient.getFirstParcel(ORDER_ID)).thenReturn(parcel);
        when(checkouterServiceClient.getOrder(eq(ORDER_ID))).thenReturn(order);

        orderDeliveryShipmentLabelUpdater = new OrderDeliveryShipmentLabelUpdater(checkouterServiceClient);
    }

    @Test
    public void updateOrderDeliveryShipmentLabelSingleShipmentSuccess() {
        orderDeliveryShipmentLabelUpdater.updateOrderDeliveryShipmentLabel(getSingleShipmentLabel());
        verify(checkouterServiceClient, times(1)).getFirstParcel(eq(ORDER_ID));

        verify(checkouterServiceClient, times(1))
            .updateParcel(eq(ORDER_ID), eq(PARCEL_ID), requestCaptor.capture());

        assertions.assertThat(requestCaptor.getValue())
            .as("ParcelPatchRequest should have correct labelUrl")
            .extracting(ParcelPatchRequest::getLabelUrl)
            .isEqualTo(LABEL_URL);
    }

    @Test
    public void updateOrderDeliveryShipmentLabelParcelShipmentSuccess() {
        orderDeliveryShipmentLabelUpdater.updateOrderDeliveryShipmentLabel(getParcelShipmentLabel());

        verify(checkouterServiceClient, times(0)).getFirstParcel(eq(ORDER_ID));
        verify(checkouterServiceClient, times(1))
            .updateParcel(eq(ORDER_ID), eq(PARCEL_ID), requestCaptor.capture());

        assertions.assertThat(requestCaptor.getValue())
            .as("ParcelPatchRequest should have correct labelUrl")
            .extracting(ParcelPatchRequest::getLabelUrl)
            .isEqualTo(LABEL_URL);
    }

    private SetOrderDeliveryShipmentLabel getSingleShipmentLabel() {
        return new SetOrderDeliveryShipmentLabel(ORDER_ID, null, LABEL_URL);
    }

    private SetOrderDeliveryShipmentLabel getParcelShipmentLabel() {
        return new SetOrderDeliveryShipmentLabel(ORDER_ID, getParcelId(), LABEL_URL);
    }

    private ResourceId getParcelId() {
        return new ResourceId(Long.toString(PARCEL_ID), null);
    }

}
