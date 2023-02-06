package ru.yandex.market.abo.core.dynamic.service;

import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.tms.quartz2.dynamic.model.DynamicTask;

/**
 * @author artemmz
 * @date 21.05.18.
 */
public abstract class DynamicTaskCronTest<T extends DynamicTask> extends CronExpressionsTest {

    @Override
    public List<String> getCronExpressions() {
        return tasks().stream().map(DynamicTask::getCronExpression).collect(Collectors.toList());
    }

    abstract List<T> tasks();
}