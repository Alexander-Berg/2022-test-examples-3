package ru.yandex.market.delivery.tracker.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.delivery.tracker.configuration.dbqueue.QueueTaskListener;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;
import ru.yandex.money.common.dbqueue.api.TaskRecord;
import ru.yandex.money.common.dbqueue.settings.QueueLocation;

@Configuration
public class TestQueueConfiguration {

    @Bean
    @Primary
    public TaskLifecycleListener latchTaskLifecycleListener() {
        return new LatchQueueTaskListener();
    }

    public class LatchQueueTaskListener extends QueueTaskListener {
        private CountDownLatch finishedLatch = new CountDownLatch(1);
        private List<TaskExecutionResult> lastResults = new ArrayList<>();

        public void setFinishedLatch(CountDownLatch finishedLatch) {
            this.finishedLatch = finishedLatch;
            lastResults = new ArrayList<>();
        }

        public List<TaskExecutionResult> getLastResults() {
            return lastResults;
        }

        @Override
        public void finished(@Nonnull QueueShardId shardId,
                             @Nonnull QueueLocation location,
                             @Nonnull TaskRecord taskRecord) {
            super.finished(shardId, location, taskRecord);
            finishedLatch.countDown();
        }

        @Override
        public void executed(@Nonnull QueueShardId shardId,
                             @Nonnull QueueLocation location,
                             @Nonnull TaskRecord taskRecord,
                             @Nonnull TaskExecutionResult executionResult,
                             long processTaskTime) {
            super.executed(shardId, location, taskRecord, executionResult, processTaskTime);
            lastResults.add(executionResult);
        }
    }
}
