package ru.yandex.market.ff.dbqueue.service;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.ff.model.dbqueue.RegisterStatusTrackingPayload;
import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.DeliveryTrackerService;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterStatusTrackingProcessingServiceTest {

    private RegisterStatusTrackingProcessingService registerStatusTrackingProcessingService;

    @Mock
    ShopRequestFetchingService shopRequestFetchingService;

    @Mock
    DeliveryTrackerService deliveryTrackerService;

    @Mock
    RequestSubTypeService requestSubTypeService;

    @BeforeEach
    void init() {
        registerStatusTrackingProcessingService = new RegisterStatusTrackingProcessingService(
                shopRequestFetchingService,
                deliveryTrackerService,
                requestSubTypeService
        );
    }

    @Test
    void testDontRegisterTrackCode() {
        when(requestSubTypeService.getEntityByRequestTypeAndSubtype(any()))
                .thenReturn(buildRequestSubType(false));
        when(shopRequestFetchingService.getRequestOrThrow(anyLong()))
                .thenReturn(new ShopRequest());

        registerStatusTrackingProcessingService.processPayload(new RegisterStatusTrackingPayload(1L));

        Mockito.verify(deliveryTrackerService, Mockito.never()).registerTrack(any());
    }

    @Test
    void testRegisterTrackCode() {
        when(requestSubTypeService.getEntityByRequestTypeAndSubtype(any()))
                .thenReturn(buildRequestSubType(true));
        when(shopRequestFetchingService.getRequestOrThrow(anyLong()))
                .thenReturn(new ShopRequest());

        registerStatusTrackingProcessingService.processPayload(new RegisterStatusTrackingPayload(1L));

        Mockito.verify(deliveryTrackerService, Mockito.times(1)).registerTrack(any());
    }

    private RequestSubTypeEntity buildRequestSubType(boolean registerTrackCode) {
        var result = new RequestSubTypeEntity();
        result.setRegisterTrackCode(registerTrackCode);
        return result;
    }
}
