package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.track;

import java.time.LocalDate;
import java.time.Month;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.order.delivery.track.DeliveryTrackerServiceImpl;
import ru.yandex.market.checkout.checkouter.order.delivery.track.TrackingType;
import ru.yandex.market.checkout.checkouter.storage.track.registration.TrackRegistration;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackRequest;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class StartTrackingProcessorTest extends AbstractServicesTestBase {

    private static final long CONSUMER_ID = 1L;
    private static final String TRACK_CODE = "asdasd";
    private static final long DELIVERY_SERVICE_ID = 55L;
    private static final long TRACK_ID = 1111L;
    private static final long TRACKER_ID = 123L;
    private static final long ORDER_ID = 234L;

    StartTrackingProcessor processor;
    @Mock
    private TrackerApiClient trackerApiClient;
    @Mock
    private OrderUpdateService orderUpdateService;

    @Nonnull
    private static TrackRegistration createTrackRegistration(DeliveryServiceType deliveryServiceType) {
        return new TrackRegistration(
                createTrack(deliveryServiceType),
                LocalDate.of(2017, Month.APRIL, 1),
                LocalDate.of(2017, Month.APRIL, 3),
                DeliveryType.DELIVERY,
                false
        );
    }

    @Nonnull
    private static Track createTrack(DeliveryServiceType deliveryServiceType) {
        Track track = TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID);
        track.setId(TRACK_ID);
        track.setOrderId(ORDER_ID);
        track.setDeliveryServiceType(deliveryServiceType);
        return track;
    }

    @Nonnull
    private static DeliveryTrackMeta createTrackMeta() {
        DeliveryTrackMeta meta = new DeliveryTrackMeta();
        meta.setId(TRACKER_ID);
        return meta;
    }

    @BeforeEach
    void init() {
        initMocks(this);
        processor = new StartTrackingProcessor(
                orderUpdateService,
                new DeliveryTrackerServiceImpl(trackerApiClient, CONSUMER_ID),
                100,
                10
        );
        processor.setTrackerApiClient(trackerApiClient);
        processor.setConsumerId(CONSUMER_ID);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(trackerApiClient);
    }

    @Test
    void registerDsTrackTest() {
        DeliveryTrackRequest request = createRequest(EntityType.ORDER, ApiVersion.DS);
        when(trackerApiClient.registerDeliveryTrack(eq(request))).thenReturn(createTrackMeta());

        processor.registerTrack(createTrackRegistration(DeliveryServiceType.CARRIER), TrackingType.DEFAULT);

        verify(trackerApiClient).registerDeliveryTrack(eq(request));
    }

    @Test
    void registerDSBSOrderFfTrackTest() {
        DeliveryTrackRequest request = createRequest(EntityType.EXTERNAL_ORDER, ApiVersion.FF);
        when(trackerApiClient.registerDeliveryTrack(eq(request))).thenReturn(createTrackMeta());

        processor.registerTrack(createTrackRegistration(DeliveryServiceType.SORTING_CENTER), TrackingType.DSBS);

        verify(trackerApiClient).registerDeliveryTrack(eq(request));
    }

    @Test
    void registerTrackWithUnknownDeliveryServiceType() {
        assertThrows(
                IllegalStateException.class,
                () -> processor.registerTrack(createTrackRegistration(DeliveryServiceType.UNKNOWN),
                        TrackingType.DEFAULT),
                "Can't get api version from unknown delivery service type"
        );
    }

    @Nonnull
    private DeliveryTrackRequest createRequest(EntityType entityType, ApiVersion apiVersion) {
        return DeliveryTrackRequest.builder()
                .trackCode(TRACK_CODE)
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .consumerId(CONSUMER_ID)
                .entityId(String.valueOf(ORDER_ID))
                .estimatedArrivalDateFrom(LocalDate.of(2017, Month.APRIL, 1))
                .estimatedArrivalDateTo(LocalDate.of(2017, Month.APRIL, 3))
                .deliveryType(ru.yandex.market.delivery.tracker.domain.enums.DeliveryType.DELIVERY)
                .isGlobalOrder(false)
                .entityType(entityType)
                .apiVersion(apiVersion)
                .build();
    }
}
