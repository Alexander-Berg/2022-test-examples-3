package ru.yandex.market.premoderation.queue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Функциональные тесты на {@link PublishModerationQueueEventsExecutor}.
 */
public class PublishModerationQueueEventsExecutorTest extends FunctionalTest {

    @Autowired
    private PublishModerationQueueEventsExecutor executor;

    @Test
    @DbUnitDataSet(before = "testPublishModerationQueueEventsExecutorTest.before.csv",
            after = "testPublishModerationQueueEventsExecutorTest.after.csv")
    public void testPublishModerationQueueEventsExecutorTest() {
        executor.doJob(null);
    }
}
