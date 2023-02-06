package ru.yandex.market.delivery.mdbapp.api;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.testutils.ResourceUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class LgwUpdateOrderSuccessTest extends MockContextualTest {

    private static final long ORDER_ID = 1L;
    private static final long PARCEL_ID = 12L;

    @Captor
    private ArgumentCaptor<Delivery> deliveryArgumentCaptor;

    @Autowired
    private HealthManager healthManager;

    @MockBean
    private CheckouterAPI checkouterAPI;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        when(healthManager.isHealthyEnough()).thenReturn(true);
    }

    @Test
    public void splitPlacesToParcels() throws Exception {
        Order order = OrderSteps.getOrderWithParcelBoxes(ORDER_ID, Color.RED);

        when(checkouterAPI.getOrder(ORDER_ID, ClientRole.SYSTEM, null, false))
            .thenReturn(order);

        setLgwUpdateOrderSuccess();

        verify(checkouterAPI).updateOrderDelivery(
            eq(ORDER_ID),
            eq(ClientRole.SYSTEM),
            eq(null),
            deliveryArgumentCaptor.capture()
        );

        List<Parcel> actualParcels = deliveryArgumentCaptor.getValue().getParcels();
        List<Parcel> expectedParcels = List.of(
            createParcel(
                PARCEL_ID,
                List.of(
                    createTrack("track-code", 123L, DeliveryServiceType.CARRIER),
                    createTrack("deliveryId2", 987L, null)
                ),
                List.of(createParcelItem()),
                List.of(createParcelBoxWithItems())
            ),
            createParcel(
                null,
                List.of(createTrack("deliveryId1", 987L, null)),
                List.of(createParcelItem()),
                List.of(createParcelBoxWithItems())
            )
        );

        softly.assertThat(actualParcels.size()).isEqualTo(expectedParcels.size());
        for (int i = 0; i < actualParcels.size(); i++) {
            Parcel actualParcel = actualParcels.get(i);
            Parcel expectedParcel = expectedParcels.get(i);
            softly.assertThat(actualParcel)
                .as(String.format("Delivery.parcels[%d]", i))
                .isEqualToIgnoringGivenFields(
                    expectedParcel,
                    "tracks", "items", "boxes"
                );

            softly.assertThat(actualParcel.getTracks().size()).isEqualTo(expectedParcel.getTracks().size());
            for (int j = 0; j < actualParcel.getTracks().size(); j++) {
                softly.assertThat(actualParcel.getTracks().get(j))
                    .as(String.format("Delivery.parcels[%d].tracks[%d]", i, j))
                    .isEqualToComparingFieldByField(expectedParcel.getTracks().get(j));
            }

            softly.assertThat(actualParcel.getParcelItems().size()).isEqualTo(expectedParcel.getParcelItems().size());
            for (int j = 0; j < actualParcel.getParcelItems().size(); j++) {
                softly.assertThat(actualParcel.getParcelItems().get(j))
                    .as(String.format("Delivery.parcels[%d].items[%d]", i, j))
                    .isEqualToComparingFieldByField(expectedParcel.getParcelItems().get(j));
            }

            softly.assertThat(actualParcel.getBoxes().size()).isEqualTo(expectedParcel.getBoxes().size());
            for (int j = 0; j < actualParcel.getBoxes().size(); j++) {
                softly.assertThat(actualParcel.getBoxes().get(j).getItems().get(0))
                    .as(String.format("Delivery.parcels[%d].boxes[%d].items", i, j))
                    .isEqualToComparingFieldByField(expectedParcel.getBoxes().get(j).getItems().get(0));
            }
        }
    }

    private void setLgwUpdateOrderSuccess() throws Exception {
        mockMvc.perform(
            post("/orders/" + ORDER_ID + "/lgwUpdateSuccess")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ResourceUtils.getFileContent(
                    "data/controller/request/lgw-update-order-success-request.json"
                ))
        )
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    private Parcel createParcel(
        Long id,
        List<Track> tracks,
        List<ParcelItem> parcelItems,
        List<ParcelBox> parcelBoxes
    ) {
        Parcel parcel = new Parcel();
        parcel.setId(id);
        parcel.setTracks(tracks);
        parcel.setParcelItems(parcelItems);
        parcel.setBoxes(parcelBoxes);

        return parcel;
    }

    private Track createTrack(
        String trackCode,
        Long deliveryServiceId,
        DeliveryServiceType deliveryServiceType
    ) {
        Track track = new Track();
        track.setTrackCode(trackCode);
        track.setDeliveryServiceId(deliveryServiceId);
        track.setDeliveryServiceType(deliveryServiceType);

        return track;
    }

    private ParcelItem createParcelItem() {
        ParcelItem parcelItem = new ParcelItem();
        parcelItem.setItemId(1L);
        parcelItem.setCount(1);

        return parcelItem;
    }

    private ParcelBox createParcelBoxWithItems() {
        ParcelBoxItem parcelBoxItem = new ParcelBoxItem();
        parcelBoxItem.setItemId(1L);
        parcelBoxItem.setCount(1);

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setItems(List.of(parcelBoxItem));

        return parcelBox;
    }
}
