package ru.yandex.market.delivery.mdbapp.components.service.checkouter.client;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterDeliveryAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.request.ParcelPatchRequest;
import ru.yandex.market.delivery.mdbapp.exception.InternalServerErrorException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

public class CheckouterParcelServiceTest {

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private static final Long ORDER_ID = 12L;
    private static final Long PARCEL_ID = 123L;
    private static final Long DELIVERY_SERVICE_ID = 1234L;
    private static final String TRACK_NO = "tracking number";

    private final CheckouterAPI checkouterClientMock = Mockito.mock(CheckouterAPI.class);
    private final CheckouterDeliveryAPI checkouterDeliveryClientMock = Mockito.mock(CheckouterDeliveryAPI.class);
    private CheckouterParcelService parcelService;

    @Before
    public void setUp() {
        parcelService = new CheckouterParcelService(checkouterClientMock, checkouterDeliveryClientMock);
    }

    @Test
    public void updateStatusSuccessTest() {
        Mockito.when(checkouterClientMock.updateParcel(
                eq(ORDER_ID),
                eq(PARCEL_ID),
                Mockito.any(ParcelPatchRequest.class),
                eq(ClientRole.SYSTEM),
                Mockito.isNull()
            ))
            .thenReturn(new Parcel());

        Parcel order = parcelService.updateParcelStatus(
            ORDER_ID,
            PARCEL_ID,
            ParcelStatus.CREATED
        );
        softly.assertThat(order).as("Proper API method should be called")
            .isNotNull();
    }

    @Test(expected = InternalServerErrorException.class)
    public void updateStatusFailureTest() {
        Mockito.when(checkouterClientMock.updateParcel(
                eq(ORDER_ID),
                eq(PARCEL_ID),
                Mockito.any(ParcelPatchRequest.class),
                eq(ClientRole.SYSTEM),
                Mockito.isNull()
            ))
            .thenThrow(new OrderStatusNotAllowedException("anycode", "anymessage", 1));
        parcelService.updateParcelStatus(
            ORDER_ID,
            PARCEL_ID,
            ParcelStatus.CREATED
        );
        Mockito.verify(checkouterClientMock, Mockito.atMost(1));
    }

    @Test
    public void addTrackSuccessTest() {
        Mockito.when(checkouterClientMock.addTrack(
                eq(ORDER_ID),
                eq(PARCEL_ID),
                any(Track.class),
                eq(ClientRole.SYSTEM),
                eq(null)
            ))
            .thenReturn(new Track());

        Track track =
            parcelService.addTrack(ORDER_ID, PARCEL_ID, TRACK_NO, DELIVERY_SERVICE_ID, DeliveryServiceType.FULFILLMENT);
        softly.assertThat(track).as("Proper API method should be called")
            .isNotNull();
    }

    @Test(expected = InternalServerErrorException.class)
    public void addTrackFailureTest() {
        Mockito.when(checkouterClientMock.addTrack(
                eq(ORDER_ID),
                eq(PARCEL_ID),
                any(Track.class),
                eq(ClientRole.SYSTEM),
                eq(null)
            ))
            .thenThrow(new RuntimeException());
        parcelService.addTrack(ORDER_ID, PARCEL_ID, TRACK_NO, DELIVERY_SERVICE_ID, DeliveryServiceType.FULFILLMENT);
    }

}
