package ru.yandex.market.mbi.api.controller.operation.partner.registration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.moderation.sandbox.SandboxRepository;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.partner.event.PartnerInfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerRegistrationListenersControllerTest extends FunctionalTest {

    private static final long UID = 1001001;
    private static final long PARTNER_ID = 1;
    private static final long BUSINESS_ID = 10;

    @Autowired
    private ParamService paramService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private PushApi pushApiClient;

    @Autowired
    private LogbrokerService mboPartnerExportLogbrokerService;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private SandboxRepository sandboxRepository;

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationListenersControllerTest.testNotifyPushApiParams.before.csv")
    void testNotifyPushApiParams() {
        getMbiOpenApiClient().notifyPushApiParams(UID, PARTNER_ID);
        verify(pushApiClient).settings(
                eq(PARTNER_ID),
                argThat(settings -> settings.isPartnerInterface().equals(Boolean.TRUE)),
                eq(false)
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationListenersControllerTest.testNotifyPartnerChange.before.csv")
    void testNotifyPartnerChange() {
        getMbiOpenApiClient().notifyPartnerChange(UID, PARTNER_ID);
        verify(mboPartnerExportLogbrokerService, times(2))
                .publishEvent(
                        argThat(event -> ((PartnerInfo.PartnerInfoEvent) event.getPayload())
                                .getUpdateType() == PartnerInfo.UpdateType.SERVICE_LINK
                        )
                );
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationListenersControllerTest.testNotifyPushApiParams.before.csv")
    void testNotifyCpaIsPartnerInterface() {
        getMbiOpenApiClient().notifyCpaIsPartnerInterface(UID, PARTNER_ID);

        Assertions.assertTrue(paramService.getParamBooleanValue(ParamType.IS_NEWBIE, PARTNER_ID));
        Assertions.assertEquals(
                ParamCheckStatus.SUCCESS,
                featureService.getFeature(PARTNER_ID, FeatureType.ORDER_AUTO_ACCEPT).getStatus()
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationListenersControllerTest.testCpaIsPartnerInterface.before.csv")
    void testNotifyCpaIsPartnerInterfaceDbs() {
        when(checkouterClient.shops()).thenReturn(Mockito.mock(CheckouterShopApi.class));

        getMbiOpenApiClient().notifyCpaIsPartnerInterface(UID, PARTNER_ID);
        Assertions.assertNull(sandboxRepository.load(PARTNER_ID, ShopProgram.SELF_CHECK));
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationListenersControllerTest.testNotifyCpaIsPartner.before.csv")
    void testNotifyCpaPartner() {
        CheckouterShopApi shopApi = Mockito.mock(CheckouterShopApi.class);
        when(checkouterClient.shops()).thenReturn(shopApi);
        when(shopApi.getShopData(eq(PARTNER_ID))).thenReturn(ShopMetaData.DEFAULT);
        getMbiOpenApiClient().notifyCpaPartner(UID, PARTNER_ID);
        verify(shopApi, times(2)).pushSchedules(any());
    }

}
