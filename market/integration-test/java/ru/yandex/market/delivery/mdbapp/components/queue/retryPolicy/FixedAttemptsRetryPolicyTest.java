package ru.yandex.market.delivery.mdbapp.components.queue.retryPolicy;

import java.time.ZonedDateTime;

import javax.annotation.Nonnull;

import lombok.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.BaseConsumer;
import ru.yandex.market.delivery.mdbapp.configuration.queue.AbstractQueueDefinition;
import ru.yandex.money.common.dbqueue.api.QueueConsumer;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.QueueShardRouter;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskPayloadTransformer;
import ru.yandex.money.common.dbqueue.settings.QueueId;

@DisplayName("Тест на политику ретраев")
class FixedAttemptsRetryPolicyTest extends AllMockContextualTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Число запусков таски ограничено и таска не перезапускается бесконечно")
    void maxAttemptsReached(boolean throwsException) {
        ConsumerForFailingTask consumerForFailingTask = new ConsumerForFailingTask();

        TaskExecutionResult taskExecutionResult = null;
        long attemptsCount = 0L;
        while (taskExecutionResult != TaskExecutionResult.finish()) {
            taskExecutionResult = consumerForFailingTask.execute(createTask(throwsException));
            attemptsCount++;
        }

        softly.assertThat(attemptsCount).isEqualTo(10L);
    }

    @Nonnull
    private Task<ForeverFailingTask> createTask(boolean throwsException) {
        return new Task<>(
            ForeverFailingTaskQueue.QUEUE_SHARD_ID,
            new ForeverFailingTask(1L, throwsException),
            10000L,
            ZonedDateTime.now(),
            null,
            null
        );
    }

    @Value
    static class ForeverFailingTask {
        long id;
        boolean throwException;
    }

    static class ForeverFailingTaskQueue extends AbstractQueueDefinition<ForeverFailingTask> {
        static final QueueId QUEUE_ID = new QueueId("test.forever.fail.task");
        static final QueueShardId QUEUE_SHARD_ID = new QueueShardId("test.forever.fail.task.shard");

        @Override
        public QueueId getQueueId() {
            return QUEUE_ID;
        }

        @Override
        public QueueConsumer<ForeverFailingTask> queueConsumerBean() {
            return new ConsumerForFailingTask();
        }

        @Override
        public QueueProducer<ForeverFailingTask> queueProducerBean() {
            return null;
        }

        @Override
        public QueueShardRouter<ForeverFailingTask> queueShardRouterBean() {
            return null;
        }

        @Override
        public TaskPayloadTransformer<ForeverFailingTask> taskPayloadTransformerBean() {
            return null;
        }
    }

    static class ConsumerForFailingTask extends BaseConsumer<ForeverFailingTask> {

        ConsumerForFailingTask() {
            super(ForeverFailingTaskQueue.QUEUE_ID, ForeverFailingTask.class);
        }

        @Override
        protected TaskExecutionResult processTask(@Nonnull Task<ForeverFailingTask> task) {
            ForeverFailingTask payload = task.getPayload().orElse(new ForeverFailingTask(1L, false));
            if (payload.isThrowException()) {
                throw new RuntimeException("Task forever fails with exception");
            }

            return TaskExecutionResult.fail();
        }
    }
}
