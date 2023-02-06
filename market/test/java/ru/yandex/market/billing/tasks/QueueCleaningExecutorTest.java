package ru.yandex.market.billing.tasks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;

class QueueCleaningExecutorTest extends FunctionalTest {
    @Autowired
    QueueCleaningExecutor executor;

    @Test
    void doJob() {
        executor.doJob(null);
    }
}
