package ru.yandex.market.logistics.nesu.jobs.consumer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.message.model.MessageRecipients;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.SendNotificationPayload;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.notification.SendNotificationResponse;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DatabaseSetup("/service/shop/prepare_database.xml")
@DisplayName("Отправка уведомлений магазинам")
class SendNotificationToShopConsumerTest extends AbstractContextualTest {

    private static final Instant NOW = Instant.parse("2021-06-10T14:00:00.00Z");
    private static final Long DAAS_SHOP_ID = 2L;
    private static final Long DROPSHIP_SHOP_ID = 3L;
    private static final Integer NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_DATA = "data";

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private SendNotificationToShopConsumer sendNotificationToShopConsumer;

    @RegisterExtension
    final BackLogCaptor backLogCaptor = new BackLogCaptor();

    @BeforeEach
    void setup() {
        clock.setFixed(NOW, ZoneId.systemDefault());
        when(mbiApiClient.sendMessageToShop(DAAS_SHOP_ID, NOTIFICATION_ID, NOTIFICATION_DATA))
            .thenReturn("MessageId = 123");
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mbiApiClient);
    }

    @Test
    @DisplayName("Успешная отправка уведомления DAAS-магазину")
    @ExpectedDatabase(
        value = "/jobs/consumer/send_notification_to_shop/after/daas_shop_notification.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void sendNotificationToDaasShopSuccess() {
        sendNotificationToShopConsumer.execute(getTask(DAAS_SHOP_ID));

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t"
                + "format=plain\t"
                + "payload=Sent notification to shop with data = data\t"
                + "request_id=requestId\t"
                + "tags=NOTIFICATION\t"
                + "extra_keys=notificationGroupId,shopId,notificationTemplateId\t"
                + "extra_values=123,2,1"
        );
        verify(mbiApiClient).sendMessageToShop(DAAS_SHOP_ID, NOTIFICATION_ID, NOTIFICATION_DATA);
    }

    @Test
    @DisplayName("Успешная отправка уведомления DROPSHIP-магазину")
    @ExpectedDatabase(
        value = "/jobs/consumer/send_notification_to_shop/after/dropship_shop_notification.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void sendNotificationToDropshipShopSuccess() {
        when(mbiApiClient.sendMessageToSupplier(DROPSHIP_SHOP_ID, NOTIFICATION_ID, NOTIFICATION_DATA))
            .thenReturn(new SendNotificationResponse(123L));

        sendNotificationToShopConsumer.execute(getTask(DROPSHIP_SHOP_ID));

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t"
                + "format=plain\t"
                + "payload=Sent notification to shop with data = data\t"
                + "request_id=requestId\t"
                + "tags=NOTIFICATION\t"
                + "extra_keys=notificationGroupId,shopId,notificationTemplateId\t"
                + "extra_values=123,3,1"
        );
        verify(mbiApiClient).sendMessageToSupplier(DROPSHIP_SHOP_ID, NOTIFICATION_ID, NOTIFICATION_DATA);
    }

    @Test
    @DisplayName("Магазин не найден, mbi-api клиент не вызывается, запись в базу не сохраняется")
    @ExpectedDatabase(
        value = "/jobs/consumer/send_notification_to_shop/after/shop_notification_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void sendNotificationToShopErrorShopNotFound() {
        sendNotificationToShopConsumer.execute(getTask(100L));
    }

    @Test
    @DisplayName("Ошибка при вызове mbi-api клиента, запись об отправке в базу не сохраняется")
    @ExpectedDatabase(
        value = "/jobs/consumer/send_notification_to_shop/after/shop_notification_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void sendNotificationToShopErrorMbiApiClient() {
        doThrow(new RuntimeException())
            .when(mbiApiClient).sendMessageToShop(DAAS_SHOP_ID, NOTIFICATION_ID, NOTIFICATION_DATA);

        sendNotificationToShopConsumer.execute(getTask(DAAS_SHOP_ID));

        verify(mbiApiClient).sendMessageToShop(DAAS_SHOP_ID, NOTIFICATION_ID, NOTIFICATION_DATA);
    }

    @Nonnull
    private Task<SendNotificationPayload> getTask(Long shopId) {
        return new Task<>(
            new QueueShardId("queueShardId"),
            new SendNotificationPayload(
                "requestId",
                NOTIFICATION_ID,
                shopId,
                null,
                NOTIFICATION_DATA,
                new MessageRecipients()
            ),
            3,
            ZonedDateTime.now(),
            "traceInfo",
            "actor"
        );
    }
}
