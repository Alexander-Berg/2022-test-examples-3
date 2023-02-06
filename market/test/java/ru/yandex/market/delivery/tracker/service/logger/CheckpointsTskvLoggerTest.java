package ru.yandex.market.delivery.tracker.service.logger;

import java.util.Date;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceRole;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryTrackStatus;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.SurveyType;

@ExtendWith(MockitoExtension.class)
class CheckpointsTskvLoggerTest {

    private static final long TRACK_ID = 1;
    private static final long CHECKPOINT_ID = 2;
    private static final long DELIVERY_SERVICE_ID = 654;
    private static final String ORDER_ID = "12345";
    private static final String TRACK_CODE = "ABC12345";
    private static final Date DATE = new Date();
    private static final String DATE_STRING = "2018-07-12 04:29:47.057+0300";
    private static final boolean FIRST_NOTIFY_SUCCESS = true;
    private static final long CONSUMER_ID = 1;
    private static final DeliveryServiceType DELIVERY_SERVICE_TYPE = DeliveryServiceType.DELIVERY;
    private static final DeliveryServiceRole DELIVERY_SERVICE_ROLE = DeliveryServiceRole.EXPRESS_GO;

    @Mock
    private TskvLogger tskvLogger;

    @InjectMocks
    private CheckpointsTskvLogger checkpointsTskvLogger;

    @Test
    void logCheckpoint() {
        Mockito.when(tskvLogger.formatDate(DATE)).thenReturn(DATE_STRING);
        checkpointsTskvLogger.logCheckpoint(
            getCheckpoint(),
            getTrackMeta(),
            CONSUMER_ID,
            DATE,
            FIRST_NOTIFY_SUCCESS,
            DELIVERY_SERVICE_ROLE
        );
        Mockito.verify(tskvLogger).log(getTskvMap());
    }

    private DeliveryTrackCheckpoint getCheckpoint() {
        return new DeliveryTrackCheckpoint()
            .setId(CHECKPOINT_ID)
            .setTrackId(TRACK_ID)
            .setCheckpointStatus(CheckpointStatus.IN_TRANSIT)
            .setDeliveryCheckpointStatus(OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED)
            .setCheckpointDate(DATE)
            .setAcquiredByTrackerDate(DATE)
            .setSurveyType(SurveyType.PULL);
    }

    private DeliveryTrackMeta getTrackMeta() {
        DeliveryTrackMeta trackMeta = new DeliveryTrackMeta();

        trackMeta.setId(TRACK_ID);
        trackMeta.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        trackMeta.setEntityId(ORDER_ID);
        trackMeta.setTrackCode(TRACK_CODE);
        trackMeta.setStartDate(DATE);
        trackMeta.setDeliveryTrackStatus(DeliveryTrackStatus.STARTED);
        trackMeta.setEntityType(EntityType.ORDER);
        trackMeta.setDeliveryServiceType(DELIVERY_SERVICE_TYPE);

        return trackMeta;
    }

    private Map<String, String> getTskvMap() {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        builder.put("trackId", String.valueOf(TRACK_ID));
        builder.put("deliveryServiceId", String.valueOf(DELIVERY_SERVICE_ID));
        builder.put("entityId", ORDER_ID);
        builder.put("orderId", ORDER_ID);
        builder.put("trackCode", TRACK_CODE);
        builder.put("startTs", DATE_STRING);
        builder.put("trackStatus", String.valueOf(DeliveryTrackStatus.STARTED.getId()));
        builder.put("checkpointStatus", String.valueOf(CheckpointStatus.IN_TRANSIT.getId()));
        builder.put("checkpointRawStatus", String.valueOf(OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED.getId()));
        builder.put("checkpointTs", DATE_STRING);
        builder.put("acquiredByTrackerTs", DATE_STRING);
        builder.put("lastNotifySuccessTs", DATE_STRING);
        builder.put("surveyType", String.valueOf(SurveyType.PULL.getId()));
        builder.put("firstNotifySuccess", String.valueOf(FIRST_NOTIFY_SUCCESS));
        builder.put("consumerId", String.valueOf(CONSUMER_ID));
        builder.put("entityType", EntityType.ORDER.name());
        builder.put("deliveryServiceType", DELIVERY_SERVICE_TYPE.name());
        builder.put("deliveryServiceRole", DELIVERY_SERVICE_ROLE.name());

        return builder.build();
    }
}
