package ru.yandex.market.delivery.mdbapp.integration.service;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbclient.model.delivery.ItemPlace;
import ru.yandex.market.delivery.mdbclient.model.delivery.Korobyte;
import ru.yandex.market.delivery.mdbclient.model.delivery.Place;
import ru.yandex.market.delivery.mdbclient.model.delivery.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.delivery.UnitId;
import ru.yandex.market.delivery.mdbclient.model.request.UpdateLgwOrder;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrderPlacesToParcelsServiceTest {

    private static final long ORDER_ID = 1000L;

    private static final long DELIVERY_SERVICE_ID = 100L;

    @Mock
    private CheckouterServiceClient checkouterClient;

    @InjectMocks
    private OrderPlacesToParcelsService orderPlacesToParcelsService;

    ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);

    @Test
    public void testSplitOrderPlacesToParcelsOneParcelSuccess() {
        when(checkouterClient.getOrder(anyLong())).thenReturn(getCheckouterOrder());

        orderPlacesToParcelsService.splitOrderPlacesToParcels(getOneParcelUpdateOrder());

        verify(checkouterClient).updateOrderDelivery(eq(ORDER_ID), deliveryCaptor.capture());

        Delivery capturedDelivery = deliveryCaptor.getValue();

        assertThat(capturedDelivery.getParcels().size())
            .as("Check parcel count")
            .isEqualTo(1);

        assertThat(capturedDelivery.getParcels().get(0).getId())
            .as("Check default parcel id")
            .isEqualTo(123L);

        assertThat(capturedDelivery.getParcels().get(0).getTracks().size())
            .as("Check tracks count")
            .isEqualTo(2);

        assertThat(capturedDelivery.getParcels().get(0).getTracks().get(0).getTrackCode())
            .as("Check trackCode")
            .isEqualTo("track_code");

        assertThat(capturedDelivery.getParcels().get(0).getParcelItems().size())
            .as("Check parcelItems size")
            .isEqualTo(3);

        assertThat(capturedDelivery.getParcels().get(0).getParcelItems().get(1).getItemId())
            .as("Check parcelItems size")
            .isEqualTo(1223L);

        assertThat(capturedDelivery.getParcels().get(0).getParcelItems().get(1).getCount())
            .as("Check parcelItems size")
            .isEqualTo(2);
    }

    @Test
    public void testSplitOrderPlacesToParcelsMultiParcelSuccess() {
        when(checkouterClient.getOrder(anyLong())).thenReturn(getCheckouterOrder());

        orderPlacesToParcelsService.splitOrderPlacesToParcels(getMultiParcelUpdateOrder());

        verify(checkouterClient).updateOrderDelivery(eq(ORDER_ID), deliveryCaptor.capture());

        Delivery capturedDelivery = deliveryCaptor.getValue();

        assertThat(capturedDelivery.getParcels().size())
            .as("Check parcel count")
            .isEqualTo(2);

        assertThat(capturedDelivery.getParcels().get(0).getId())
            .as("Check default parcel id")
            .isEqualTo(123L);

        assertThat(capturedDelivery.getParcels().get(0).getTracks().size())
            .as("Check tracks count")
            .isEqualTo(2);

        assertThat(capturedDelivery.getParcels().get(1).getId())
            .as("Check new parcel id")
            .isNull();

        assertThat(capturedDelivery.getParcels().get(1).getTracks().size())
            .as("Check tracks count 2")
            .isEqualTo(1);

        assertThat(capturedDelivery.getParcels().get(0).getTracks().get(0).getTrackCode())
            .as("Check trackCode")
            .isEqualTo("track_code");

        assertThat(capturedDelivery.getParcels().get(1).getTracks().get(0).getTrackCode())
            .as("Check trackCode 2")
            .isEqualTo("202");

        assertThat(capturedDelivery.getParcels().get(0).getParcelItems().size())
            .as("Check parcelItems size")
            .isEqualTo(2);

        assertThat(capturedDelivery.getParcels().get(1).getParcelItems().size())
            .as("Check parcelItems size 2")
            .isEqualTo(3);

        // check items

        assertThat(capturedDelivery.getParcels().get(0).getParcelItems().get(0).getItemId())
            .as("Check parcelItem")
            .isEqualTo(1223L);

        assertThat(capturedDelivery.getParcels().get(0).getParcelItems().get(0).getCount())
            .as("Check parcelItem")
            .isEqualTo(1);

        assertThat(capturedDelivery.getParcels().get(0).getParcelItems().get(1).getItemId())
            .as("Check parcelItem")
            .isEqualTo(1323L);

        assertThat(capturedDelivery.getParcels().get(0).getParcelItems().get(1).getCount())
            .as("Check parcelItem")
            .isEqualTo(1);



        assertThat(capturedDelivery.getParcels().get(1).getParcelItems().get(0).getItemId())
            .as("Check parcelItem")
            .isEqualTo(1123L);

        assertThat(capturedDelivery.getParcels().get(1).getParcelItems().get(0).getCount())
            .as("Check parcelItems size")
            .isEqualTo(1);
    }

    @Test(expected = RuntimeException.class)
    public void testSplitOrderPlacesToParcelsEmptyPlacesFailed() {
        when(checkouterClient.getOrder(anyLong())).thenReturn(getCheckouterOrder());

        orderPlacesToParcelsService.splitOrderPlacesToParcels(getEmptyPlacesUpdateOrder());
    }

    @Test(expected = RuntimeException.class)
    public void testSplitOrderPlacesToParcelsOrderNotFoundFailed() {
        when(checkouterClient.getOrder(anyLong())).thenReturn(null);

        orderPlacesToParcelsService.splitOrderPlacesToParcels(getMultiParcelUpdateOrder());
    }

    @Test(expected = RuntimeException.class)
    public void testSplitOrderPlacesToParcelsChecksumFailed() {
        when(checkouterClient.getOrder(anyLong())).thenReturn(getCheckouterOrder());

        orderPlacesToParcelsService.splitOrderPlacesToParcels(getMultiParcelUpdateOrderWithWrongChecksum());

        verify(checkouterClient).updateOrderDelivery(eq(ORDER_ID), deliveryCaptor.capture());
    }

    private UpdateLgwOrder getEmptyPlacesUpdateOrder() {
        ResourceId orderId = new ResourceId(String.valueOf(ORDER_ID), null);

        return new UpdateLgwOrder(orderId, null);
    }

    private UpdateLgwOrder getOneParcelUpdateOrder() {
        ResourceId orderId = new ResourceId(String.valueOf(ORDER_ID), null);

        ResourceId placeId = new ResourceId(null, String.valueOf(101));

        Korobyte korobyte = new Korobyte.KorobyteBuilder().build();
        Place place = new Place.PlaceBuilder(placeId, korobyte)
            .setItemPlaces(Arrays.asList(
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "123").build(), 1),
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "223").build(), 2),
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "323").build(), 3)))
            .build();

        return new UpdateLgwOrder(orderId, Collections.singletonList(place));
    }

    private UpdateLgwOrder getMultiParcelUpdateOrderWithWrongChecksum() {
        ResourceId orderId = new ResourceId(String.valueOf(ORDER_ID), null);

        ResourceId placeId1 = new ResourceId(null, String.valueOf(101));

        Korobyte korobyte = new Korobyte.KorobyteBuilder().build();

        Place place1 = new Place.PlaceBuilder(placeId1, korobyte)
            .setItemPlaces(Arrays.asList(
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "223").build(), 2),
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "323").build(), 1)))
            .build();

        ResourceId placeId2 = new ResourceId(null, String.valueOf(202));

        Place place2 = new Place.PlaceBuilder(placeId2, korobyte)
            .setItemPlaces(Arrays.asList(
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "123").build(), 1),
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "223").build(), 1),
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "323").build(), 2)))
            .build();

        return new UpdateLgwOrder(orderId, Arrays.asList(place1, place2));
    }

    private UpdateLgwOrder getMultiParcelUpdateOrder() {
        ResourceId orderId = new ResourceId(String.valueOf(ORDER_ID), null);

        ResourceId placeId1 = new ResourceId(null, String.valueOf(101));

        Korobyte korobyte = new Korobyte.KorobyteBuilder().build();

        Place place1 = new Place.PlaceBuilder(placeId1, korobyte)
            .setItemPlaces(Arrays.asList(
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "223").build(), 1),
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "323").build(), 1)))
            .build();

        ResourceId placeId2 = new ResourceId(null, String.valueOf(202));

        Place place2 = new Place.PlaceBuilder(placeId2, korobyte)
            .setItemPlaces(Arrays.asList(
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "123").build(), 1),
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "223").build(), 1),
                new ItemPlace(new UnitId.UnitIdBuilder(1L, "323").build(), 2)))
            .build();

        return new UpdateLgwOrder(orderId, Arrays.asList(place1, place2));
    }

    private Order getCheckouterOrder() {
        Order order = new Order();

        OrderItem orderItem1 = new OrderItem();
        orderItem1.setFeedOfferId(new FeedOfferId("1", 1L));
        orderItem1.setId(1123L);
        orderItem1.setShopSku("123");
        orderItem1.setSupplierId(1L);
        orderItem1.setCount(1);

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setFeedOfferId(new FeedOfferId("2", 2L));
        orderItem2.setId(1223L);
        orderItem2.setShopSku("223");
        orderItem2.setSupplierId(1L);
        orderItem2.setCount(2);

        OrderItem orderItem3 = new OrderItem();
        orderItem3.setFeedOfferId(new FeedOfferId("3", 3L));
        orderItem3.setId(1323L);
        orderItem3.setShopSku("323");
        orderItem3.setSupplierId(1L);
        orderItem3.setCount(3);

        order.setItems(Arrays.asList(orderItem1, orderItem2, orderItem3));

        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(DELIVERY_SERVICE_ID);

        Parcel parcel = new Parcel();
        parcel.setId(123L);
        Track track = new Track(order.getId(), "track_code", 1122L);
        parcel.setTracks(Collections.singletonList(track));

        delivery.setParcels(Collections.singletonList(parcel));
        order.setDelivery(delivery);

        return order;
    }

}
