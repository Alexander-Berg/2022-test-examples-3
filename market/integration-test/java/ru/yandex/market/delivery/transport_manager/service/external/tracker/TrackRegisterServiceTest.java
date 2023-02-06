package ru.yandex.market.delivery.transport_manager.service.external.tracker;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackRequest;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType;
import ru.yandex.market.delivery.transport_manager.queue.task.tracker.register.RegisterTrackApiType;

class TrackRegisterServiceTest extends AbstractContextualTest {
    @Autowired
    private TrackRegisterService trackRegisterService;

    @Autowired
    private TrackerApiClient trackerApiClient;

    @Test
    void registerTrack() {
        trackRegisterService.registerTrack(
            1L,
            EntityType.MOVEMENT,
            "12345tgt",
            6L,
            RegisterTrackApiType.DS
        );

        Mockito.verify(trackerApiClient).registerDeliveryTrack(request(
            "12345tgt",
            6L,
            "TMM1",
            ru.yandex.market.delivery.tracker.domain.enums.EntityType.MOVEMENT,
            ApiVersion.DS
        ));

        trackRegisterService.registerTrack(
            2L,
            EntityType.INBOUND,
            "inb",
            7L,
            RegisterTrackApiType.FF
        );

        Mockito.verify(trackerApiClient).registerDeliveryTrack(request(
            "inb",
            7L,
            "TMU2",
            ru.yandex.market.delivery.tracker.domain.enums.EntityType.INBOUND,
            ApiVersion.FF
        ));

        trackRegisterService.registerTrack(
            3L,
            EntityType.OUTBOUND,
            "oub",
            100500L,
            RegisterTrackApiType.FF
        );

        Mockito.verify(trackerApiClient).registerDeliveryTrack(request(
            "oub",
            100500L,
            "TMU3",
            ru.yandex.market.delivery.tracker.domain.enums.EntityType.OUTBOUND,
            ApiVersion.FF
        ));
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
