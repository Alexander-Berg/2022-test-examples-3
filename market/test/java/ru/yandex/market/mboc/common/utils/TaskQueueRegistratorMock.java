package ru.yandex.market.mboc.common.utils;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.mbo.taskqueue.TaskQueueRegistrator;
import ru.yandex.market.mbo.taskqueue.TaskQueueTask;

public class TaskQueueRegistratorMock extends TaskQueueRegistrator {

    private List<TaskQueueTask> tasks = new ArrayList<>();

    public TaskQueueRegistratorMock() {
        super(null, null);
    }

    @Override
    public long registerTask(TaskQueueTask task) {
        tasks.add(task);
        return 0L;
    }

    public List<TaskQueueTask> getTasks() {
        return tasks;
    }
}
