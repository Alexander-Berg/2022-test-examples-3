package ru.yandex.market.pvz.core.domain.approve.delivery_service.handler;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientPartnerDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.PlatformClientPartnerStageHandler.BERU_PLATFORM_ID;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PlatformClientPartnerStageHandlerTest {

    private static final long DELIVERY_SERVICE_ID = 48455L;

    @MockBean
    private LMSClient lmsClient;

    private final PlatformClientPartnerStageHandler platformClientPartnerStageHandler;

    @Test
    void setupPlatformClientPartners() {
        platformClientPartnerStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));
        verify(lmsClient, times(1))
                .addOrUpdatePlatformClientPartner(PlatformClientPartnerDto.newBuilder()
                        .partnerId(DELIVERY_SERVICE_ID)
                        .platformClientId(BERU_PLATFORM_ID)
                        .status(PartnerStatus.ACTIVE)
                        .build());
    }

    @Test
    void internalLmsError() {
        when(lmsClient.addOrUpdatePlatformClientPartner(any())).thenThrow(new HttpTemplateException(500, ""));

        var params = new LmsParams(DELIVERY_SERVICE_ID, "");
        assertThatThrownBy(() -> platformClientPartnerStageHandler.handle(params))
                .isExactlyInstanceOf(HttpTemplateException.class);

        verify(lmsClient, times(1))
                .addOrUpdatePlatformClientPartner(PlatformClientPartnerDto.newBuilder()
                        .partnerId(DELIVERY_SERVICE_ID)
                        .platformClientId(BERU_PLATFORM_ID)
                        .status(PartnerStatus.ACTIVE)
                        .build());
    }
}
