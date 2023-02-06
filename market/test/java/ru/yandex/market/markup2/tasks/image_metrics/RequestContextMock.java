package ru.yandex.market.markup2.tasks.image_metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.group.TaskConfigGroupInfo;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.workflow.general.IResponseItem;
import ru.yandex.market.markup2.workflow.general.ITaskDataItemPayload;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.generation.RequestGeneratorContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RequestContextMock<I, P extends ITaskDataItemPayload<I>, R extends IResponseItem>
    extends RequestGeneratorContext<I, P, R> {

    private int needToGenerate;
    private TaskConfigGroupInfo configGroupInfo;
    private List<P> payloads = new ArrayList<>();
    private static final Logger log = LogManager.getLogger();

    public RequestContextMock(int count, TaskInfo taskInfo, TaskConfigGroupInfo configGroupInfo) {
        super(null, taskInfo, null, null);
        this.needToGenerate = count;
        this.configGroupInfo = configGroupInfo;
    }

    public RequestContextMock(int count, TaskInfo taskInfo,
                              TaskConfigGroupInfo configGroupInfo, List<P> payloads) {
        super(null, taskInfo, null, null);
        this.payloads = payloads;
        this.needToGenerate = count;
        this.configGroupInfo = configGroupInfo;
    }

    public static <I, P extends ITaskDataItemPayload<I>, R extends IResponseItem> RequestContextMock<I, P, R>
    create(int count, TaskConfigGroupInfo configGroupInfo) {
        return create(count, configGroupInfo, new ArrayList<>());
    }

    public static <I, P extends ITaskDataItemPayload<I>, R extends IResponseItem> RequestContextMock<I, P, R>
    create(int count, TaskConfigGroupInfo configGroupInfo, List<P> payloads) {

        TaskConfigInfo.Builder configInfoBuilder = new TaskConfigInfo.Builder();
        configInfoBuilder.setCount(count);

        TaskInfo.Builder taskInfoBuilder = new TaskInfo.Builder();
        taskInfoBuilder.setConfig(configInfoBuilder.build());

        return new RequestContextMock<>(count, taskInfoBuilder.build(), configGroupInfo, payloads);
    }

    @Override
    public int getCategoryId() {
        return 1;
    }

    @Override
    public boolean taskExists(I id) {
        return payloads.stream().anyMatch(p -> p.getDataIdentifier().equals(id));
    }

    @Override
    public int getLeftToGenerate() {
        return this.needToGenerate - payloads.size();
    }

    public List<P> getPayloads() {
        return payloads;
    }

    @Override
    public TaskConfigGroupInfo getGroupConfigInfo() {
        return configGroupInfo;
    }

    @Override
    public boolean createTaskDataItem(P payload) {
        boolean isExist = payloads.stream().anyMatch(p -> p.getDataIdentifier().equals(payload.getDataIdentifier()));
        if (!isExist) {
            payloads.add(payload);
        }

        return !isExist;
    }

    @Override
    public Collection<TaskDataItem<P, R>> getTaskDataItems() {
        return payloads.stream().map(p -> new TaskDataItem<P, R>(0, p)).collect(Collectors.toList());
    }
}
