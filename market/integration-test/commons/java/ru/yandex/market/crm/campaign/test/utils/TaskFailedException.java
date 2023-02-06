package ru.yandex.market.crm.campaign.test.utils;

import ru.yandex.market.crm.tasks.domain.TaskInstanceInfo;

public class TaskFailedException extends IllegalStateException {

    private final TaskInstanceInfo taskInstanceInfo;

    public TaskFailedException(String s, TaskInstanceInfo taskInstanceInfo) {
        super(s);
        this.taskInstanceInfo = taskInstanceInfo;
    }

    public TaskInstanceInfo getTaskInstanceInfo() {
        return taskInstanceInfo;
    }
}
