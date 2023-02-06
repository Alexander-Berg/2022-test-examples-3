package ru.yandex.market.logistics.logistics4shops.controller.orderexternaltrack;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderExternalTrackDto;
import ru.yandex.market.logistics.logistics4shops.config.properties.FeatureProperties;
import ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AbstractOrderExternalTrackControllerTest extends AbstractIntegrationTest {

    @Autowired
    protected CheckouterAPI checkouterAPI;

    @Captor
    protected ArgumentCaptor<List<Track>> tracksCaptor;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(checkouterAPI);
    }


    protected void setupLogic(boolean useFinalLogic) {
        setupFeature(FeatureProperties::isUseFinalOrderExternalTrackLogic, useFinalLogic);
    }

    @Nonnull
    protected AutoCloseable mockUpdateTracks(long orderId) {
        return checkouterFactory.mockPutOrderTracks(
            orderId,
            CheckouterFactory.CHECKOUTER_SHOP_ID,
            tracksCaptor
        );
    }

    @Nonnull
    protected AutoCloseable mockUpdateWithBadRequest() {
        when(checkouterAPI.updateDeliveryTracks(
            eq(CheckouterFactory.CHECKOUTER_ORDER_ID),
            eq(CheckouterFactory.CHECKOUTER_PARCEL_ID),
            anyList(),
            eq(ClientRole.SHOP),
            eq(CheckouterFactory.CHECKOUTER_SHOP_ID)
        )).thenThrow(new ErrorCodeException("TRACK_UPDATE_FAILED", "Cannot update tracks", 400));
        return () -> verify(checkouterAPI).updateDeliveryTracks(
            eq(CheckouterFactory.CHECKOUTER_ORDER_ID),
            eq(CheckouterFactory.CHECKOUTER_PARCEL_ID),
            tracksCaptor.capture(),
            eq(ClientRole.SHOP),
            eq(CheckouterFactory.CHECKOUTER_SHOP_ID)
        );
    }

    @Nonnull
    protected AutoCloseable mockCheckouter(long orderId, long shopId) {
        return checkouterFactory.mockGetOrderTracks(
            orderId,
            shopId,
            List.of(CheckouterFactory.buildTrack(orderId))
        );
    }

    @Nonnull
    protected AutoCloseable mockCheckouter(long orderId) {
        return mockCheckouter(orderId, CheckouterFactory.CHECKOUTER_SHOP_ID);
    }

    @Nonnull
    protected AutoCloseable mockCheckouter() {
        return mockCheckouter(CheckouterFactory.CHECKOUTER_ORDER_ID);
    }

    @Nonnull
    protected AutoCloseable mockCheckouterWithoutTracks(long orderId) {
        return checkouterFactory.mockGetOrderTracks(
            orderId,
            CheckouterFactory.CHECKOUTER_SHOP_ID,
            null
        );
    }

    @Nonnull
    protected AutoCloseable mockCheckouterWithoutTracks() {
        return mockCheckouterWithoutTracks(CheckouterFactory.CHECKOUTER_ORDER_ID);
    }

    @Nonnull
    protected AutoCloseable mockCheckouterNotFound(long orderId, long shopId) {
        return checkouterFactory.mockGetOrderTracksNotFound(orderId, shopId);
    }

    @Nonnull
    protected AutoCloseable mockCheckouterNotFound(long shopId) {
        return mockCheckouterNotFound(CheckouterFactory.CHECKOUTER_ORDER_ID, shopId);
    }

    @Nonnull
    protected AutoCloseable mockCheckouterNotFound() {
        return mockCheckouterNotFound(CheckouterFactory.CHECKOUTER_SHOP_ID);
    }

    @Nonnull
    protected static OrderExternalTrackDto buildDefaultResponse() {
        return new OrderExternalTrackDto()
            .deliveryServiceId(CheckouterFactory.DS_ID)
            .trackingNumber(CheckouterFactory.DS_TRACK_CODE);
    }
}
