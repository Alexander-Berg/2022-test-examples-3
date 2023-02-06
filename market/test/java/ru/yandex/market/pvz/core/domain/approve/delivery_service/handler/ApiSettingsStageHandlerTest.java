package ru.yandex.market.pvz.core.domain.approve.delivery_service.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.pvz.core.config.PvzDsApiConfiguration;
import ru.yandex.market.pvz.core.domain.approve.delivery_service.ApiSettingsCreator;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.ApiSettingsStageHandler.API_FORMAT;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.ApiSettingsStageHandler.API_TYPE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.handler.ApiSettingsStageHandler.API_VERSION;

@ExtendWith(SpringExtension.class)
class ApiSettingsStageHandlerTest {

    private static final long DELIVERY_SERVICE_ID = 48455L;
    private static final String TOKEN = "3cefef9b51354dc9a4a7f0fe4754c5c1534b754f087e4038a3f826563b24d76c";

    @Mock
    private ApiSettingsCreator apiSettingsCreator;

    @Mock
    private PvzDsApiConfiguration pvzDsApiConfiguration;

    @Mock
    private ConfigurationProvider configurationProvider;

    private ApiSettingsStageHandler apiSettingsStageHandler;

    @BeforeEach
    void setup() {
        when(pvzDsApiConfiguration.getUpdateRecipient()).thenReturn("updateRecipient");
        when(pvzDsApiConfiguration.getUpdateOrder()).thenReturn("updateOrder");
        when(pvzDsApiConfiguration.getGetReferencePickupPoints()).thenReturn("getGetReferencePickupPoints");
        when(pvzDsApiConfiguration.getGetOrdersStatus()).thenReturn("getGetOrdersStatus");
        when(pvzDsApiConfiguration.getGetOrderHistory()).thenReturn("getGetOrderHistory");
        when(pvzDsApiConfiguration.getCreateOrder()).thenReturn("getCreateOrder");
        when(pvzDsApiConfiguration.getCancelOrder()).thenReturn("getCancelOrder");
        when(pvzDsApiConfiguration.getGetOrdersDeliveryDate()).thenReturn("getGetOrdersDeliveryDate");
        when(pvzDsApiConfiguration.getUpdateItemsInstances()).thenReturn("getUpdateItemsInstances");
        when(pvzDsApiConfiguration.getUpdateOrderItems()).thenReturn("getUpdateOrderItems");
        when(pvzDsApiConfiguration.getUpdateOrderDeliveryDate()).thenReturn("getUpdateOrderDeliveryDate");

        apiSettingsStageHandler = new ApiSettingsStageHandler(
                pvzDsApiConfiguration, apiSettingsCreator, configurationProvider
        );
        apiSettingsStageHandler.buildMethods();
    }

    @Test
    void setupApiSettingsAndMethods() {
        apiSettingsStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, TOKEN));

        verify(apiSettingsCreator, times(1))
                .create(createSettingsApiDto(), apiSettingsStageHandler.apiMethods);
    }

    @Test
    void lmsErrorWhileSetupApiSettingsAndMethods() {
        doThrow(new HttpTemplateException(500, "")).when(apiSettingsCreator).create(any(), any());

        assertThatThrownBy(() -> apiSettingsStageHandler.handle(new LmsParams(DELIVERY_SERVICE_ID, TOKEN)))
                .isExactlyInstanceOf(HttpTemplateException.class);
    }

    private SettingsApiDto createSettingsApiDto() {
        return SettingsApiDto.newBuilder()
                .partnerId(DELIVERY_SERVICE_ID)
                .apiType(API_TYPE)
                .token(TOKEN)
                .format(API_FORMAT)
                .version(API_VERSION)
                .build();
    }
}
