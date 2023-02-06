package ru.yandex.market.pvz.core.domain.logbroker.crm;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.client.crm.dto.CrmPayloadDto;
import ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus;
import ru.yandex.market.pvz.core.TestUtils;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointParams;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointQueryService;
import ru.yandex.market.pvz.core.domain.logbroker.crm.consume.CrmLogbrokerEventHandler;
import ru.yandex.market.pvz.core.domain.logbroker.crm.consume.PrePickupPointEventListener;
import ru.yandex.market.pvz.core.domain.logbroker.crm.consume.pre_pickup_point.CrmPrePickupPointStatusChangeEventHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrePickupPointEventListenerTest {

    private static final long PRE_PICKUP_POINT_ID = 2542;

    @Test
    void test() {
        CrmPrePickupPointQueryService queryService = mock(CrmPrePickupPointQueryService.class, RETURNS_DEEP_STUBS);
        when(queryService.getById(PRE_PICKUP_POINT_ID)).thenReturn(CrmPrePickupPointParams.builder()
                .status(PrePickupPointApproveStatus.CHECKING)
                .build());

        CrmPrePickupPointStatusChangeEventHandler invalidHandler =
                mock(CrmPrePickupPointStatusChangeEventHandler.class);

        CrmPrePickupPointStatusChangeEventHandler validHandler =
                mock(CrmPrePickupPointStatusChangeEventHandler.class);

        when(invalidHandler.getTargetStatus()).thenReturn(PrePickupPointApproveStatus.REJECTED);
        when(validHandler.getTargetStatus()).thenReturn(PrePickupPointApproveStatus.APPROVED);

        PrePickupPointEventListener listener = new PrePickupPointEventListener(
                List.of(invalidHandler, validHandler)
        );

        CrmLogbrokerEventHandler handler = new CrmLogbrokerEventHandler(List.of(listener));
        CrmPayloadDto<?> dto = handler.parse(
                TestUtils.getFileContent("crm/pre_pickup_point_logbroker_event.json"));

        handler.handle(dto);

        verify(invalidHandler, times(0)).handle(any());
        verify(validHandler, times(1)).handle(any());
    }

}
