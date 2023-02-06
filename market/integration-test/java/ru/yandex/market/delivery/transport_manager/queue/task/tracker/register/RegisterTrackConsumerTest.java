package ru.yandex.market.delivery.transport_manager.queue.task.tracker.register;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackRequest;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

class RegisterTrackConsumerTest extends AbstractContextualTest {
    @Autowired
    private RegisterTrackConsumer consumer;

    @Autowired
    private TrackerApiClient trackerApiClient;

    @Test
    void executeTask() {
        consumer.executeTask(task());

        Mockito.verify(trackerApiClient).registerDeliveryTrack(request(
            "12345tgt",
            6L,
            "TMM1",
            ru.yandex.market.delivery.tracker.domain.enums.EntityType.MOVEMENT,
            ApiVersion.DS
        ));
    }

    private static Task<RegisterTrackDto> task() {
        return Task.<RegisterTrackDto>builder(new QueueShardId("123"))
            .withPayload(new RegisterTrackDto(
                1L,
                EntityType.MOVEMENT,
                "12345tgt",
                6L,
                RegisterTrackApiType.DS
            ))
            .build();
    }

    private static DeliveryTrackRequest request(
        String externalId,
        Long partnerId,
        String id,
        ru.yandex.market.delivery.tracker.domain.enums.EntityType entityType,
        ApiVersion apiVersion
    ) {
        return DeliveryTrackRequest.builder()
            .trackCode(externalId)
            .deliveryServiceId(partnerId)
            .consumerId(5)
            .entityId(id)
            .isGlobalOrder(false)
            .entityType(entityType)
            .apiVersion(apiVersion)
            .build();
    }
}
