package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.TaskDataItemPersister;
import ru.yandex.market.markup2.workflow.general.IResponseItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemStub;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author york
 * @since 25.05.2018
 */
public class TaskDataItemPersisterStub extends TaskDataItemPersister implements IPersisterStub {
    private final DefaultPersisterStub<Long, TaskDataItemStub> storage;

    public TaskDataItemPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private TaskDataItemPersisterStub(DefaultPersisterStub<Long, TaskDataItemStub> storage) {
        this.storage = storage;
    }

    public TaskDataItemPersisterStub copy() {
        return new TaskDataItemPersisterStub(this.storage);
    }

    @Override
    public List<TaskDataItemStub> getValues(Long... integers) {
        return storage.getValues(integers);
    }

    @Override
    public List<TaskDataItemStub> getValuesByTaskId(Collection<Integer> values) {
        return storage.getByValues(values, TaskDataItemStub::getTaskId);
    }

    @Override
    public List<TaskDataItemStub> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public Collection<Long> upsert(int taskId, int taskTypeId, Collection<? extends TaskDataItem> items) {
        return items.stream()
            .map(tdi -> convert(taskId, taskTypeId, tdi))
            .map(stub -> storage.upsert(stub, TaskDataItemStub::getId))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<Long> update(int taskTypeId, Collection<? extends TaskDataItem> items) {
        Map<Long, Integer> idToTaskId = storage.getAllValues().stream()
            .collect(Collectors.toMap(TaskDataItemStub::getId, TaskDataItemStub::getTaskId));
        return items.stream()
            .map(tdi -> convert(idToTaskId.get(tdi.getId()), taskTypeId, tdi))
            .map(stub -> storage.upsert(stub, TaskDataItemStub::getId))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<Long> updateState(Collection<? extends TaskDataItem> items) {
        Map<Long, TaskDataItemStub> map = storage.getAllValues().stream()
            .collect(Collectors.toMap(TaskDataItemStub::getId, Function.identity()));

        return items.stream()
            .map(tdi -> {
                TaskDataItemStub stub = map.get(tdi.getId());
                stub.setState(tdi.getState());
                return stub.getId();
            })
            .collect(Collectors.toList());
    }

    @Override
    public Collection<Long> updateStateAndResponse(int taskTypeId, Collection<? extends TaskDataItem> items) {
        Map<Long, TaskDataItemStub> map = storage.getAllValues().stream()
            .collect(Collectors.toMap(TaskDataItemStub::getId, Function.identity()));

        return items.stream()
            .map(tdi -> {
                TaskDataItemStub stub = map.get(tdi.getId());
                stub.setState(tdi.getState());
                try {
                    stub.setRespData(writeResponseData(taskTypeId,
                        (IResponseItem) tdi.getResponseInfo()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return stub.getId();
            })
            .collect(Collectors.toList());
    }

    private TaskDataItemStub convert(int taskId, int taskTypeId, TaskDataItem taskDataItem) {
        try {
            TaskDataItemStub result = new TaskDataItemStub();
            result.setId(taskDataItem.getId());
            result.setGenerateTime(taskDataItem.getGenerateTime());
            result.setState(taskDataItem.getState());
            result.setTaskId(taskId);
            result.setDataIdentifier(writeDataItemIdentifier(taskTypeId,
                taskDataItem.getInputData().getDataIdentifier()));
            result.setMainData(writeDataItemPayload(taskTypeId,
                taskDataItem.getInputData()));
            result.setRespData(writeResponseData(taskTypeId,
                (IResponseItem) taskDataItem.getResponseInfo()));
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
