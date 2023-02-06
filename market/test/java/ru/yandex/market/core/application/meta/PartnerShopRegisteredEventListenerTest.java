package ru.yandex.market.core.application.meta;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.contact.model.Contact;
import ru.yandex.market.core.delivery.LogisticPartnerService;
import ru.yandex.market.core.ds.model.DatasourceInfo;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PartnerShopRegisteredEventListenerTest extends FunctionalTest {

    @Mock
    private NotificationService notificationService;

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private ParamService paramService;

    @Autowired
    private RegionService regionService;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    private LogisticPartnerService logisticPartnerService;

    @Autowired
    private BusinessService businessService;

    private PartnerShopRegisteredEventListener listener;

    @BeforeEach
    void beforeAll() {
        reset(notificationService);

        listener = new PartnerShopRegisteredEventListener(notificationService, paramService, regionService,
                partnerTypeAwareService, logisticPartnerService, businessService);
    }

    @ParameterizedTest
    @CsvSource({
            "DROPSHIP,SUPPLIER,1,false",
            "DROPSHIP_BY_SELLER,SHOP,3,false",
            "SUPPLIER,SUPPLIER,4,false"
    })
    @DisplayName("Партнёры не должны регистрироваться в Несу")
    @DbUnitDataSet(before = "PartnerShopRegisteredEventListenerTest.dbs.before.csv")
    void testNesuRegistrationInvocationForDbs(
            ShopRole shopRole, CampaignType campaignType, long datasourceId, boolean shouldInvokeNesu
    ) {
        String internalDsName = "internal-datasource-name";

        Contact contact = new Contact();

        CampaignInfo campaignInfo = new CampaignInfo();
        campaignInfo.setDatasourceId(datasourceId);
        campaignInfo.setType(campaignType);

        DatasourceInfo datasourceInfo = new DatasourceInfo();
        datasourceInfo.setId(datasourceId);
        datasourceInfo.setInternalName(internalDsName);

        listener.onApplicationEvent(new PartnerShopRegisteredEvent(contact, campaignInfo, datasourceInfo, null));

        ArgumentCaptor<RegisterShopDto> capture = ArgumentCaptor.forClass(RegisterShopDto.class);
        verify(nesuClient, shouldInvokeNesu ? times(1) : never()).registerShop(capture.capture());

        if (shouldInvokeNesu) {
            RegisterShopDto value = capture.getValue();
            assertEquals(datasourceId, value.getId());
            assertEquals(internalDsName, value.getName());
            assertEquals(shopRole, value.getRole());
            assertNull(value.getMarketId());
            assertNull(value.getBalanceClientId());
            assertNull(value.getBalanceContractId());
            assertNull(value.getBalancePersonId());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "DROPSHIP_BY_SELLER,SHOP,3,false",
    })
    @DisplayName("В Несу склад регистрируется без префикса")
    @DbUnitDataSet(before = "PartnerShopRegisteredEventListenerTest.dbs.before.csv")
    void testNesuRegistrationWarehouseName(
            ShopRole shopRole, CampaignType campaignType, long datasourceId, boolean shouldInvokeNesu
    ) {
        String internalName = "test1";
        String internalDsName = "Склад " + "\"" + internalName + "\"";

        Contact contact = new Contact();

        CampaignInfo campaignInfo = new CampaignInfo();
        campaignInfo.setDatasourceId(datasourceId);
        campaignInfo.setType(campaignType);

        DatasourceInfo datasourceInfo = new DatasourceInfo();
        datasourceInfo.setId(datasourceId);
        datasourceInfo.setInternalName(internalDsName);

        listener.onApplicationEvent(new PartnerShopRegisteredEvent(
                contact, campaignInfo, datasourceInfo, internalName)
        );

        ArgumentCaptor<RegisterShopDto> capture = ArgumentCaptor.forClass(RegisterShopDto.class);
        verify(nesuClient, shouldInvokeNesu ? times(1) : never()).registerShop(capture.capture());

        if (shouldInvokeNesu) {
            RegisterShopDto value = capture.getValue();
            assertEquals(datasourceId, value.getId());
            assertEquals(internalName, value.getName());
            assertEquals(shopRole, value.getRole());
            assertNull(value.getMarketId());
            assertNull(value.getBalanceClientId());
            assertNull(value.getBalanceContractId());
            assertNull(value.getBalancePersonId());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "SHOP,true,95",
            "DELIVERY,true,1575269486",
            "SUPPLIER_1P,false,0"
    })
    @DisplayName("Проверка отправки приветственного письма")
    @DbUnitDataSet(before = "PartnerShopRegisteredEventListenerTest.dbs.before.csv")
    void testSendWelcomeNotification(CampaignType campaignType, boolean shouldInvokeNotification,
                                            int notificationTypeId) {
        Contact contact = new Contact();

        CampaignInfo campaignInfo = new CampaignInfo();
        campaignInfo.setDatasourceId(1);
        campaignInfo.setType(campaignType);

        DatasourceInfo datasourceInfo = new DatasourceInfo();
        datasourceInfo.setId(1);
        datasourceInfo.setInternalName("internal-datasource-name");

        listener.onApplicationEvent(new PartnerShopRegisteredEvent(contact, campaignInfo, datasourceInfo, null));

        ArgumentCaptor<NotificationSendContext> capture = ArgumentCaptor.forClass(NotificationSendContext.class);
        verify(notificationService, shouldInvokeNotification ? atLeastOnce() : never())
                .send(capture.capture());

        if (shouldInvokeNotification) {
            boolean notificationWasSend = capture.getAllValues().stream()
                    .anyMatch(n -> notificationTypeId == n.getTypeId());
            Assertions.assertTrue(notificationWasSend, "Должны отправить приветственное письмо указанного типа");
        }
    }

    @Test
    @DisplayName("В nesu не ходим, если проставлен флаг эксперимента.")
    @DbUnitDataSet(before = {
            "PartnerShopRegisteredEventListenerTest.dbs.before.csv",
            "PartnerShopRegisteredEventListenerTest.testNesuRegistrationExperiment.before.csv"
    })
    void testNesuRegistrationExperiment() {
        long datasourceId = 3;

        String internalName = "test1";
        String internalDsName = "Склад " + "\"" + internalName + "\"";

        Contact contact = new Contact();

        CampaignInfo campaignInfo = new CampaignInfo();
        campaignInfo.setDatasourceId(datasourceId);
        campaignInfo.setType(CampaignType.SHOP);

        DatasourceInfo datasourceInfo = new DatasourceInfo();
        datasourceInfo.setId(datasourceId);
        datasourceInfo.setInternalName(internalDsName);

        listener.onApplicationEvent(new PartnerShopRegisteredEvent(
                contact, campaignInfo, datasourceInfo, internalName)
        );

        // Если флаг эксперимента выставлен, то в nesu не ходим.
        verify(nesuClient, never()).registerShop(any());
    }

    @Test
    @DisplayName("Для ПВЗ никаких писем о регистрации не шлем")
    void testZeroNotificationsForTPL() {
        CampaignType.TPL_CAMPAIGNS.forEach(type -> {
            CampaignInfo campaignInfo = new CampaignInfo();
            campaignInfo.setType(type);

            listener.onApplicationEvent(
                    new PartnerShopRegisteredEvent(
                            new Contact(), campaignInfo, new DatasourceInfo(), "Я склад"
                    )
            );
        });

        verifyNoInteractions(notificationService);
        verifyNoInteractions(nesuClient);
    }

}
