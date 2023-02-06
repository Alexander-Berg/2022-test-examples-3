package ru.yandex.market.mbi.api.controller.notification;

import java.util.List;

import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.market.partner.notification.client.PartnerNotificationSendException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.NotificationToBusinessBatchRequest;
import ru.yandex.market.mbi.open.api.client.model.NotificationToBusinessBatchResponse;
import ru.yandex.market.mbi.open.api.client.model.NotificationToBusinessRequest;
import ru.yandex.market.mbi.open.api.client.model.NotificationToBusinessResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbi.open.api.client.model.NotificationToBusinessResponse.StatusEnum;

@DbUnitDataSet(before = "SendNotificationToBusinessControllerTest.before.csv")
class SendNotificationToBusinessControllerTest extends FunctionalTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    @DisplayName("Проверка, что возвращаются ошибки, если не найден бизнес или тип нотификации")
    void testNotFound() {
        PartnerNotificationApiServiceTest.setUpClient(partnerNotificationClient, () -> {
            throw new PartnerNotificationSendException("Notification type not found: 1", 400, List.of());
        });

        NotificationToBusinessRequest request1 =
                new NotificationToBusinessRequest()
                        .businessId(3L)
                        .notificationType(1543402172);

        NotificationToBusinessRequest request2 =
                new NotificationToBusinessRequest()
                        .businessId(1L)
                        .notificationType(1);

        NotificationToBusinessBatchRequest requestBatch =
                new NotificationToBusinessBatchRequest()
                        .batchRequest(
                                List.of(request1, request2)
                        );

        NotificationToBusinessBatchResponse response =
                getMbiOpenApiClient().sendNotificationToBusinessBatch(requestBatch);

        assertThat(response.getIsSuccess()).isFalse();
        assertThat(response.getBatchResponse())
                .containsExactlyInAnyOrder(
                        new NotificationToBusinessResponse()
                                .request(request1)
                                .status(StatusEnum.WARN)
                                .message("Business not found: 3"),
                        new NotificationToBusinessResponse()
                                .request(request2)
                                .status(StatusEnum.WARN)
                                .message("Notification type not found: 1")

                );
    }

    @Test
    @DisplayName("Успешная отправка уведомлений")
    @SuppressWarnings("unchecked")
    void testSuccess() {
        NotificationToBusinessRequest request1 =
                new NotificationToBusinessRequest()
                        .businessId(1L)
                        .notificationType(1543402172);

        NotificationToBusinessRequest request2 =
                new NotificationToBusinessRequest()
                        .businessId(2L)
                        .notificationType(1636552194)
                        .addNotificationDataItem("<success>0</success>");

        NotificationToBusinessBatchRequest requestBatch =
                new NotificationToBusinessBatchRequest()
                        .batchRequest(
                                List.of(request1, request2)
                        );

        NotificationToBusinessBatchResponse response = getMbiOpenApiClient()
                .sendNotificationToBusinessBatch(requestBatch);

        assertThat(response.getIsSuccess()).isTrue();
        assertThat(response.getBatchResponse())
                .containsExactlyInAnyOrder(
                        new NotificationToBusinessResponse()
                                .request(request1)
                                .status(StatusEnum.OK)
                                .message("OK")
                                .groupId(1L),
                        new NotificationToBusinessResponse()
                                .request(request2)
                                .status(StatusEnum.OK)
                                .message("OK")
                                .groupId(2L)

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
                                        SendNotificationToBusinessController.NOTIFICATION_DATA_BUSINESS_ID,
                                        1L),
                                new NamedContainer(
                                        SendNotificationToBusinessController.NOTIFICATION_DATA_BUSINESS_NAME,
                                        "Business1"),
                                new NamedContainer(
                                        SendNotificationToBusinessController
                                                .NOTIFICATION_DATA_BUSINESS_ENVIRONMENT,
                                        "development")
                        );
            } else {
                assertThat(context.getData())
                        .hasSize(4)
                        .contains(
                                new NamedContainer(
                                        SendNotificationToBusinessController.NOTIFICATION_DATA_BUSINESS_ID,
                                        2L),
                                new NamedContainer(
                                        SendNotificationToBusinessController.NOTIFICATION_DATA_BUSINESS_NAME,
                                        "Business2"),
                                new NamedContainer(
                                        SendNotificationToBusinessController
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
}
