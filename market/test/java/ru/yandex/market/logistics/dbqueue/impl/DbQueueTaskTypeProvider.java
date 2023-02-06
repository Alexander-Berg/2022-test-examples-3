package ru.yandex.market.logistics.dbqueue.impl;

import org.springframework.stereotype.Service;

@Service
public class DbQueueTaskTypeProvider implements DbQueueTaskTypeProviderInterface {

    @Override
    public DbQueueTaskTypeInterface getValueOf(String name) {
        return DbQueueTaskType.valueOf(name);
    }

    @Override
    public DbQueueTaskTypeInterface[] getValues() {
        return DbQueueTaskType.values();
    }
}
