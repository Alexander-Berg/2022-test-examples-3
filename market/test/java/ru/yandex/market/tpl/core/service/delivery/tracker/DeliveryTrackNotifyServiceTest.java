package ru.yandex.market.tpl.core.service.delivery.tracker;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.tracker.domain.entity.DateInterval;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrack;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryTrackStatus;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.SurveyType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.mockito.Mockito.verify;

@RequiredArgsConstructor
public class DeliveryTrackNotifyServiceTest extends TplAbstractTest {
    private final DeliveryTrackNotifyService deliveryTrackNotifyService;
    private final Clock clock;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;

    @MockBean
    private DeliveryTrackService deliveryTrackService;

    @Test
    void shouldSaveToDBqueue() throws Exception {
        var mockTrack = mockDeliveryTrack();
        configurationServiceAdapter.insertValue(ConfigurationProperties.TRACK_NOTIFY_OVER_QUEUE_ENABLED, true);
        deliveryTrackNotifyService.notifyTracks(List.of(mockDeliveryTrack()));
        dbQueueTestUtil.executeSingleQueueItem(QueueType.NOTIFY_TRACK);
        verify(deliveryTrackService).notifyTracks(List.of(mockTrack));
    }


    private DeliveryTrack mockDeliveryTrack() {
        return new DeliveryTrack(mockDeliveryTrackMeta(), List.of(mockDeliveryTrackCheckpoint()));
    }

    private DeliveryTrackMeta mockDeliveryTrackMeta() {
        DeliveryTrackMeta meta = new DeliveryTrackMeta();
        meta.setId(12345L);
        meta.setTrackCode("trackCode");
        meta.setDeliveryServiceId(123L);
        meta.setDeliveryServiceType(DeliveryServiceType.DELIVERY);
        meta.setSourceId(123L);
        meta.setStartDate(Date.from(Instant.now(clock)));
        meta.setLastUpdatedDate(Date.from(Instant.now(clock)));
        meta.setNextRequestDate(Date.from(Instant.now(clock)));
        meta.setEntityId("entityId");
        meta.setOrderId("orderId");
        meta.setGlobalOrder(true);
        meta.setDeliveryTrackStatus(DeliveryTrackStatus.STARTED);
        meta.setEstimatedArrivalDate(DateInterval.create(LocalDate.now(), LocalDate.now().plusDays(1)));
        meta.setDeliveryType(DeliveryType.DELIVERY);
        meta.setLastCheckpointAcquiredDate(Date.from(Instant.now(clock)));
        meta.setStopTrackingDate(Date.from(Instant.now(clock)));
        meta.setLastStatusRequestDate(Date.from(Instant.now(clock)));
        meta.setLastOrdersStatusRequestDate(Date.from(Instant.now(clock)));
        meta.setEntityType(EntityType.ORDER);
        meta.setExternalDeliveryServiceId(1234567L);
        meta.setApiVersion(ApiVersion.DS);
        return meta;
    }

    private DeliveryTrackCheckpoint mockDeliveryTrackCheckpoint() {
        return new DeliveryTrackCheckpoint(
                12345L,
                123456L,
                "Country",
                "City",
                "location",
                "message",
                CheckpointStatus.DELIVERED,
                "zipCode",
                Date.from(Instant.now(clock)),
                1234,
                Date.from(Instant.now(clock)),
                SurveyType.PUSH,
                EntityType.ORDER
        );
    }
}
