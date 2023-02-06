package ru.yandex.market.pvz.core.domain.logbroker.crm;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.client.crm.dto.CrmPayloadDto;
import ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus;
import ru.yandex.market.pvz.core.TestUtils;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerParams;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.logbroker.crm.consume.CrmLogbrokerEventHandler;
import ru.yandex.market.pvz.core.domain.logbroker.crm.consume.PreLegalPartnerEventListener;
import ru.yandex.market.pvz.core.domain.logbroker.crm.consume.pre_legal_partner.PreLegalPartnerStatusChangeEventHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreLegalPartnerEventListenerTest {

    private static final long PRE_PICKUP_POINT_ID = 2542;

    @Test
    void test() {
        PreLegalPartnerQueryService queryService = mock(PreLegalPartnerQueryService.class, RETURNS_DEEP_STUBS);
        when(queryService.getById(PRE_PICKUP_POINT_ID)).thenReturn(PreLegalPartnerParams.builder()
                .approveStatus(PreLegalPartnerApproveStatus.CHECKING)
                .build());

        PreLegalPartnerStatusChangeEventHandler invalidHandler = mock(PreLegalPartnerStatusChangeEventHandler.class);
        PreLegalPartnerStatusChangeEventHandler validHandler = mock(PreLegalPartnerStatusChangeEventHandler.class);

        when(invalidHandler.getTargetStatus()).thenReturn(PreLegalPartnerApproveStatus.REJECTED);
        when(validHandler.getTargetStatus()).thenReturn(PreLegalPartnerApproveStatus.APPROVED);

        PreLegalPartnerEventListener listener = new PreLegalPartnerEventListener(
                List.of(invalidHandler, validHandler)
        );

        CrmLogbrokerEventHandler handler = new CrmLogbrokerEventHandler(List.of(listener));
        CrmPayloadDto<?> dto = handler.parse(
                TestUtils.getFileContent("crm/pre_legal_partner_logbroker_event.json"));

        handler.handle(dto);

        verify(invalidHandler, times(0)).handle(any());
        verify(validHandler, times(1)).handle(any());
    }

}
