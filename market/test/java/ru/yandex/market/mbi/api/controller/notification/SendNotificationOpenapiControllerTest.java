package ru.yandex.market.mbi.api.controller.notification;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.market.partner.notification.client.PartnerNotificationSendException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.ds.model.DatasourceInfo;
import ru.yandex.market.core.notification.model.NotificationSupplierInfo;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.NotificationBusinessBatchRequest;
import ru.yandex.market.mbi.open.api.client.model.NotificationBusinessBatchResponse;
import ru.yandex.market.mbi.open.api.client.model.NotificationBusinessRequest;
import ru.yandex.market.mbi.open.api.client.model.NotificationBusinessResponse;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerBatchRequest;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerBatchResponse;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerRequest;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerResponse;
import ru.yandex.market.mbi.open.api.client.model.NotificationResponseData;
import ru.yandex.market.mbi.open.api.client.model.NotificationTemplateData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbi.open.api.client.model.NotificationResponseData.StatusEnum;

@DbUnitDataSet(before = "SendNotificationOpenapiControllerTest.before.csv")
class SendNotificationOpenapiControllerTest extends FunctionalTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    @DisplayName("Проверка, что возвращаются ошибки, если не найден бизнес или тип нотификации")
    void sendNotificationBusinessBatchNotFound() {
        PartnerNotificationApiServiceTest.setUpClient(partnerNotificationClient, () -> {
            throw new PartnerNotificationSendException("Notification type not found: 1", 400, List.of());
        });

        NotificationBusinessRequest request1 =
                new NotificationBusinessRequest()
                        .businessId(3L)
                        .templateData(
                                new NotificationTemplateData()
                                        .notificationType(1543402172)
                        );

        NotificationBusinessRequest request2 =
                new NotificationBusinessRequest()
                        .businessId(1L)
                        .templateData(
                                new NotificationTemplateData()
                                        .notificationType(1)
                        );

        NotificationBusinessBatchRequest requestBatch =
                new NotificationBusinessBatchRequest()
                        .batchRequest(
                                List.of(request1, request2)
                        );

        NotificationBusinessBatchResponse response =
                getMbiOpenApiClient().sendNotificationBusinessBatch(requestBatch);

        assertThat(response.getIsSuccess()).isFalse();
        assertThat(response.getBatchResponse())
                .containsExactlyInAnyOrder(
                        new NotificationBusinessResponse()
                                .request(request1)
                                .data(
                                        new NotificationResponseData()
                                                .status(StatusEnum.WARN)
                                                .message("Business not found: 3")
                                ),
                        new NotificationBusinessResponse()
                                .request(request2)
                                .data(
                                        new NotificationResponseData()
                                                .status(StatusEnum.WARN)
                                                .message("Notification type not found: 1")
                                )
                );
    }

    @Test
    @DisplayName("Успешная отправка уведомлений списку бизнесов")
    @SuppressWarnings("unchecked")
    void sendNotificationBusinessBatchSuccess() {
        NotificationBusinessRequest request1 =
                new NotificationBusinessRequest()
                        .businessId(1L)
                        .templateData(
                                new NotificationTemplateData()
                                        .notificationType(1543402172)
                        );

        NotificationBusinessRequest request2 =
                new NotificationBusinessRequest()
                        .businessId(2L)
                        .templateData(
                                new NotificationTemplateData()
                                        .notificationType(1636552194)
                                        .addNotificationDataItem("<success>0</success>")
                        );

        NotificationBusinessBatchRequest requestBatch =
                new NotificationBusinessBatchRequest()
                        .batchRequest(
                                List.of(request1, request2)
                        );

        NotificationBusinessBatchResponse response = getMbiOpenApiClient()
                .sendNotificationBusinessBatch(requestBatch);

        assertThat(response.getIsSuccess()).isTrue();

        assertThat(response.getBatchResponse())
                .containsExactlyInAnyOrder(
                        new NotificationBusinessResponse()
                                .request(request1)
                                .data(
                                        new NotificationResponseData()
                                                .status(StatusEnum.OK)
                                                .message("OK")
                                                .groupId(1L)
                                ),
                        new NotificationBusinessResponse()
                                .request(request2)
                                .data(
                                        new NotificationResponseData()
                                                .status(StatusEnum.OK)
                                                .message("OK")
                                                .groupId(2L)
                                )
                );

        ArgumentCaptor<NotificationSendContext> argument =
                ArgumentCaptor.forClass(NotificationSendContext.class);

        verify(notificationService, times(2)).send(argument.capture());

        List<NotificationSendContext> sendContexts = argument.getAllValues();

        assertThat(sendContexts)
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("data")
                .containsExactlyInAnyOrder(
                        new NotificationSendContext.Builder()
                                .setBusinessId(1L)
                                .setTypeId(1543402172)
                                .setData(List.of())
                                .build(),
                        new NotificationSendContext.Builder()
                                .setBusinessId(2L)
                                .setTypeId(1636552194)
                                .setData(List.of())
                                .build()

                );

        for (NotificationSendContext context : sendContexts) {
            if (context.getBusinessId() == 1) {
                assertThat(context.getData())
                        .containsExactlyInAnyOrder(
                                new NamedContainer(
                                        SendNotificationOpenapiController.NOTIFICATION_DATA_BUSINESS_ID,
                                        1L),
                                new NamedContainer(
                                        SendNotificationOpenapiController.NOTIFICATION_DATA_BUSINESS_NAME,
                                        "Business1"),
                                new NamedContainer(
                                        SendNotificationOpenapiController
                                                .NOTIFICATION_DATA_BUSINESS_ENVIRONMENT,
                                        "development")
                        );
            } else {
                assertThat(context.getData())
                        .hasSize(4)
                        .contains(
                                new NamedContainer(
                                        SendNotificationOpenapiController.NOTIFICATION_DATA_BUSINESS_ID,
                                        2L),
                                new NamedContainer(
                                        SendNotificationOpenapiController.NOTIFICATION_DATA_BUSINESS_NAME,
                                        "Business2"),
                                new NamedContainer(
                                        SendNotificationOpenapiController
                                                .NOTIFICATION_DATA_BUSINESS_ENVIRONMENT,
                                        "development")
                        );
                for (Object obj : context.getData()) {
                    if (obj instanceof Element) {
                        Element element = (Element) obj;
                        assertThat(element.getName()).isEqualTo("success");
                        assertThat(element.getText()).isEqualTo("0");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Успешная отправка уведомлений списку партнеров")
    @SuppressWarnings("unchecked")
    void sendNotificationPartnerBatchSuccess() {
        NotificationPartnerRequest request1 =
                new NotificationPartnerRequest()
                        .partnerId(3L)
                        .templateData(
                                new NotificationTemplateData()
                                        .notificationType(1543402172)
                        );

        NotificationPartnerRequest request2 =
                new NotificationPartnerRequest()
                        .partnerId(4L)
                        .templateData(
                                new NotificationTemplateData()
                                        .notificationType(1636552194)
                                        .addNotificationDataItem("<success>0</success>")
                        );

        NotificationPartnerBatchRequest requestBatch =
                new NotificationPartnerBatchRequest()
                        .needAdditionalData(true)
                        .batchRequest(
                                List.of(request1, request2)
                        );

        NotificationPartnerBatchResponse response = getMbiOpenApiClient()
                .sendNotificationPartnerBatch(requestBatch);

        assertThat(response.getIsSuccess()).isTrue();
        assertThat(response.getBatchResponse())
                .containsExactlyInAnyOrder(
                        new NotificationPartnerResponse()
                                .request(request1)
                                .data(
                                        new NotificationResponseData()
                                                .status(StatusEnum.OK)
                                                .message("OK")
                                                .groupId(1L)
                                ),
                        new NotificationPartnerResponse()
                                .request(request2)
                                .data(
                                        new NotificationResponseData()
                                                .status(StatusEnum.OK)
                                                .message("OK")
                                                .groupId(2L)
                                )
                );

        ArgumentCaptor<NotificationSendContext> argument =
                ArgumentCaptor.forClass(NotificationSendContext.class);

        verify(notificationService, times(2)).send(argument.capture());

        List<NotificationSendContext> sendContexts = argument.getAllValues();

        assertThat(sendContexts)
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("data")
                .containsExactlyInAnyOrder(
                        new NotificationSendContext.Builder()
                                .setShopId(3L)
                                .setTypeId(1543402172)
                                .setData(List.of())
                                .build(),
                        new NotificationSendContext.Builder()
                                .setShopId(4L)
                                .setTypeId(1636552194)
                                .setData(List.of())
                                .build()

                );

        DatasourceInfo datasourceInfo = new DatasourceInfo();
        datasourceInfo.setId(3L);
        datasourceInfo.setInternalName("SHOP1");
        datasourceInfo.setManagerId(100L);
        datasourceInfo.setPlacementTypes(new ArrayList<>());

        for (NotificationSendContext context : sendContexts) {
            if (context.getShopId() == 3) {
                assertThat(context.getData())
                        .contains(
                                new CampaignInfo(555521, 3, 201, 1015, CampaignType.SHOP)
                        );

                for (Object obj : context.getData()) {
                    if (obj instanceof DatasourceInfo) {
                        DatasourceInfo ds = (DatasourceInfo) obj;
                        assertThat(ds.getId()).isEqualTo(3);
                        assertThat(ds.getInternalName()).isEqualTo("SHOP1");
                        assertThat(ds.getManagerId()).isEqualTo(100);
                    }
                }
            } else {
                assertThat(context.getData())
                        .contains(
                                new CampaignInfo(555524, 4, 204, 1015, CampaignType.SUPPLIER)
                        );

                for (Object obj : context.getData()) {
                    if (obj instanceof NotificationSupplierInfo) {
                        NotificationSupplierInfo ns = (NotificationSupplierInfo) obj;
                        assertThat(ns.getId()).isEqualTo(4);
                        assertThat(ns.getName()).isEqualTo("поставщик");
                        assertThat(ns.getCampaignId()).isEqualTo(555524);
                    } else if (obj instanceof Element) {
                        Element element = (Element) obj;
                        assertThat(element.getName()).isEqualTo("success");
                        assertThat(element.getText()).isEqualTo("0");
                    }
                }
            }
        }
    }


    @Test
    @DisplayName("Успешная отправка уведомлений списку бизнесов с параметром emailList")
    @SuppressWarnings("unchecked")
    void sendNotificationBusinessBatchWithEmailListSuccess() {
        NotificationBusinessRequest request1 =
                new NotificationBusinessRequest()
                        .businessId(1L)
                        .templateData(
                                new NotificationTemplateData()
                                        .notificationType(1618813719)
                                        .addNotificationDataItem("<login>test</login>")
                                        .addNotificationDataItem("<invitation-id>invitation1</invitation-id>")
                        )
                        .addEmailListItem("test@yandex.ru");

        NotificationBusinessRequest request2 =
                new NotificationBusinessRequest()
                        .businessId(2L)
                        .templateData(
                                new NotificationTemplateData()
                                        .notificationType(1618813719)
                                        .addNotificationDataItem("<login>test2</login>")
                                        .addNotificationDataItem("<invitation-id>invitation2</invitation-id>")
                        )
                        .addEmailListItem("test2@yandex.ru")
                        .addEmailListItem("test1@yandex.ru");

        NotificationBusinessBatchRequest requestBatch =
                new NotificationBusinessBatchRequest()
                        .batchRequest(
                                List.of(request1, request2)
                        );

        NotificationBusinessBatchResponse response = getMbiOpenApiClient()
                .sendNotificationBusinessBatch(requestBatch);

        assertThat(response.getIsSuccess()).isTrue();

        assertThat(response.getBatchResponse())
                .containsExactlyInAnyOrder(
                        new NotificationBusinessResponse()
                                .request(request1)
                                .data(
                                        new NotificationResponseData()
                                                .status(StatusEnum.OK)
                                                .message("OK")
                                                .groupId(1L)
                                ),
                        new NotificationBusinessResponse()
                                .request(request2)
                                .data(
                                        new NotificationResponseData()
                                                .status(StatusEnum.OK)
                                                .message("OK")
                                                .groupId(2L)
                                )
                );

        ArgumentCaptor<NotificationSendContext> argument =
                ArgumentCaptor.forClass(NotificationSendContext.class);

        verify(notificationService, times(2)).send(argument.capture());

        List<NotificationSendContext> sendContexts = argument.getAllValues();

        assertThat(sendContexts)
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("data")
                .containsExactlyInAnyOrder(
                        new NotificationSendContext.Builder()
                                .setBusinessId(1L)
                                .setTypeId(1618813719)
                                .setData(List.of())
                                .setRecepientEmail("test@yandex.ru")
                                .build(),
                        new NotificationSendContext.Builder()
                                .setBusinessId(2L)
                                .setTypeId(1618813719)
                                .setData(List.of())
                                .setRecepientEmail("test2@yandex.ru")
                                .build()
                );

        for (NotificationSendContext context : sendContexts) {
            if (context.getBusinessId() == 1) {
                assertThat(context.getData())
                        .hasSize(5)
                        .contains(
                                new NamedContainer(
                                        SendNotificationOpenapiController.NOTIFICATION_DATA_BUSINESS_ID,
                                        1L),
                                new NamedContainer(
                                        SendNotificationOpenapiController.NOTIFICATION_DATA_BUSINESS_NAME,
                                        "Business1"),
                                new NamedContainer(
                                        SendNotificationOpenapiController
                                                .NOTIFICATION_DATA_BUSINESS_ENVIRONMENT,
                                        "development")
                        );
                for (Object obj : context.getData()) {
                    if (obj instanceof Element) {
                        Element element = (Element) obj;
                        if (element.getName().equals("login")) {
                            assertThat(element.getText()).isEqualTo("test");
                        } else {
                            assertThat(element.getName()).isEqualTo("invitation-id");
                            assertThat(element.getText()).isEqualTo("invitation1");
                        }
                    }
                }
            } else {
                assertThat(context.getData())
                        .hasSize(5)
                        .contains(
                                new NamedContainer(
                                        SendNotificationOpenapiController.NOTIFICATION_DATA_BUSINESS_ID,
                                        2L),
                                new NamedContainer(
                                        SendNotificationOpenapiController.NOTIFICATION_DATA_BUSINESS_NAME,
                                        "Business2"),
                                new NamedContainer(
                                        SendNotificationOpenapiController
                                                .NOTIFICATION_DATA_BUSINESS_ENVIRONMENT,
                                        "development")
                        );
                for (Object obj : context.getData()) {
                    if (obj instanceof Element) {
                        Element element = (Element) obj;
                        if (element.getName().equals("login")) {
                            assertThat(element.getText()).isEqualTo("test2");
                        } else {
                            assertThat(element.getName()).isEqualTo("invitation-id");
                            assertThat(element.getText()).isEqualTo("invitation2");
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Успешная отправка уведомлений списку бизнесов с пустым параметром emailList")
    @SuppressWarnings("unchecked")
    void sendNotificationBusinessBatchWithEmptyEmailListSuccess() {
        NotificationBusinessRequest request1 =
                new NotificationBusinessRequest()
                        .businessId(1L)
                        .templateData(
                                new NotificationTemplateData()
                                        .notificationType(1618813719)
                                        .addNotificationDataItem("<login>test</login>")
                                        .addNotificationDataItem("<invitation-id>invitation1</invitation-id>")
                        )
                        .emailList(List.of());


        NotificationBusinessBatchRequest requestBatch =
                new NotificationBusinessBatchRequest()
                        .batchRequest(
                                List.of(request1)
                        );

        NotificationBusinessBatchResponse response = getMbiOpenApiClient()
                .sendNotificationBusinessBatch(requestBatch);

        assertThat(response.getIsSuccess()).isTrue();

        assertThat(response.getBatchResponse())
                .containsExactlyInAnyOrder(
                        new NotificationBusinessResponse()
                                .request(request1)
                                .data(
                                        new NotificationResponseData()
                                                .status(StatusEnum.OK)
                                                .message("OK")
                                                .groupId(1L)
                                )
                );

        ArgumentCaptor<NotificationSendContext> argument =
                ArgumentCaptor.forClass(NotificationSendContext.class);

        verify(notificationService, times(1)).send(argument.capture());

        List<NotificationSendContext> sendContexts = argument.getAllValues();

        assertThat(sendContexts)
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("data")
                .containsExactlyInAnyOrder(
                        new NotificationSendContext.Builder()
                                .setBusinessId(1L)
                                .setTypeId(1618813719)
                                .setData(List.of())
                                .build()
                );

        for (NotificationSendContext context : sendContexts) {
            if (context.getBusinessId() == 1) {
                assertThat(context.getData())
                        .hasSize(5)
                        .contains(
                                new NamedContainer(
                                        SendNotificationOpenapiController.NOTIFICATION_DATA_BUSINESS_ID,
                                        1L),
                                new NamedContainer(
                                        SendNotificationOpenapiController.NOTIFICATION_DATA_BUSINESS_NAME,
                                        "Business1"),
                                new NamedContainer(
                                        SendNotificationOpenapiController
                                                .NOTIFICATION_DATA_BUSINESS_ENVIRONMENT,
                                        "development")
                        );
                for (Object obj : context.getData()) {
                    if (obj instanceof Element) {
                        Element element = (Element) obj;
                        if (element.getName().equals("login")) {
                            assertThat(element.getText()).isEqualTo("test");
                        } else {
                            assertThat(element.getName()).isEqualTo("invitation-id");
                            assertThat(element.getText()).isEqualTo("invitation1");
                        }
                    }
                }
            }
        }
    }
}
