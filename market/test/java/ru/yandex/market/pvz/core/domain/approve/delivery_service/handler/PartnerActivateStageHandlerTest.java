package ru.yandex.market.pvz.core.domain.approve.delivery_service.handler;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.UpdatePartnerDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerActivateStageHandlerTest {

    private static final long DELIVERY_SERVICE_ID = 48455L;

    @MockBean
    private LMSClient lmsClient;

    private final PartnerActivateStageHandler partnerActivateStageHandler;

    @Test
    void activatePartner() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
                .id(DELIVERY_SERVICE_ID)
                .marketId(DELIVERY_SERVICE_ID + 1000)
                .build();
        when(lmsClient.getPartner(DELIVERY_SERVICE_ID)).thenReturn(Optional.of(partnerResponse));

        partnerActivateStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));

        verify(lmsClient, times(1))
                .updatePartner(DELIVERY_SERVICE_ID, UpdatePartnerDto.fromResponse(partnerResponse)
                        .status(PartnerStatus.ACTIVE)
                        .build());
    }

    @Test
    void tryToActivateNotExistentPartner() {
        when(lmsClient.getPartner(DELIVERY_SERVICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> partnerActivateStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, "")))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void partnerHasInvalidInitialMarketId() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
                .id(DELIVERY_SERVICE_ID)
                .marketId(DELIVERY_SERVICE_ID)
                .build();
        when(lmsClient.getPartner(DELIVERY_SERVICE_ID)).thenReturn(Optional.of(partnerResponse));

        partnerActivateStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));

        verify(lmsClient, never()).updatePartner(anyLong(), any());
    }

    @Test
    void partnerHasInvalidZeroMarketId() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
                .id(DELIVERY_SERVICE_ID)
                .marketId(0L)
                .build();
        when(lmsClient.getPartner(DELIVERY_SERVICE_ID)).thenReturn(Optional.of(partnerResponse));

        partnerActivateStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));

        verify(lmsClient, never()).updatePartner(anyLong(), any());
    }

    @Test
    void partnerHasInvalidNullMarketId() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
                .id(DELIVERY_SERVICE_ID)
                .build();
        when(lmsClient.getPartner(DELIVERY_SERVICE_ID)).thenReturn(Optional.of(partnerResponse));

        partnerActivateStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));

        verify(lmsClient, never()).updatePartner(anyLong(), any());
    }

    @Test
    void lmsInternalErrorOnGet() {
        when(lmsClient.getPartner(DELIVERY_SERVICE_ID)).thenThrow(new HttpTemplateException(500, ""));

        assertThatThrownBy(() -> partnerActivateStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, "")))
                .isExactlyInstanceOf(HttpTemplateException.class);

        verify(lmsClient, never()).updatePartner(anyLong(), any());
    }

    @Test
    void lmsInternalErrorOnPartnerUpdate() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
                .id(DELIVERY_SERVICE_ID)
                .marketId(DELIVERY_SERVICE_ID + 1000)
                .build();
        when(lmsClient.getPartner(DELIVERY_SERVICE_ID)).thenReturn(Optional.of(partnerResponse));
        when(lmsClient.updatePartner(anyLong(), any())).thenThrow(new HttpTemplateException(500, ""));

        assertThatThrownBy(() -> partnerActivateStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, "")))
                .isExactlyInstanceOf(HttpTemplateException.class);

        verify(lmsClient, times(1))
                .updatePartner(DELIVERY_SERVICE_ID, UpdatePartnerDto.fromResponse(partnerResponse)
                        .status(PartnerStatus.ACTIVE)
                        .build());
    }
}
