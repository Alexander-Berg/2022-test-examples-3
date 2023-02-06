package ru.yandex.market.pvz.core.domain.approve.delivery_service.handler;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PartnerExternalParamStageHandler.EXTERNAL_PARAMS;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerExternalParamStageHandlerTest {

    private static final long DELIVERY_SERVICE_ID = 48455L;

    @MockBean
    private LMSClient lmsClient;

    private final PartnerExternalParamStageHandler partnerExternalParamStageHandler;

    @Test
    void setupExternalParams() {
        partnerExternalParamStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));

        verify(lmsClient, times(EXTERNAL_PARAMS.size()))
                .addOrUpdatePartnerExternalParam(eq(DELIVERY_SERVICE_ID), any());

        verify(lmsClient).addOrUpdatePartnerExternalParam(
            DELIVERY_SERVICE_ID,
            new PartnerExternalParamRequest(PartnerExternalParamType.IS_MULTIPLACES_SUPPORTED, "1")
        );
    }

    @Test
    void externalLmsError() {
        when(lmsClient.addOrUpdatePartnerExternalParam(anyLong(), any())).thenThrow(new HttpTemplateException(500, ""));

        assertThatThrownBy(() -> partnerExternalParamStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, "")));

        verify(lmsClient, times(1))
                .addOrUpdatePartnerExternalParam(eq(DELIVERY_SERVICE_ID), any());
    }
}
