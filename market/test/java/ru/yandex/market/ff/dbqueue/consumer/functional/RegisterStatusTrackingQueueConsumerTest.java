package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackRequest;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryTrackStatus;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.RegisterStatusTrackingQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.RegisterStatusTrackingPayload;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

class RegisterStatusTrackingQueueConsumerTest extends IntegrationTestWithDbQueueConsumers {

    private static final long CONSUMER_ID = 6L;

    @Autowired
    private TrackerApiClient trackerApiClient;
    @Autowired
    private RegisterStatusTrackingQueueConsumer registerStatusTrackingQueueConsumer;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup("classpath:service/delivery-tracker/register-before.xml")
    void shouldBeRegisteredAtDeliveryTracker() {
        DeliveryTrackMeta meta = new DeliveryTrackMeta("222",
                new DeliveryService(1, null, null),
                "0",
                EntityType.INBOUND, 0L).setDeliveryTrackStatus(DeliveryTrackStatus.STARTED);
        Mockito.when(trackerApiClient.registerDeliveryTrack(any(DeliveryTrackRequest.class))).thenReturn(meta);

        executeTask(0);

        Mockito.verify(trackerApiClient).registerDeliveryTrack(
            argThat(matchRequestWithParams("Supply0", 1L, "0", EntityType.INBOUND))
        );
    }

    @Test
    @DatabaseSetup("classpath:service/delivery-tracker/register-before.xml")
    void shouldBeRegisteredAtDeliveryTrackerWithOldApi() {
        DeliveryTrackMeta meta = new DeliveryTrackMeta("888",
                new DeliveryService(1, null, null),
                "1",
                EntityType.INBOUND_OLD, 0L).setDeliveryTrackStatus(DeliveryTrackStatus.STARTED);
        Mockito.when(trackerApiClient.registerDeliveryTrack(any(DeliveryTrackRequest.class))).thenReturn(meta);

        executeTask(1);
        Mockito.verify(trackerApiClient).registerDeliveryTrack(
            argThat(matchRequestWithParams(null, null, null, EntityType.INBOUND_OLD))
        );
    }

    @Test
    @DatabaseSetup("classpath:service/delivery-tracker/register-xdoc-before.xml")
    void shouldBeRegisteredAtDeliveryTrackerAsXdoc() {
        DeliveryTrackMeta meta = new DeliveryTrackMeta("200",
                new DeliveryService(1, null, null),
                "1",
                EntityType.INBOUND, 0L).setDeliveryTrackStatus(DeliveryTrackStatus.STARTED);
        Mockito.when(trackerApiClient.registerDeliveryTrack(any(DeliveryTrackRequest.class))).thenReturn(meta);

        executeTask(1);
        Mockito.verify(trackerApiClient).registerDeliveryTrack(
            argThat(matchRequestWithParams("XDocSupply145", 48111L, "1", EntityType.INBOUND_OLD))
        );
    }

    @Test
    @DatabaseSetup("classpath:service/delivery-tracker/register-before.xml")
    void shouldBeSavedForRetry() {
        DeliveryTrackMeta metaDeleted = new DeliveryTrackMeta("666",
                new DeliveryService(1, null, null),
                "1",
                EntityType.INBOUND, 0L).setDeliveryTrackStatus(DeliveryTrackStatus.DELETED);
        DeliveryTrackMeta meta = new DeliveryTrackMeta("666",
                new DeliveryService(1, null, null),
                "1",
                EntityType.INBOUND, 0L).setDeliveryTrackStatus(DeliveryTrackStatus.STARTED);
        Mockito.when(trackerApiClient.registerDeliveryTrack(any(DeliveryTrackRequest.class)))
                .thenReturn(metaDeleted)
                .thenReturn(meta);

        executeTask(2);
        Mockito.verify(trackerApiClient).registerDeliveryTrack(
            argThat(matchRequestWithParams(null, null, null, EntityType.INBOUND))
        );
    }

    private void executeTask(long requestId) {
        RegisterStatusTrackingPayload payload = new RegisterStatusTrackingPayload(requestId);
        Task<RegisterStatusTrackingPayload> task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        transactionTemplate.execute(status -> registerStatusTrackingQueueConsumer.execute(task));
    }

    @Nonnull
    private ArgumentMatcher<DeliveryTrackRequest> matchRequestWithParams(
        @Nullable String trackCode,
        @Nullable Long deliveryServiceId,
        @Nullable String entityId,
        @Nullable EntityType entityType
    ) {
        return argument -> (trackCode == null || trackCode.equals(argument.getTrackCode()))
            && (deliveryServiceId == null || deliveryServiceId == argument.getDeliveryServiceId())
            && (entityId == null || entityId.equals(argument.getEntityId()))
            && (entityType == null || entityType.equals(argument.getEntityType()))
            && argument.getConsumerId() == CONSUMER_ID
            && !argument.isGlobalOrder();
    }

}
