package ru.yandex.market.logistics.lom.utils.jobs;

import java.time.ZonedDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.lom.jobs.model.ExecutionQueueItemPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.retry.FixedAttemptsRetryPolicy;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

@UtilityClass
@ParametersAreNonnullByDefault
public class TaskFactory {

    public <T extends ExecutionQueueItemPayload> Task<T> createTask(T payload) {
        return new Task<>(
            new QueueShardId("QUEUE_SHARD_ID"),
            payload,
            FixedAttemptsRetryPolicy.DEFAULT_ATTEMPTS_NUMBER,
            ZonedDateTime.now(),
            null,
            null
        );
    }

    public <T extends ExecutionQueueItemPayload> Task<T> createTask(QueueType queueType, T payload) {
        return new Task<>(
            new QueueShardId(queueType.name()),
            payload,
            queueType.getRetryAttemptsCount(),
            ZonedDateTime.now(),
            null,
            null
        );
    }

    public <T extends ExecutionQueueItemPayload> Task<T> createTask(QueueType queueType, T payload, int attemptsCount) {
        return new Task<>(
            new QueueShardId(queueType.name()),
            payload,
            attemptsCount,
            ZonedDateTime.now(),
            null,
            null
        );
    }
}
