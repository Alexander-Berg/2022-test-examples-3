package ru.yandex.market.global.checkout.domain.queue.task;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.config.DatabaseAccessLayer;
import ru.yoomoney.tech.dbqueue.config.QueueShard;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;

@Disabled
public class SendOrderNotificationConsumerLocalTest extends BaseLocalTest {
    @Autowired
    public QueueShard<DatabaseAccessLayer> shard;

    @Autowired
    SendOrderNotificationConsumer sendOrderNotificationConsumer;

    @Test
    public void test() {
        TestQueueTaskRunner.runTaskThrowOnFail(sendOrderNotificationConsumer, testData.getNewOrder().getId());
    }
}
