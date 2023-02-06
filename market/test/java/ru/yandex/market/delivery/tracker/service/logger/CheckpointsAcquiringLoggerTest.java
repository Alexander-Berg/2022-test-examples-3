package ru.yandex.market.delivery.tracker.service.logger;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceRole;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.tracker.domain.enums.HasIntId.enumById;

class CheckpointsAcquiringLoggerTest {

    @Mock
    private TskvLogger tskvLogger;

    @InjectMocks
    private CheckpointsAcquiringLogger checkpointsAcquiringLogger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void logNewCheckpointWithEmptyData() {
        DeliveryTrackMeta trackMeta = new DeliveryTrackMeta();
        DeliveryTrackCheckpoint checkpoint = new DeliveryTrackCheckpoint()
            .setDeliveryCheckpointStatus(OrderDeliveryCheckpointStatus.UNKNOWN);
        RequestType requestType = enumById(RequestType.class, 0);

        Map<String, String> expectedTskvMap = ImmutableMap.<String, String>builder()
            .put("trackMethod", "ORDER_HISTORY")
            .put("startTs", "")
            .put("deliveryServiceId", "0")
            .put("entityId", "")
            .put("orderId", "")
            .put("trackCode", "")
            .put("checkpointRawStatus", "-1")
            .put("checkpointTs", "")
            .put("previousCheckTs", "")
            .put("acquiredTs", "")
            .put("scheduledRequestTs", "")
            .put("entityType", "")
            .put("deliveryServiceRole", "")
            .build();

        when(tskvLogger.formatDate(null)).thenReturn("");

        checkpointsAcquiringLogger.logNewCheckpoint(trackMeta, checkpoint, requestType, null);

        verify(tskvLogger).log(expectedTskvMap);
    }

    @Test
    void logNewCheckpointWithFullData() {
        Date startDate = Date.from(Instant.parse("2019-01-01T00:00:00Z"));
        Date lastUpdatedDate = Date.from(Instant.parse("2019-02-02T00:00:00Z"));
        Date statusDate = Date.from(Instant.parse("2019-03-03T00:00:00Z"));
        Date acquiredTs = Date.from(Instant.parse("2019-04-04T00:00:00Z"));
        Date scheduledRequestTs = Date.from(Instant.parse("2019-05-05T00:00:00Z"));

        DeliveryTrackMeta trackMeta = new DeliveryTrackMeta();
        trackMeta.setStartDate(startDate);
        trackMeta.setDeliveryServiceId(1);
        trackMeta.setEntityId("OrderId_1");
        trackMeta.setTrackCode("TrackCode_1");
        trackMeta.setLastUpdatedDate(lastUpdatedDate);
        trackMeta.setNextRequestDate(scheduledRequestTs);
        trackMeta.setEntityType(EntityType.ORDER);

        DeliveryTrackCheckpoint checkpoint = new DeliveryTrackCheckpoint()
            .setDeliveryCheckpointStatus(OrderDeliveryCheckpointStatus.ERROR)
            .setCheckpointDate(statusDate)
            .setAcquiredByTrackerDate(acquiredTs);

        RequestType requestType = enumById(RequestType.class, 1);
        DeliveryServiceRole deliveryServiceRole = DeliveryServiceRole.EXPRESS_GO;

        Map<String, String> expectedTskvMap = ImmutableMap.<String, String>builder()
            .put("trackMethod", "ORDER_STATUS")
            .put("startTs", "2019-01-01 00:00:00")
            .put("deliveryServiceId", "1")
            .put("entityId", "OrderId_1")
            .put("orderId", "OrderId_1")
            .put("trackCode", "TrackCode_1")
            .put("checkpointRawStatus", "2")
            .put("checkpointTs", "2019-03-03 00:00:00")
            .put("previousCheckTs", "2019-02-02 00:00:00")
            .put("acquiredTs", "2019-04-04 00:00:00")
            .put("scheduledRequestTs", "2019-05-05 00:00:00")
            .put("entityType", "ORDER")
            .put("deliveryServiceRole", DeliveryServiceRole.EXPRESS_GO.name())
            .build();

        when(tskvLogger.formatDate(startDate)).thenReturn("2019-01-01 00:00:00");
        when(tskvLogger.formatDate(lastUpdatedDate)).thenReturn("2019-02-02 00:00:00");
        when(tskvLogger.formatDate(statusDate)).thenReturn("2019-03-03 00:00:00");
        when(tskvLogger.formatDate(acquiredTs)).thenReturn("2019-04-04 00:00:00");
        when(tskvLogger.formatDate(scheduledRequestTs)).thenReturn("2019-05-05 00:00:00");

        checkpointsAcquiringLogger.logNewCheckpoint(trackMeta, checkpoint, requestType, deliveryServiceRole);

        verify(tskvLogger).log(expectedTskvMap);
    }
}
