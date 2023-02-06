package ru.yandex.market.delivery.mdbapp.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;

import org.junit.Ignore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrdersTskvLogger;
import ru.yandex.market.delivery.mdbapp.components.queue.TaskActorTransformer;
import ru.yandex.market.delivery.mdbapp.components.queue.TaskListenerLoggingDecorator;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;
import ru.yandex.money.common.dbqueue.api.TaskRecord;
import ru.yandex.money.common.dbqueue.settings.QueueLocation;

@Ignore
@Configuration
public class LatchTaskListenerConfig {

    @Bean
    @Primary
    TaskLifecycleListener latchTaskLifecycleListener(
        TaskActorTransformer taskActorTransformer,
        BackLogOrdersTskvLogger backLogOrdersTskvLogger
    ) {
        return new TaskListener(
            new TaskListenerLoggingDecorator(
                new ru.yandex.market.delivery.mdbapp.components.queue.TaskListener(),
                taskActorTransformer,
                backLogOrdersTskvLogger
            )
        );
    }

    @Ignore
    public static class TaskListener implements TaskLifecycleListener {

        private final TaskLifecycleListener delegate;
        //@todo you can add some new latch for picked, started etc
        private CountDownLatch finishedLatch = new CountDownLatch(1);
        private List<TaskExecutionResult> lastResults = new ArrayList<>();

        public TaskListener(TaskLifecycleListener delegate) {
            this.delegate = delegate;
        }

        public void setFinishedLatch(CountDownLatch finishedLatch) {
            this.finishedLatch = finishedLatch;
            lastResults = new ArrayList<>();
        }

        public List<TaskExecutionResult> getLastResults() {
            return lastResults;
        }

        @Override
        public void picked(@Nonnull QueueShardId shardId,
                           @Nonnull QueueLocation location,
                           @Nonnull TaskRecord taskRecord,
                           long pickTaskTime) {
            delegate.picked(shardId, location, taskRecord, pickTaskTime);
        }

        @Override
        public void started(@Nonnull QueueShardId shardId,
                            @Nonnull QueueLocation location,
                            @Nonnull TaskRecord taskRecord) {
            delegate.started(shardId, location, taskRecord);
        }

        @Override
        public void executed(@Nonnull QueueShardId shardId,
                             @Nonnull QueueLocation location,
                             @Nonnull TaskRecord taskRecord,
                             @Nonnull TaskExecutionResult executionResult,
                             long processTaskTime) {
            delegate.executed(shardId, location, taskRecord, executionResult, processTaskTime);
            lastResults.add(executionResult);
        }

        @Override
        public void finished(@Nonnull QueueShardId shardId,
                             @Nonnull QueueLocation location,
                             @Nonnull TaskRecord taskRecord) {
            delegate.finished(shardId, location, taskRecord);
            finishedLatch.countDown();
        }

        @Override
        public void crashed(@Nonnull QueueShardId shardId,
                            @Nonnull QueueLocation location,
                            @Nonnull TaskRecord taskRecord,
                            @Nonnull Exception exc) {
            delegate.crashed(shardId, location, taskRecord, exc);
        }
    }
}
