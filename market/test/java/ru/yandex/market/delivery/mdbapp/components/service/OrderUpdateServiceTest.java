package ru.yandex.market.delivery.mdbapp.components.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Iterables;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.components.util.DeliveryServices;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.integration.converter.ParcelBoxConverter;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.Place;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrderResult;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class OrderUpdateServiceTest {
    private static final String PARTNER_ID = "exactPartnerId";
    private static final Long ORDER_ID = 123456L;
    private static final String STRING_ORDER_ID = String.valueOf(ORDER_ID);
    private static final Long PARCEL_ID = 321123L;

    private CheckouterServiceClient checkouterServiceClient = mock(CheckouterServiceClient.class);
    private ParcelBoxConverter parcelBoxConverter = new ParcelBoxConverter();
    private FeatureProperties featureProperties = new FeatureProperties();
    private OrderUpdateService orderUpdateService = new OrderUpdateService(
        checkouterServiceClient,
        parcelBoxConverter,
        featureProperties
    );

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Parameterized.Parameter()
    public Long marketDeliveryServiceId;

    @Parameterized.Parameters()
    public static Collection<Object[]> getParameters() {
        Collection<Object[]> parameters = new ArrayList<>();

        parameters.add(new Object[]{DeliveryServices.MARSCHROUTE_POST_DELIVERY_SERVICE_ID});
        parameters.add(new Object[]{DeliveryServices.MARSCHROUTE_NEW_POST_DELIVERY_SERVICE_ID});

        return parameters;
    }

    @Before
    public void setUp() {
        featureProperties.setLgwGetOrderEventHandlingDisabled(true);
    }

    @Test
    public void putParcelBoxes() {
        when(checkouterServiceClient.getOrder(ORDER_ID)).thenReturn(getCheckouterOrderWithPostTrack());
        orderUpdateService.putParcelBoxes(getPostOrder());
        verify(checkouterServiceClient).putParcelBoxes(
            Matchers.eq(ORDER_ID),
            Matchers.eq(PARCEL_ID),
            argThat(ParcelsMatcher.hasElementWithExactPartnerId())
        );
    }

    @Test
    public void putParcelBoxesWithNullPlaces() {
        when(checkouterServiceClient.getOrder(ORDER_ID)).thenReturn(getCheckouterOrderWithPostTrack());
        orderUpdateService.putParcelBoxes(getPostOrderWithNullPlaces());
        verify(checkouterServiceClient).putParcelBoxes(
            Matchers.eq(ORDER_ID),
            Matchers.eq(PARCEL_ID),
            Matchers.eq(List.of())
        );
    }

    @Test
    public void putParcelBoxesWhenPutParcelBoxesThrowsOrderStatusNotAllowedException() {
        when(checkouterServiceClient.getOrder(ORDER_ID))
            .thenReturn(getCheckouterOrderWithPostTrack());
        doThrow(new OrderStatusNotAllowedException("code", "message", 400))
            .when(checkouterServiceClient).putParcelBoxes(anyLong(), anyLong(), any());

        orderUpdateService.putParcelBoxes(getPostOrder());

        verify(checkouterServiceClient).putParcelBoxes(anyLong(), anyLong(), any());
        verify(checkouterServiceClient, never()).putTrack(anyLong(), anyLong(), any());
    }

    @Test
    public void putParcelBoxesWhenPutTrackThrowsOrderStatusNotAllowedException() {
        when(checkouterServiceClient.getOrder(ORDER_ID))
            .thenReturn(getCheckouterOrder());
        doThrow(new OrderStatusNotAllowedException("code", "message", 400))
            .when(checkouterServiceClient).putTrack(anyLong(), anyLong(), any());

        orderUpdateService.putParcelBoxes(getPostOrder());

        verify(checkouterServiceClient).putParcelBoxes(anyLong(), anyLong(), any());
        verify(checkouterServiceClient).putTrack(anyLong(), anyLong(), any());
    }

    @Test
    public void putTrackForPostOrder() {
        when(checkouterServiceClient.getOrder(ORDER_ID)).thenReturn(getCheckouterOrder());
        orderUpdateService.putParcelBoxes(getPostOrder());
        ArgumentCaptor<Track> trackArgument = ArgumentCaptor.forClass(Track.class);
        verify(checkouterServiceClient).putTrack(Matchers.eq(ORDER_ID),
            Matchers.eq(PARCEL_ID),
            trackArgument.capture());
        softly.assertThat(trackArgument.getValue().getDeliveryServiceId())
            .isEqualTo(DeliveryServices.EMS_OPTIMA_DELIVERY_SERVICE_ID);
    }

    @Test
    public void doesNotPutTrackForPostOrderWhenTrackAlreadyExist() {
        when(checkouterServiceClient.getOrder(ORDER_ID)).thenReturn(getCheckouterOrderWithPostTrack());
        orderUpdateService.putParcelBoxes(getPostOrder());
        verify(checkouterServiceClient, never()).putTrack(any(), any(), any());
    }

    private ru.yandex.market.checkout.checkouter.order.Order getCheckouterOrder() {
        ru.yandex.market.checkout.checkouter.order.Order order =
            new ru.yandex.market.checkout.checkouter.order.Order();
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(marketDeliveryServiceId);
        LinkedList<Parcel> parcels = new LinkedList<>();
        Parcel parcel = new Parcel();
        parcel.setId(PARCEL_ID);
        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(123L);
        parcelBox.setFulfilmentId("exactPartnerId");
        parcel.setBoxes(Collections.singletonList(parcelBox));
        parcels.add(parcel);
        delivery.setParcels(parcels);
        order.setDelivery(delivery);
        return order;
    }

    private ru.yandex.market.checkout.checkouter.order.Order getCheckouterOrderWithPostTrack() {
        ru.yandex.market.checkout.checkouter.order.Order order =
            getCheckouterOrder();
        Track newTrack = new Track();
        newTrack.setTrackCode(PARTNER_ID);
        newTrack.setDeliveryServiceId(DeliveryServices.EMS_OPTIMA_DELIVERY_SERVICE_ID);
        Iterables.getOnlyElement(order.getDelivery().getParcels()).setTracks(
            new ArrayList<Track>() {{
                add(newTrack);
            }}
        );
        return order;
    }

    private GetOrderResult getPostOrder() {
        return new GetOrderResult(
            STRING_ORDER_ID,
            DeliveryServices.EMS_OPTIMA_DELIVERY_SERVICE_ID,
            PARTNER_ID,
            Collections.singletonList(getPlace())
        );
    }

    private GetOrderResult getPostOrderWithNullPlaces() {
        return new GetOrderResult(
            STRING_ORDER_ID,
            DeliveryServices.EMS_OPTIMA_DELIVERY_SERVICE_ID,
            PARTNER_ID,
            null
        );
    }

    private Place getPlace() {
        return new Place(new ResourceId("yaId", PARTNER_ID), null, null);
    }

    public static class ParcelsMatcher {
        static ArgumentMatcher<List<ParcelBox>> hasElementWithExactPartnerId() {
            return argument -> argument.get(0).getFulfilmentId().equals(OrderUpdateServiceTest.PARTNER_ID);
        }
    }

    public static class DeliveryMatcher {
        private static final ParcelStatus EXPECTED_STATUS = ParcelStatus.READY_TO_SHIP;

        static ArgumentMatcher<Delivery> hasReadyToShipParcelStatus() {
            return argument -> argument.getParcels().get(0).getStatus().equals(EXPECTED_STATUS);
        }
    }
}
