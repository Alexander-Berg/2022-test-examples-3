package ru.yandex.market.markup2.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.market.markup2.AppContext;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.dao.HitmanExecutionPersister;
import ru.yandex.market.markup2.dao.TaskDataItemPersister;
import ru.yandex.market.markup2.dao.TaskStatisticPersister;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.config.TaskConfigState;
import ru.yandex.market.markup2.entries.group.TaskConfigGroupInfo;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.entries.type.TaskTypeInfo;
import ru.yandex.market.markup2.processors.task.ProgressStatus;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.workflow.TaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.generation.RequestGeneratorContext;
import ru.yandex.market.markup2.workflow.hitman.HitmanApiHandler;
import ru.yandex.market.markup2.workflow.pipe.Pipes;
import ru.yandex.market.markup2.workflow.requestSender.RequestsSender;
import ru.yandex.market.markup2.workflow.responseReceiver.ResponsesReceiver;
import ru.yandex.market.markup2.workflow.resultMaker.ResultMakerContext;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueStrategy;
import ru.yandex.market.markup2.workflow.taskType.TaskTypeContainer;
import ru.yandex.market.markup2.workflow.taskType.processor.AbstractTaskTypeProcessor;
import ru.yandex.market.markup2.workflow.taskType.processor.HitmanTaskTypeProcessor;
import ru.yandex.market.markup2.workflow.taskType.processor.ITaskTypeProcessor;
import ru.yandex.market.markup2.workflow.taskType.processor.SimpleDataItemsProcessor;
import ru.yandex.qe.hitman.main.api.v1.dto.JobStatusDto;
import ru.yandex.qe.hitman.main.api.v1.dto.StartedExecution;
import ru.yandex.qe.hitman.main.data.model.Status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyInt;
import static ru.yandex.market.markup2.utils.DummyTestTask.DummyTaskIdentity;
import static ru.yandex.market.markup2.utils.DummyTestTask.DummyTaskPayload;
import static ru.yandex.market.markup2.utils.DummyTestTask.DummyTaskResponse;


