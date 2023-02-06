package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.queue.InspectionQueue;
import ru.yandex.market.markup2.queue.InspectionQueueItem;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author galaev
 * @since 2019-06-11
 */
public class InspectionQueueStub extends InspectionQueue implements IPersisterStub {
    private final DefaultPersisterStub<Integer, InspectionQueueItem> storage;

    public InspectionQueueStub() {
        this(new DefaultPersisterStub<>());
    }

    private InspectionQueueStub(DefaultPersisterStub<Integer, InspectionQueueItem> storage) {
        this.storage = storage;
    }

    @Override
    public InspectionQueueStub copy() {
        return new InspectionQueueStub(this.storage);
    }

    @Override
    public List<InspectionQueueItem> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<InspectionQueueItem> getValues(Collection<Integer> taskIds) {
        return storage.getValues(taskIds);
    }

    @Override
    public void processNewTasks(int taskType, Consumer<List<Integer>> consumer, int needed) {
        List<InspectionQueueItem> items = storage.getByCriteria(queueItem -> !queueItem.isApproved());
        List<Integer> taskIds = items.stream()
            .map(item -> {
                item.setApproved(true);
                return item.getTaskId();
            })
            .limit(needed)
            .collect(Collectors.toList());

        log.debug("Loaded {} tasks of {}", taskIds.size(), items.size());

        consumer.accept(taskIds);
    }

    @Override
    public List<InspectionQueueItem> getNewByTypeAndCategory(int taskType, int categoryId) {
        return storage.getAllValues().stream()
            .filter(i -> !i.isApproved() && i.getCategoryId() == categoryId && i.getTaskType() == taskType)
            .collect(Collectors.toList());
    }

    @Override
    public void persist(InspectionQueueItem queueItem) {
        storage.persist(queueItem.getTaskId(), queueItem);
        log.debug("persisted {}, size {}", queueItem, storage.storage.size());
    }
}
