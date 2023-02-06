package ru.yandex.market.markup2.workflow.taskType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.config.TaskConfigState;
import ru.yandex.market.markup2.entries.group.TaskConfigGroupInfo;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.entries.type.TaskTypeInfo;
import ru.yandex.market.markup2.processors.task.TaskProgress;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.workflow.general.IResponseItem;
import ru.yandex.market.markup2.workflow.general.ITaskDataItemPayload;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.pipe.Pipes;
import ru.yandex.market.markup2.workflow.requestSender.RequestSenderContext;
import ru.yandex.market.markup2.workflow.requestSender.RequestsSender;
import ru.yandex.market.markup2.workflow.responseReceiver.ResponseReceiverContext;
import ru.yandex.market.markup2.workflow.responseReceiver.ResponsesReceiver;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueStrategy;
import ru.yandex.market.markup2.workflow.taskType.processor.ITaskTypeProcessor;
import ru.yandex.market.markup2.workflow.taskType.processor.SimpleDataItemsProcessor;
import ru.yandex.market.markup2.workflow.taskType.processor.SkippingTaskTypeProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.yandex.market.markup2.utils.DummyTestTask.DummyResponse;
import static ru.yandex.market.markup2.utils.DummyTestTask.DummyTaskIdentity;
import static ru.yandex.market.markup2.utils.DummyTestTask.DummyTaskPayload;

/**
 * @author galaev@yandex-team.ru
 * @since 15/08/2017.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class DummyHitmanTaskProcessorTest {

    private SkippingTaskTypeProcessor<DummyTaskIdentity, DummyTaskPayload, DummyResponse> processor;

    @Before
    public void setUp() throws IOException {
        SimpleDataItemsProcessor<DummyTaskIdentity, DummyTaskPayload, DummyResponse> dataItemsProcessor =
            Mockito.mock(SimpleDataItemsProcessor.class);
        Mockito.when(dataItemsProcessor.getResponseClass())
            .thenAnswer(invocation -> DummyResponse.class);
        Mockito.when(dataItemsProcessor.getRequestSerializer())
            .thenAnswer(invocation -> new JsonUtils.DefaultJsonSerializer<>());
        Mockito.when(dataItemsProcessor.getResponseDeserializer())
            .thenAnswer(invocation -> new JsonUtils.DefaultJsonDeserializer<>(DummyResponse.class));

        processor = new SkippingTaskTypeProcessor<>(dataItemsProcessor);
    }

    @Test
    public void testRequestsSender() {
        TestRequestsSender sender = new TestRequestsSender();
        TaskInfo taskInfo = createTaskInfo(TaskDataItemState.GENERATED);
        taskInfo.getProgress().updateTaskProgress(p -> p.incGeneratedEntitiesCount(1)
            .incGeneratedCount(1));

        RequestSenderContext senderContext = sender.process(taskInfo, processor);

        Assert.assertTrue(!senderContext.isEmpty());
        TaskProgress taskProgress = senderContext.getTask().getProgress();
        Assert.assertEquals(20, taskProgress.getDataItemsByState(TaskDataItemState.SENT).size());
    }

    @Test
    public void testResponsesReceiver() {
        TestResponsesReceiver receiver = new TestResponsesReceiver();
        TaskInfo taskInfo = createTaskInfo(TaskDataItemState.SENT);
        taskInfo.getProgress().updateTaskProgress(p -> p.incSendedCount(1));
        ResponseReceiverContext receiverContext = receiver.process(taskInfo, processor);

        Assert.assertTrue(!receiverContext.isEmpty());
        TaskProgress taskProgress = receiverContext.getTask().getProgress();
        Assert.assertEquals(20, taskProgress.getDataItemsByState(TaskDataItemState.SUCCESSFUL_RESPONSE).size());
    }

    private TaskInfo createTaskInfo(TaskDataItemState dataItemsState) {
        TaskTypeInfo taskTypeInfo = new TaskTypeInfo(1, "dummy", "hitman-mitman", 1, 1,
            Pipes.FULLY_PARALLEL, TaskDataUniqueStrategy.TYPE_CATEGORY);

        TaskConfigGroupInfo groupConfigInfo = new TaskConfigGroupInfo.Builder()
            .setTypeInfo(taskTypeInfo)
            .setId(2)
            .build();
        TaskConfigInfo taskConfigInfo = new TaskConfigInfo.Builder()
            .setCount(21)
            .setGroupInfo(groupConfigInfo)
            .setId(3)
            .setState(TaskConfigState.ACTIVE)
            .build();
        TaskInfo taskInfo = new TaskInfo.Builder()
            .setId(1)
            .setState(TaskState.RUNNING)
            .setConfig(taskConfigInfo)
            .build();

        List<TaskDataItem<DummyTaskPayload, DummyResponse>> dataItems = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            dataItems.add(new TaskDataItem<>(i, 100, new DummyTaskPayload(i, "data")));
        }

        taskInfo.getProgress().initDataItemsState(dataItems, dataItemsState);
        return taskInfo;
    }

    private static class TestRequestsSender extends RequestsSender {
        @Override
        public <I, D extends ITaskDataItemPayload<I>, R extends IResponseItem> RequestSenderContext
        process(TaskInfo task, ITaskTypeProcessor<I, D, R> processor) {
            return super.process(task, processor);
        }

        @Override
        protected Collection<Long> persistItems(TaskInfo taskInfo, Collection<? extends TaskDataItem> requests) {
            return null;
        }
    }

    private static class TestResponsesReceiver extends ResponsesReceiver {
        @Override
        public  <I, D extends ITaskDataItemPayload<I>, R extends IResponseItem> ResponseReceiverContext
        process(TaskInfo task, ITaskTypeProcessor<I, D, R> processor) {
            return super.process(task, processor);
        }

        @Override
        protected Collection<Long> persistItems(TaskInfo taskInfo, Collection<? extends TaskDataItem> requests) {
            return null;
        }
    }
}