/**
 * @author york
 * @since 04.07.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class TestHitmanRequestSending {
    private AppContext appContext;
    private AbstractTaskTypeProcessor<DummyTaskIdentity, DummyTaskPayload, DummyTaskResponse> processor;
    private TasksCache tasksCache;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(
                new SimpleModule()
                    .addSerializer(DummyTaskResponse.class, new JsonUtils.DefaultJsonSerializer())
            );
        appContext = new AppContext();
        tasksCache = new TasksCache();
        appContext.setHitmanExecutionPersister(Mockito.mock(HitmanExecutionPersister.class));

        TaskTypesContainers taskTypesContainers = Mockito.mock(TaskTypesContainers.class);
        TaskTypeContainer container = new TaskTypeContainer();
        container.setMinBatchCount(10);
        container.setMaxBatchCount(10);
        Mockito.when(taskTypesContainers.getTaskTypeContainer(anyInt())).thenReturn(container);
        appContext.setTaskTypesContainers(taskTypesContainers);

        SimpleDataItemsProcessor<DummyTaskIdentity, DummyTaskPayload, DummyTaskResponse> dataItemsProcessor =
            new SimpleDataItemsProcessor<DummyTaskIdentity, DummyTaskPayload, DummyTaskResponse>() {
                @Override
                public JsonSerializer<? super TaskDataItem<DummyTaskPayload, DummyTaskResponse>>
                getRequestSerializer() {
                    return new JsonUtils.DefaultJsonSerializer<>();
                }

                @Override
                public Class<DummyTaskResponse> getResponseClass() {
                    return DummyTaskResponse.class;
                }

                @Override
                public JsonDeserializer<? extends DummyTaskResponse> getResponseDeserializer() {
                    return new JsonUtils.DefaultJsonDeserializer<>(DummyTaskResponse.class);
                }
            };
        processor = new HitmanTaskTypeProcessor<DummyTaskIdentity, DummyTaskPayload, DummyTaskResponse>(
                dataItemsProcessor) {
            @Override
            public void generateRequests(
                RequestGeneratorContext<DummyTaskIdentity, DummyTaskPayload, DummyTaskResponse> context) {
                context.createTaskDataItems(generatePayloads(context.getLeftToGenerate()));
            }

            @Override
            public void makeResults(ResultMakerContext<DummyTaskIdentity, DummyTaskPayload,
                DummyTaskResponse> context) {
            }
        };
    }

    private class TestRequestsSender extends RequestsSender {
        TestRequestsSender() {
            setTasksCache(tasksCache);
            setTaskProcessManager(Mockito.mock(TaskProcessManager.class));
        }
        void process(TaskInfo taskInfo) {
            tasksCache.addTaskInfo(taskInfo);
            ReflectionTestUtils.setField(this, "threadsPool", Mockito.mock(ScheduledExecutorService.class));
            ReflectionTestUtils.invokeMethod(this, "exclusiveTaskProcess", taskInfo.getId());
        }
        @Override
        protected TaskTypeContainer getTaskTypeContainer(TaskInfo task) {
            return new TaskTypeContainer() {
                @Override
                public ITaskTypeProcessor getProcessor() {
                    return processor;
                }
            };
        }
        @Override
        public AppContext getAppContext() {
            return appContext;
        }
        @Override
        public TaskStatisticPersister getTaskStatisticPersister() {
            return Mockito.mock(TaskStatisticPersister.class);
        }
        @Override
        public TaskDataItemPersister getDataItemPersister() {
            return Mockito.mock(TaskDataItemPersister.class);
        }
    }

    private class TestResponseReceiver extends ResponsesReceiver {
        TestResponseReceiver() {
            setTasksCache(tasksCache);
            setTaskProcessManager(Mockito.mock(TaskProcessManager.class));
        }

        void process(TaskInfo taskInfo) {
            tasksCache.addTaskInfo(taskInfo);
            ReflectionTestUtils.setField(this, "threadsPool", Mockito.mock(ScheduledExecutorService.class));
            ReflectionTestUtils.invokeMethod(this, "exclusiveTaskProcess", taskInfo.getId());
        }
        @Override
        protected TaskTypeContainer getTaskTypeContainer(TaskInfo task) {
            return new TaskTypeContainer() {
                @Override
                public ITaskTypeProcessor getProcessor() {
                    return processor;
                }
            };
        }
        @Override
        public AppContext getAppContext() {
            return appContext;
        }
        @Override
        public TaskStatisticPersister getTaskStatisticPersister() {
            return Mockito.mock(TaskStatisticPersister.class);
        }
        @Override
        public TaskDataItemPersister getDataItemPersister() {
            return Mockito.mock(TaskDataItemPersister.class);
        }
    }


    @Test
    public void doTest() throws Exception {
        int idSeq = 10;
        TaskTypeInfo taskTypeInfo = new TaskTypeInfo(idSeq++, "dummy", "hitman-mitman", 1, 1,
            Pipes.SEQUENTIALLY, TaskDataUniqueStrategy.TYPE_CATEGORY);

        TaskConfigGroupInfo groupConfigInfo = new TaskConfigGroupInfo.Builder()
            .setCategoryId(1000)
            .setTypeInfo(taskTypeInfo)
            .setId(idSeq++)
            .build();
        TaskConfigInfo taskConfigInfo = new TaskConfigInfo.Builder()
            .setCount(10)
            .setGroupInfo(groupConfigInfo)
            .setId(idSeq++)
            .setState(TaskConfigState.ACTIVE)
            .build();
        TaskInfo taskInfo = new TaskInfo.Builder()
            .setId(idSeq++)
            .setConfig(taskConfigInfo)
            .setState(TaskState.RUNNING)
            .build();

        HitmanApiHandler hitmanApiHandler = Mockito.mock(HitmanApiHandler.class);
        Map<String, JobStatusDto> statuses = new HashMap<>();
        Multimap<String, JsonNode> responses = ArrayListMultimap.create();
        final String gExecutionId = RandomStringUtils.randomNumeric(5);
        Mockito.when(hitmanApiHandler.checkStatus(Mockito.anyString())).thenAnswer(invocation -> {
            String executionId = invocation.getArgument(0);
            return statuses.get(executionId);
        });
        Mockito.when(hitmanApiHandler.startExecution(Mockito.anyString(),
            Mockito.anyCollection(),
            Mockito.any(), Mockito.anyMap())).thenAnswer(
                invocation -> new StartedExecution(gExecutionId, "url lalala")
        );
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String executionId = invocation.getArgument(0);
                Consumer<JsonNode> consumer = invocation.getArgument(1);
                for (JsonNode resp : responses.get(executionId)) {
                    consumer.accept(resp);
                }
                return null;
            }
        }).when(hitmanApiHandler).downloadResult(Mockito.anyString(), Mockito.any());
        appContext.setHitmanApiHandler(hitmanApiHandler);

        int itemsSize = 10;
        Collection<TaskDataItem<DummyTaskPayload, DummyTaskResponse>> dataItems = generateItems(itemsSize);

        taskInfo.getProgress().changeDataItemsState(dataItems, null, TaskDataItemState.GENERATED);
        taskInfo.getProgress().updateTaskProgress(b ->
            b.incGeneratedCount(itemsSize).setRequestsGenerationStatus(ProgressStatus.FINISHED)
        );
        Assert.assertEquals(itemsSize, taskInfo.getProgress().getDataItemsByState(TaskDataItemState.GENERATED).size());

        //send requests
        new TestRequestsSender().process(taskInfo);
        Assert.assertEquals(itemsSize, taskInfo.getProgress().getDataItemsByState(TaskDataItemState.SENT).size());
        Assert.assertEquals(ProgressStatus.FINISHED, taskInfo.getTaskStatus().getSendingStatus());
        Assert.assertEquals(itemsSize, taskInfo.getTaskStatus().getSentCount());

        //receive responses
        TestResponseReceiver responseReceiver = new TestResponseReceiver();
        statuses.put(gExecutionId, generate(gExecutionId, Status.RUNNING));
        responseReceiver.process(taskInfo);
        Assert.assertEquals(itemsSize, taskInfo.getProgress().getDataItemsByState(TaskDataItemState.SENT).size());
        Assert.assertEquals(ProgressStatus.NOT_STARTED, taskInfo.getTaskStatus().getReceivingStatus());
        //failed in hitman
        statuses.put(gExecutionId, generate(gExecutionId, Status.FAILED));
        responseReceiver.process(taskInfo);
        Assert.assertEquals(itemsSize, taskInfo.getProgress().getDataItemsByState(
            TaskDataItemState.FAILED_RESPONSE).size());
        Assert.assertEquals(itemsSize, taskInfo.getTaskStatus().getReceivedFailedCount());
        Assert.assertEquals(ProgressStatus.PROCESSED, taskInfo.getTaskStatus().getReceivingStatus());
        //once again failed
        responseReceiver.process(taskInfo);
        Assert.assertEquals(ProgressStatus.PROCESSED, taskInfo.getTaskStatus().getReceivingStatus());

        statuses.put(gExecutionId, generate(gExecutionId, Status.SUCCEEDED));
        int lost = 1;
        int cannot = 2;
        int i = -1;
        for (TaskDataItem<DummyTaskPayload, DummyTaskResponse> item : dataItems) {
            if (++i < lost) {
                continue;
            } else {
                DummyTaskResponse resp = DummyTaskResponse.generateResponse(item.getId(), i < lost + cannot);
                responses.put(gExecutionId, objectMapper.valueToTree(resp));
            }
        }
        responseReceiver.process(taskInfo);
        Assert.assertEquals(0, taskInfo.getProgress().getDataItemsByState(
            TaskDataItemState.FAILED_RESPONSE).size());
        Assert.assertEquals(cannot, taskInfo.getProgress().getDataItemsByState(
            TaskDataItemState.CANNOT_RESPONSE).size());
        Assert.assertEquals(lost, taskInfo.getProgress().getDataItemsByState(
            TaskDataItemState.LOST_RESPONSE).size());
        Assert.assertEquals(itemsSize - lost - cannot, taskInfo.getProgress().getDataItemsByState(
            TaskDataItemState.SUCCESSFUL_RESPONSE).size());
        Assert.assertEquals(ProgressStatus.FINISHED, taskInfo.getTaskStatus().getReceivingStatus());
    }

    private JobStatusDto generate(String execId, Status status) {
        return new JobStatusDto(1L, Long.parseLong(execId),
            "", status, 0.2, "ahahaha");
    }

    private Collection<TaskDataItem<DummyTaskPayload, DummyTaskResponse>> generateItems(int quantity) {
        AtomicLong ids = new AtomicLong(50);
        return generatePayloads(quantity).stream().map(
                p -> new TaskDataItem<DummyTaskPayload, DummyTaskResponse>(ids.incrementAndGet(), p)
            ).collect(Collectors.toList());
    }

    private Collection<DummyTaskPayload> generatePayloads(int quantity) {
        Collection<DummyTaskPayload> result = new ArrayList<>();
        Random rnd = new Random(1);
        for (int j = 0; j < quantity; j++) {
            DummyTaskPayload payload = new DummyTaskPayload(j,
                RandomStringUtils.randomAscii(rnd.nextInt(10) + 1));
            result.add(payload);
        }
        return result;
    }
}
