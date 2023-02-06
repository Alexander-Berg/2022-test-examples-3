package ru.yandex.market.global.checkout.domain.queue.task.notification;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.yoomoney.tech.dbqueue.config.DatabaseAccessLayer;
import ru.yoomoney.tech.dbqueue.config.QueueShard;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.push.model.PushTankerKeyField;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;

public class SendRecipientOrderNotificationTest extends BaseFunctionalTest {

    public static final NotificationContent CONTENT = NotificationContent.ORDER_CANCELED_WITHOUT_PAYMENT;
    public static final Long ORDER_ID = 12356L;
    public static final String YTAXI_ID = "12356ffs";
    public static final String LOCALE = "en";

    @MockBean
    private SendPushProducer testProducer;
    @Autowired
    private SendRecipientOrderNotificationConsumer consumer;
    @Autowired
    private QueueShard<DatabaseAccessLayer> shard;


    @Test
    public void testPush() {

        var payload = new SendRecipientOrderNotificationPayload(ORDER_ID, YTAXI_ID, LOCALE, CONTENT);

        SendPushPayload pushPayload = new SendPushPayload()
                .setPushName(CONTENT + " for order " + ORDER_ID)
                .setIdempotencyKey(ORDER_ID + CONTENT.name())
                .setUserIdType(SendPushPayload.SendPushUserIdentifierType.GO_USER_ID)
                .setUserId(YTAXI_ID)
                .setTitleTankerKey(new PushTankerKeyField(CONTENT.getTitleTankerKey()))
                .setTextTankerKey(new PushTankerKeyField(CONTENT.getTextTankerKey()))
                .setLocale(LOCALE);

        TestQueueTaskRunner.runTaskOnceThrowOnFail(consumer, payload);
        ArgumentCaptor<SendPushPayload> captor = ArgumentCaptor.forClass(SendPushPayload.class);
        Mockito.verify(this.testProducer).enqueueImmediately(captor.capture());

        Assertions.assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(pushPayload);

    }

}
