package ru.yandex.market.mboc.common.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.taskqueue.TaskQueueRepository;
import ru.yandex.market.mbo.taskqueue.TaskRecord;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

public class TaskQueueRepositoryMock extends TaskQueueRepository {

    private Set<TaskRecord> taskRecords = new HashSet<>();

    public TaskQueueRepositoryMock(NamedParameterJdbcTemplate jdbcTemplate, TransactionHelper transactionHelper,
                                   String schemaName) {
        super(null, null, null);
    }

    public TaskQueueRepositoryMock() {
        super(null, null, null);
    }

    @Override
    public long insert(TaskRecord taskRecord) {
        taskRecords.add(taskRecord);
        return taskRecord.getId();
    }

    @Override
    public void insertBatch(Collection<TaskRecord> instances) {
        taskRecords.addAll(instances);
    }

    @Override
    public Optional<TaskRecord> findNextTask(boolean withLock, List<String> taskTypes) {
        throw new NotImplementedException("");
    }

    @Override
    public TaskRecord findById(long taskId) {
        for (TaskRecord taskRecord : taskRecords) {
            if (taskRecord.getId() == taskId) {
                return taskRecord;
            }
        }
        return null;
    }

    @Override
    public List<TaskRecord> findAll() {
        return findAll(false);
    }

    @Override
    public List<TaskRecord> findAll(boolean checkLocks) {
        return new ArrayList<>(taskRecords);
    }

    @Override
    public void deleteAll() {
        taskRecords.clear();
    }

    @Override
    public void markDone(long taskId, String result, long duration) {
        throw new NotImplementedException("");
    }

    @Override
    public void markFailed(long taskId, String lastError) {
        throw new NotImplementedException("");
    }

    @Override
    public void markRetry(long taskId, Instant nextRun, int attempts, String lastError) {
        throw new NotImplementedException("");
    }

    @Override
    public List<TaskRecord> getFailedTasks() {
        return taskRecords.stream()
            .filter(i -> i.getTaskResult() != null)
            .filter(i -> i.getTaskResult().equals("FAILED"))
            .collect(Collectors.toList());
    }

    @Override
    public List<TaskRecord> getRunningTasks() {
        return taskRecords.stream()
            .filter(i -> i.getTaskResult() == null || !i.getTaskResult().equals("FAILED") && !i.getTaskResult().equals("DONE"))
            .collect(Collectors.toList());
    }
}
