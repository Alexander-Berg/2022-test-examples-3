package ru.yandex.market.delivery.tracker.service.logger;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.TrackRequestMeta;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceRole;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;

import static org.mockito.Mockito.verify;
import static ru.yandex.market.delivery.tracker.domain.enums.HasIntId.enumById;

public class TrackingFailureLoggerTest {

    public static final long BATCH_ID = 1L;
    public static final long SERVICE_ID = 10L;
    public static final long TRACK_ID = 100L;
    @Mock
    private TskvLogger tskvLogger;

    @InjectMocks
    private TrackingFailureLogger trackingFailureLogger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void logFailureWithEmptyData() {
        List<DeliveryTrackMeta> tracks = List.of();
        TrackRequestMeta trackRequestMeta = new TrackRequestMeta()
            .setServiceId(SERVICE_ID)
            .setType(enumById(RequestType.class, 0));
        String exception = "";
        DeliveryService deliveryService = null;

        Map<String, String> expectedTskvMap = ImmutableMap.<String, String>builder()
            .put("trackIds", "[]")
            .put("serviceId", "10")
            .put("trackMethod", "ORDER_HISTORY")
            .put("batchId", "1")
            .put("exception", "")
            .put("deliveryServiceRole", "")
            .build();

        trackingFailureLogger.logFailure(BATCH_ID, tracks, trackRequestMeta, exception, deliveryService);

        verify(tskvLogger).log(expectedTskvMap);
    }

    @Test
    void logFailureWithFullData() {
        List<DeliveryTrackMeta> tracks = List.of(
            new DeliveryTrackMeta().setId(TRACK_ID),
            new DeliveryTrackMeta().setId(TRACK_ID + 1)
        );
        TrackRequestMeta trackRequestMeta = new TrackRequestMeta()
            .setServiceId(SERVICE_ID)
            .setType(RequestType.ORDER_STATUS);
        String exception = "failed to fetch";
        DeliveryService deliveryService = new DeliveryService(SERVICE_ID, "", DeliveryServiceType.DELIVERY);
        deliveryService.setRole(DeliveryServiceRole.EXPRESS_GO);

        Map<String, String> expectedTskvMap = ImmutableMap.<String, String>builder()
            .put("trackIds", "[100, 101]")
            .put("serviceId", "10")
            .put("trackMethod", "ORDER_STATUS")
            .put("batchId", "1")
            .put("exception", exception)
            .put("deliveryServiceRole", DeliveryServiceRole.EXPRESS_GO.name())
            .build();

        trackingFailureLogger.logFailure(BATCH_ID, tracks, trackRequestMeta, exception, deliveryService);

        verify(tskvLogger).log(expectedTskvMap);
    }
}
