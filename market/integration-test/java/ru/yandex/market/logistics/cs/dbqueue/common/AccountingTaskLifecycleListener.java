package ru.yandex.market.logistics.cs.dbqueue.common;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;
import ru.yandex.money.common.dbqueue.api.TaskRecord;
import ru.yandex.money.common.dbqueue.settings.QueueLocation;

@ParametersAreNonnullByDefault
public class AccountingTaskLifecycleListener implements TaskLifecycleListener {
    private final Map<QueueCoordinates, Deque<LifecycleEvent>> events = new ConcurrentHashMap<>();

    @Override
    public void picked(
        QueueShardId shardId,
        QueueLocation location,
        TaskRecord taskRecord,
        long pickTaskTime
    ) {
        putEvent(
            new QueueCoordinates(shardId, location),
            LifecycleEvent.builder(LifecycleEventType.PICKED, taskRecord)
                .pickTaskTime(pickTaskTime)
                .build()
        );
    }

    @Override
    public void started(
        QueueShardId shardId,
        QueueLocation location,
        TaskRecord taskRecord
    ) {
        putEvent(
            new QueueCoordinates(shardId, location),
            LifecycleEvent.builder(LifecycleEventType.STARTED, taskRecord).build()
        );
    }

    @Override
    public void executed(
        QueueShardId shardId,
        QueueLocation location,
        TaskRecord taskRecord,
        TaskExecutionResult executionResult,
        long processTaskTime
    ) {
        putEvent(
            new QueueCoordinates(shardId, location),
            LifecycleEvent.builder(LifecycleEventType.EXECUTED, taskRecord)
                .executionResult(executionResult)
                .processTaskTime(processTaskTime)
                .build()
        );
    }

    @Override
    public void finished(
        QueueShardId shardId,
        QueueLocation location,
        TaskRecord taskRecord
    ) {
        putEvent(
            new QueueCoordinates(shardId, location),
            LifecycleEvent.builder(LifecycleEventType.FINISHED, taskRecord).build()
        );
    }

    @Override
    public void crashed(
        QueueShardId shardId,
        QueueLocation location,
        TaskRecord taskRecord,
        Exception exc
    ) {
        putEvent(
            new QueueCoordinates(shardId, location),
            LifecycleEvent.builder(LifecycleEventType.CRASHED, taskRecord)
                .exception(exc)
                .build()
        );
    }

    public Deque<LifecycleEvent> getEvents(QueueCoordinates coordinates) {
        return events.computeIfAbsent(coordinates, c -> new ConcurrentLinkedDeque<>());
    }

    public void reset() {
        events.clear();
    }

    private void putEvent(QueueCoordinates coordinates, LifecycleEvent event) {
        getEvents(coordinates).add(event);
    }

    public enum LifecycleEventType {
        PICKED, STARTED, EXECUTED, FINISHED, CRASHED
    }

    @Value
    public static class QueueCoordinates {
        QueueShardId shardId;
        QueueLocation location;
    }

    @ToString
    @EqualsAndHashCode
    @Builder(access = AccessLevel.PRIVATE)
    public static class LifecycleEvent {
        private final LifecycleEventType type;
        private final TaskRecord record;
        private final Long pickTaskTime;
        private final TaskExecutionResult executionResult;
        private final Long processTaskTime;
        private final Exception exception;

        public LifecycleEventType getType() {
            return type;
        }

        public TaskRecord getRecord() {
            return record;
        }

        public Optional<Long> getPickTaskTime() {
            return Optional.ofNullable(pickTaskTime);
        }

        public Optional<TaskExecutionResult> getExecutionResult() {
            return Optional.ofNullable(executionResult);
        }

        public Optional<Long> getProcessTaskTime() {
            return Optional.ofNullable(processTaskTime);
        }

        public Optional<Exception> getException() {
            return Optional.ofNullable(exception);
        }

        public static LifecycleEvent.LifecycleEventBuilder builder(LifecycleEventType type, TaskRecord record) {
            return new LifecycleEventBuilder().type(type).record(record);
        }
    }
}
