package ru.yandex.market.tpl.core.domain.sc.task.notify;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackRequest;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryTrackStatus;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

@RequiredArgsConstructor
class RegisterTrackServiceTest extends TplAbstractTest {

    private final RegisterTrackService registerTrackService;

    @MockBean
    private TrackerApiClient trackerApiClient;

    @Test
    void registerTrackTest() {
        DeliveryTrackRequest request = createRequest();
        Mockito.when(trackerApiClient.registerDeliveryTrack(Mockito.eq(request))).thenReturn(createMeta());

        registerTrackService.processPayload(new RegisterTrackPayload("1", "123", "456", 789));

        Mockito.verify(trackerApiClient).registerDeliveryTrack(Mockito.eq(request));
    }

    @Nonnull
    private DeliveryTrackRequest createRequest() {
        return DeliveryTrackRequest.builder()
                .trackCode("456")
                .deliveryServiceId(789)
                .consumerId(4)
                .entityId("123")
                .deliveryType(DeliveryType.DELIVERY)
                .isGlobalOrder(false)
                .entityType(EntityType.ORDER)
                .apiVersion(ApiVersion.FF)
                .build();
    }

    @Nonnull
    private DeliveryTrackMeta createMeta() {
        return new DeliveryTrackMeta()
                .setId(1L)
                .setDeliveryTrackStatus(DeliveryTrackStatus.STARTED);
    }
}
