package ru.yandex.market.tpl.core.domain.yago;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.tpl.common.util.TestUtil;
import ru.yandex.market.tpl.core.domain.yago.event.YandexGoOrderConfirmedEvent;
import ru.yandex.market.tpl.core.service.delivery.tracker.DeliveryTrackService;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YandexGoOrderSelfEventListenersTest {

    @InjectMocks
    YandexGoOrderSelfEventListeners yandexGoOrderSelfEventListeners;

    @Mock
    DeliveryTrackService deliveryTrackService;

    @Mock
    YandexGoOrderProperties yandexGoOrderProperties;

    @Test
    void shouldUseExternalOrderIdAsTrackCodeAndOrderId_whenHandleYandexGoOrderConfirmed() {
        // given
        String externalOrderId = "123";
        long deliveryServiceId = 456L;
        long yandexGoOrderId = 678L;
        String trackId = "track-id";
        when(yandexGoOrderProperties.requireDeliveryServiceId()).thenReturn(deliveryServiceId);
        YandexGoOrder yandexGoOrder = TestUtil.createStrictMock(YandexGoOrder.class);
        doReturn(yandexGoOrderId).when(yandexGoOrder).getId();
        doReturn(externalOrderId).when(yandexGoOrder).getExternalOrderId();
        doReturn(null).when(yandexGoOrder).getRealClass();
        doReturn(trackId).when(yandexGoOrder).getTrackId();
        YandexGoOrderConfirmedEvent event = new YandexGoOrderConfirmedEvent(yandexGoOrder);

        // when
        yandexGoOrderSelfEventListeners.handleYandexGoOrderConfirmed(event);

        // then
        verify(deliveryTrackService).registerDeliveryTrack(
                externalOrderId,
                trackId,
                deliveryServiceId,
                ApiVersion.DS);
    }

}
