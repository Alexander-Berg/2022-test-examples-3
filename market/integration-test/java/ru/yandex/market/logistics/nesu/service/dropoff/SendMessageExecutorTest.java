package ru.yandex.market.logistics.nesu.service.dropoff;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.jobs.producer.SendNotificationToShopProducer;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тест на выполнение подзадачи отключения дропоффа. Отправка уведомлений партнерам.")
public class SendMessageExecutorTest extends AbstractDisablingSubtaskTest {

    private static final int NOTIFICATION_ID = 1639036893;

    @Autowired
    private SendNotificationToShopProducer sendNotificationToShopProducer;

    @BeforeEach
    void setupProducer() {
        doNothing().when(sendNotificationToShopProducer).produceTask(anyInt(), anyLong(), anyLong(), anyString());
    }

    @AfterEach
    void noInteraction() {
        verifyNoMoreInteractions(sendNotificationToShopProducer);
    }

    @Test
    @DatabaseSetup(
        value = "/service/dropoff/before/send_message_notification.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/dropoff/after/send_message_notification.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная отправка уведомлений.")
    void successSendNotification() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));

        verify(sendNotificationToShopProducer).produceTask(
            NOTIFICATION_ID,
            SHOP_ID_11,
            DROPSHIP_PARTNER_ID_1,
            "<request><supplier-id>11</supplier-id><closing-date>06 декабря</closing-date>"
                + "<dropoff-address-text>test</dropoff-address-text></request>"
        );
    }

    @Test
    @DatabaseSetup(
        value = "/service/dropoff/before/send_message_notification_no_affected_shops.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/dropoff/after/send_message_notification_no_affected_shops.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Нет затронутых магазинов, не отправляем уведомления.")
    void noAffectedShops() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));
    }
}
