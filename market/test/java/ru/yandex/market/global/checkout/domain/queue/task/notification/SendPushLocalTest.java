package ru.yandex.market.global.checkout.domain.queue.task.notification;

import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.config.DatabaseAccessLayer;
import ru.yoomoney.tech.dbqueue.config.QueueShard;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.domain.push.model.PushTankerKeyField;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;

import static ru.yandex.market.global.checkout.configuration.ConfigurationProperties.PUSH_NOTIFICATIONS_ENABLED;

@Disabled
public class SendPushLocalTest extends BaseLocalTest {

    @Autowired
    private QueueShard<DatabaseAccessLayer> shard;
    @Autowired
    private SendPushConsumer consumer;
    @Autowired
    private ConfigurationService configuration;

    @Test
    public void sentPushTest() {

        configuration.insertValue(PUSH_NOTIFICATIONS_ENABLED, true);

        NotificationContent notification = NotificationContent.REWARD_PROMOCODE_RECEIVED;
        SendPushPayload sendPushPayload = new SendPushPayload()
                .setPushName("test_push_for_debugging")
                .setIdempotencyKey("push_for_testing" + System.currentTimeMillis())
                .setUserIdType(SendPushPayload.SendPushUserIdentifierType.YANDEX_UID)
                .setUserId("") // вставить ключ сюда
                .setLocale("en")
                .setTitleTankerKey(new PushTankerKeyField(notification.getTitleTankerKey()))
                .setTextTankerKey(new PushTankerKeyField(notification.getTextTankerKey(),
                        Map.of("sum", "5 ₽")));

        TestQueueTaskRunner.runTaskOnceThrowOnFail(consumer, sendPushPayload);

    }

}
