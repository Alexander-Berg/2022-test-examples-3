package ru.yandex.market.logistics.iris.jobs.consumers.retry;

import java.time.ZonedDateTime;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.market.logistics.iris.jobs.model.LoopingQueueItemPayload;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

public class SimpleRetryStrategyTest {

    @Test
    public void failIfMaxAttemptsNotReached() {
        SimpleRetryPolicy<LoopingQueueItemPayload> retryPolicy = new SimpleRetryPolicy<>(2);
        TaskExecutionResult res = retryPolicy.processFailedTask(null, createTask(1, LoopingQueueItemPayload.class));
        SoftAssertions.assertSoftly(softly -> softly.assertThat(res).isEqualTo(TaskExecutionResult.fail()));
    }

    @Test
    public void finishIfMaAttemptsExceeded() {
        SimpleRetryPolicy<LoopingQueueItemPayload> retryPolicy = new SimpleRetryPolicy<>(2);
        TaskExecutionResult res = retryPolicy.processFailedTask(null, createTask(2, LoopingQueueItemPayload.class));
        SoftAssertions.assertSoftly(softly -> softly.assertThat(res).isEqualTo(TaskExecutionResult.finish()));
    }

    private <T> Task<T> createTask(int attemptsCount, Class<T> payloadClass) {
        return new Task<>(new QueueShardId("meh"), null, attemptsCount, ZonedDateTime.now(), "mehInfo", null);
    }
}
