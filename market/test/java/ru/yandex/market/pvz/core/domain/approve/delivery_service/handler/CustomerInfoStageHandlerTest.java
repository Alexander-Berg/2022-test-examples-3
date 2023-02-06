package ru.yandex.market.pvz.core.domain.approve.delivery_service.handler;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.CustomerInfoStageHandler.PARTNER_CUSTOMER_INFO_ID;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CustomerInfoStageHandlerTest {

    private static final long DELIVERY_SERVICE_ID = 48455L;

    @MockBean
    private LMSClient lmsClient;

    private final CustomerInfoStageHandler customerInfoStageHandler;

    @Test
    void setupCustomerInfo() {
        customerInfoStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, ""));

        verify(lmsClient, times(1))
                .setCustomerInfoToPartner(DELIVERY_SERVICE_ID, PARTNER_CUSTOMER_INFO_ID);
    }

    @Test
    void customerInfoNotFound() {
        when(lmsClient.setCustomerInfoToPartner(DELIVERY_SERVICE_ID, PARTNER_CUSTOMER_INFO_ID))
                .thenThrow(new HttpTemplateException(404, ""));

        assertThatThrownBy(() -> customerInfoStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, "")))
                .isExactlyInstanceOf(HttpTemplateException.class);

        verify(lmsClient, times(1))
                .setCustomerInfoToPartner(DELIVERY_SERVICE_ID, PARTNER_CUSTOMER_INFO_ID);
    }

    @Test
    void lmsInternalError() {
        when(lmsClient.setCustomerInfoToPartner(DELIVERY_SERVICE_ID, PARTNER_CUSTOMER_INFO_ID))
                .thenThrow(new HttpTemplateException(500, ""));

        assertThatThrownBy(() -> customerInfoStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, "")))
                .isExactlyInstanceOf(HttpTemplateException.class);

        verify(lmsClient, times(1))
                .setCustomerInfoToPartner(DELIVERY_SERVICE_ID, PARTNER_CUSTOMER_INFO_ID);
    }
}
