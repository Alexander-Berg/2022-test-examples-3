package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.track;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.delivery.track.TrackingService;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeleteTrackProcessorTest  extends AbstractServicesTestBase {
    private static final String TRACK_CODE = "asdasd";
    private static final long DELIVERY_SERVICE_ID = 55L;
    private static final long TRACK_ID = 1111L;
    private static final long TRACKER_ID = 123L;
    private static final long ORDER_ID = 234L;

    private DeleteTrackProcessor deleteTrackProcessor;
    @Mock
    private TrackerApiClient trackerApiClient;
    @Autowired
    CheckouterFeatureReader checkouterFeatureReader;
    @Mock
    TrackingService trackingService;

    @BeforeEach
    void init() {
        deleteTrackProcessor = new DeleteTrackProcessor(trackerApiClient, checkouterFeatureReader, trackingService, 100,
                10);
    }

    @Test
    public void deleteTrackTest() {
        //given:
        when(trackerApiClient.deleteDeliveryTrack(
            Mockito.anyLong()
        )).thenReturn(createTrackMeta());

        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_QUEUED_CALL_FOR_DELETE_TRACK, true);
        when(trackingService.findTrackById(TRACK_ID)).thenReturn(Optional.of(createTrack()));

        //when:
        ExecutionResult process = deleteTrackProcessor.process(new QueuedCallProcessor.QueuedCallExecution(TRACK_ID,
            null, 3, Instant.now(), TRACK_ID));

        //then:
        verify(trackerApiClient).deleteDeliveryTrack(TRACK_ID);
        assertEquals(ExecutionResult.SUCCESS, process);
    }

    private static Track createTrack() {
        Track track = TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID);
        track.setId(TRACK_ID);
        track.setOrderId(ORDER_ID);
        return track;
    }

    private static DeliveryTrackMeta createTrackMeta() {
        DeliveryTrackMeta meta = new DeliveryTrackMeta();
        meta.setId(TRACKER_ID);
        return meta;
    }
}
