package ru.yandex.market.pvz.core.domain.approve.delivery_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.pvz.core.config.MarketHubFfApiConfiguration;
import ru.yandex.market.pvz.core.domain.approve.delivery_service.model.DropOffCreateParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointCommandService;
import ru.yandex.market.sc.internal.client.ScLogisticsClient;
import ru.yandex.market.sc.internal.model.DropOffDto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.DropOffCreateService.API_FORMAT;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.DropOffCreateService.API_TYPE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.DropOffCreateService.API_VERSION;

@ExtendWith(SpringExtension.class)
class DropOffCreateServiceTest {

    private static final long DROP_OFF_DS_ID = 756345L;
    private static final String TOKEN = "3cefef9b51354dc9a4a7f0fe4754c5c1534b754f087e4038a3f826563b24d76c";
    private static final long CAMPAIGN_ID = 4905858L;
    private static final long COURIER_DS_ID = 2895690L;
    private static final long LMS_ID = 475489948L;
    private static final String DROP_OFF_NAME = "Дроп офф в Чертаново";
    private static final String ADDRESS = "Москва, ул. Чертановская, д. 8";
    private static final String PARTNER_NAME = "ИП Васильев Иван Петрович";

    private static final DropOffCreateParams DROP_OFF_CREATE_PARAMS = DropOffCreateParams.builder()
            .pickupPointId(1L)
            .lmsId(String.valueOf(LMS_ID))
            .dropOffDsId(DROP_OFF_DS_ID)
            .token(TOKEN)
            .courierDsId(COURIER_DS_ID)
            .campaignId(CAMPAIGN_ID)
            .dropOffName(DROP_OFF_NAME)
            .address(ADDRESS)
            .legalPartnerName(PARTNER_NAME)
            .build();

    @Mock
    private MarketHubFfApiConfiguration marketHubFfApiConfiguration;

    @Mock
    private ApiSettingsCreator apiSettingsCreator;

    @Mock
    private PickupPointCommandService pickupPointCommandService;

    @Mock
    private ScLogisticsClient scLogisticsClient;

    private DropOffCreateService dropOffCreateService;

    @BeforeEach
    void setup() {
        when(marketHubFfApiConfiguration.getCreateOrder()).thenReturn("createOrder");
        when(marketHubFfApiConfiguration.getCancelOrder()).thenReturn("cancelOrder");
        when(marketHubFfApiConfiguration.getGetOrderHistory()).thenReturn("getOrderHistory");
        when(marketHubFfApiConfiguration.getUpdateOrderItems()).thenReturn("updateOrderItems");
        when(marketHubFfApiConfiguration.getUpdateOrder()).thenReturn("updateOrder");
        when(marketHubFfApiConfiguration.getGetOrdersStatus()).thenReturn("getOrdersStatus");
        when(marketHubFfApiConfiguration.getCreateReturnRegister()).thenReturn("getCreateReturnRegister");

        dropOffCreateService = new DropOffCreateService(marketHubFfApiConfiguration, apiSettingsCreator,
                pickupPointCommandService, scLogisticsClient);
    }

    @Test
    void createDropOff() {
        dropOffCreateService.create(DROP_OFF_CREATE_PARAMS);

        verify(apiSettingsCreator, times(1)).create(
                SettingsApiDto.newBuilder()
                        .partnerId(DROP_OFF_CREATE_PARAMS.getDropOffDsId())
                        .token(TOKEN)
                        .apiType(API_TYPE)
                        .format(API_FORMAT)
                        .version(API_VERSION)
                        .build(),
                dropOffCreateService.getApiMethods()
        );

        verify(scLogisticsClient, times(1)).createSortingCenterAsDropOff(
                DropOffDto.builder()
                        .deliveryPartnerId(DROP_OFF_CREATE_PARAMS.getDropOffDsId())
                        .address(ADDRESS)
                        .apiToken(TOKEN)
                        .campaignId(String.valueOf(CAMPAIGN_ID))
                        .partnerName(PARTNER_NAME)
                        .logisticPointId(String.valueOf(LMS_ID))
                        .dropOffName(DROP_OFF_NAME)
                        .courierDeliveryServiceId(COURIER_DS_ID)
                        .build()
        );

        verify(pickupPointCommandService, times(1))
                .makeDropOff(DROP_OFF_CREATE_PARAMS.getPickupPointId());
    }

    @Test
    void lmsError() {
        doThrow(new HttpTemplateException(500, "")).when(apiSettingsCreator).create(any(), any());

        assertThatThrownBy(() -> dropOffCreateService.create(DROP_OFF_CREATE_PARAMS))
                .isExactlyInstanceOf(HttpTemplateException.class);
        verify(apiSettingsCreator, times(1)).create(
                SettingsApiDto.newBuilder()
                        .partnerId(DROP_OFF_CREATE_PARAMS.getDropOffDsId())
                        .token(TOKEN)
                        .apiType(API_TYPE)
                        .format(API_FORMAT)
                        .version(API_VERSION)
                        .build(),
                dropOffCreateService.getApiMethods()
        );
        verify(scLogisticsClient, never()).createSortingCenterAsDropOff(any());
        verify(pickupPointCommandService, never()).makeDropOff(anyLong());
    }

    @Test
    void scError() {
        doThrow(new HttpTemplateException(500, "")).when(scLogisticsClient).createSortingCenterAsDropOff(any());

        assertThatThrownBy(() -> dropOffCreateService.create(DROP_OFF_CREATE_PARAMS))
                .isExactlyInstanceOf(HttpTemplateException.class);
        verify(apiSettingsCreator, times(1)).create(
                SettingsApiDto.newBuilder()
                        .partnerId(DROP_OFF_CREATE_PARAMS.getDropOffDsId())
                        .token(TOKEN)
                        .apiType(API_TYPE)
                        .format(API_FORMAT)
                        .version(API_VERSION)
                        .build(),
                dropOffCreateService.getApiMethods()
        );
        verify(scLogisticsClient, times(1)).createSortingCenterAsDropOff(
                DropOffDto.builder()
                        .deliveryPartnerId(DROP_OFF_CREATE_PARAMS.getDropOffDsId())
                        .address(ADDRESS)
                        .apiToken(TOKEN)
                        .campaignId(String.valueOf(CAMPAIGN_ID))
                        .partnerName(PARTNER_NAME)
                        .logisticPointId(String.valueOf(LMS_ID))
                        .dropOffName(DROP_OFF_NAME)
                        .courierDeliveryServiceId(COURIER_DS_ID)
                        .build()
        );
        verify(pickupPointCommandService, never()).makeDropOff(anyLong());
    }
}
