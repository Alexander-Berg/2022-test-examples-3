package ru.yandex.market.delivery.mdbapp.integration.transformer;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.integration.payload.GetLabel;
import ru.yandex.market.delivery.mdbapp.integration.payload.OrderWrapper;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;

import static org.assertj.core.api.Assertions.assertThat;
import static steps.orderSteps.OrderSteps.getFilledOrder;

public class GetLabelsRequestTransformerTest {

    private static final long ORDER_ID = 1L;
    private static final long PARCEL_ID = 11L;
    private static final long PARCEL_NEW_ID = 22L;
    private static final long DELIVERY_SERVICE_ID = 987L;
    private static final String TRACK_CODE = "123";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private GetLabelsRequestTransformer getLabelsRequestTransformer;

    @Before
    public void setup() {
        getLabelsRequestTransformer = new GetLabelsRequestTransformer();
    }

    @Test
    public void transformSingleShipmentUpdate() {
        List<GetLabel> labels = getLabelsRequestTransformer.transform(getOrderWrapperSingleShipment());

        assertThat(labels.size()).as("Check label request list size").isEqualTo(1);
        assertThat(labels.get(0).getOrderId()).as("Check label request list size").isEqualTo(getOrderId());
    }

    @Test
    public void transformMultiShipmentFullUpdate() {
        List<GetLabel> labels = getLabelsRequestTransformer.transform(getOrderWrapperMultipleShipment());

        assertThat(labels.size()).as("Check label request list size").isEqualTo(2);
        assertThat(labels.get(0).getOrderId()).as("Check label request list size").isEqualTo(getOrderId());
        assertThat(labels.get(1).getOrderId()).as("Check label request list size").isEqualTo(getOrderId());
        assertThat(labels.get(0).getParcelId()).as("Check label request list size").isEqualTo(getParcelId());
        assertThat(labels.get(1).getParcelId()).as("Check label request list size").isEqualTo(getParcelNewId());
    }

    @Test
    public void transformMultiShipmentPartialLabelUpdate() {
        List<GetLabel> labels = getLabelsRequestTransformer.transform(getOrderWrapperMultiplePartialShipment());

        assertThat(labels.size()).as("Check label request list size").isEqualTo(1);
        assertThat(labels.get(0).getOrderId()).as("Check label request list size").isEqualTo(getOrderId());
        assertThat(labels.get(0).getParcelId()).as("Check label request list size").isEqualTo(getParcelNewId());
    }

    @Test(expected = IllegalStateException.class)
    public void transformShipmentWithoutTrackCode() {
        getLabelsRequestTransformer.transform(getOrderWrapperWithoutTrackCode());
    }

    private ResourceId getOrderId() {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId(Long.toString(ORDER_ID))
            .setDeliveryId(TRACK_CODE)
            .setPartnerId(TRACK_CODE)
            .build();
    }

    private ResourceId getParcelId() {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId(Long.toString(PARCEL_ID))
            .setDeliveryId(TRACK_CODE)
            .setPartnerId(TRACK_CODE)
            .build();
    }

    private ResourceId getParcelNewId() {
        return new ResourceId.ResourceIdBuilder()
            .setYandexId(Long.toString(PARCEL_NEW_ID))
            .setDeliveryId(TRACK_CODE)
            .setPartnerId(TRACK_CODE)
            .build();
    }

    private OrderWrapper getOrderWrapperSingleShipment() {
        Order order = getFilledOrder(ORDER_ID);
        order.getDelivery().setDeliveryServiceId(DELIVERY_SERVICE_ID);

        order.getDelivery().getParcels().get(0).setTracks(Collections.singletonList(buildTrack()));
        order.getDelivery().getParcels().get(0).setLabelURL("");
        order.getDelivery().getParcels().get(0).setId(PARCEL_ID);

        return new OrderWrapper(order);
    }

    private OrderWrapper getOrderWrapperWithoutTrackCode() {
        Order order = getFilledOrder(ORDER_ID);
        order.getDelivery().setDeliveryServiceId(DELIVERY_SERVICE_ID);
        Track track = buildTrack();
        track.setTrackCode(null);
        order.getDelivery().getParcels().get(0).setLabelURL("");
        order.getDelivery().getParcels().get(0).setTracks(Collections.singletonList(track));
        return new OrderWrapper(order);
    }

    private OrderWrapper getOrderWrapperMultipleShipment() {
        Order order = getFilledOrder(ORDER_ID);
        order.getDelivery().setDeliveryServiceId(DELIVERY_SERVICE_ID);

        order.getDelivery().getParcels().get(0).setTracks(Collections.singletonList(buildTrack()));
        order.getDelivery().getParcels().get(0).setLabelURL("");
        order.getDelivery().getParcels().get(0).setId(PARCEL_ID);

        Parcel parcel = new Parcel();
        parcel.setTracks(Collections.singletonList(buildTrack()));
        parcel.setLabelURL(null);
        parcel.setId(PARCEL_NEW_ID);
        order.getDelivery().addParcel(parcel);

        return new OrderWrapper(order);
    }

    private OrderWrapper getOrderWrapperMultiplePartialShipment() {
        OrderWrapper orderWrapper = getOrderWrapperMultipleShipment();
        orderWrapper.getOrder().getDelivery().getParcels().get(0).setLabelURL("some_url");

        return orderWrapper;
    }

    private Track buildTrack() {
        Track track = new Track();
        track.setTrackCode(TRACK_CODE);
        track.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        return track;
    }
}
