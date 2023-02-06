package ru.yandex.market.crm.tasks.primitives;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.Task;
import ru.yandex.market.crm.tasks.domain.TaskInstanceInfo;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.tasks.primitives.DynamicStepsSequenceTask.ContextFactory;
import ru.yandex.market.crm.tasks.services.ClusterTasksDAO;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceFactory;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceTestConfig;
import ru.yandex.market.mcrm.db.test.DbTestTool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author apershukov
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ClusterTasksServiceTestConfig.class)
public class DefaultSequenceTaskTest {

    private static class SubTaskData1 {

        @JsonProperty("foo")
        private String foo;

        String getFoo() {
            return foo;
        }

        void setFoo(String foo) {
            this.foo = foo;
        }
    }

    private static class SubTaskData2 {

        @JsonProperty("bar")
        private String bar;

        String getBar() {
            return bar;
        }

        void setBar(String bar) {
            this.bar = bar;
        }
    }

    private static class SubTask1 implements Task<PipelineContext, SubTaskData1> {

        @Nonnull
        @Override
        public ExecutionResult run(PipelineContext context, SubTaskData1 data, Control<SubTaskData1> control) {
            data = new SubTaskData1();
            data.setFoo("task_1");
            control.saveData(data);

            return ExecutionResult.completed();
        }

        @Override
        public String getId() {
            return SubTask1.class.getSimpleName();
        }
    }

    private static class SubTask2 implements Task<PipelineContext, SubTaskData2> {

        @Nonnull
        @Override
        public ExecutionResult run(PipelineContext context, SubTaskData2 data, Control<SubTaskData2> control) {
            data = new SubTaskData2();
            data.setBar("task_2");
            control.saveData(data);

            return ExecutionResult.completed();
        }

        @Override
        public String getId() {
            return SubTask2.class.getSimpleName();
        }
    }

    public static class DefaultSequenceTaskData extends SequenceTaskData {

        @JsonProperty("field")
        private String field = "default_value";

        String getField() {
            return field;
        }

        void setField(String field) {
            this.field = field;
        }
    }

    private static class PipelineContext {

        private String field;

        String getField() {
            return field;
        }

        void setField(String field) {
            this.field = field;
        }
    }

    private static class ProgressInfo {

        private final String stepId;
        private final int index;
        private final int total;

        ProgressInfo(String stepId, int index, int total) {
            this.stepId = stepId;
            this.index = index;
            this.total = total;
        }


        String getStepId() {
            return stepId;
        }

        int getIndex() {
            return index;
        }

        int getTotal() {
            return total;
        }
    }

    private abstract static class TestSequenceTask extends DefaultSequenceTask<
                Void,
                PipelineContext,
                DefaultSequenceTaskData> {

        TestSequenceTask(JsonDeserializer jsonDeserializer,
                         JsonSerializer jsonSerializer,
                         ContextFactory<Void, PipelineContext, DefaultSequenceTaskData> contextFactory,
                         Task<? super PipelineContext, ?>... steps) {
            super(jsonDeserializer, jsonSerializer, contextFactory, steps);
        }

        @Override
        public String getId() {
            return TASK_ID;
        }
    }

    private static final String TASK_ID = "task_id";
    private static final ContextFactory<Void, PipelineContext, DefaultSequenceTaskData> CONTEXT_PROVIDER =
            (parentContext, data) -> {
                PipelineContext context = new PipelineContext();
                if (data != null) {
                    context.setField(data.getField());
                }
                return context;
            };

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private ClusterTasksDAO clusterTasksDAO;

    @Inject
    private ClusterTasksServiceFactory serviceFactory;

    @Inject
    private DbTestTool dbTestTool;

    private ClusterTasksService service;

    @AfterEach
    public void tearDown() throws Exception {
        dbTestTool.runScript("/sql/clearDatabase.sql");
        if (service != null) {
            service.stop();
        }
    }

    /**
     * В случае если текущий шаг не заполнен таска начинает выполнение с первого шага
     *
     * При этом:
     * 1. При выполнении каждого шага доступно сохранение статуса, относящегося исключительно
     *    к этому шагу
     * 2. В каждый шаг передается контекст, сконструированный с использованием всей информации,
     *    связанной с таской
     * 3. После выполнения всех шагов вызывается метод onSuccess() таски
     */
    @Test
    void testExecuteAllSubtasks() throws Exception {
        CountDownLatch onSuccessLatch = new CountDownLatch(1);
        BlockingQueue<PipelineContext> queue = new ArrayBlockingQueue<>(1);

        Task<Void, DefaultSequenceTaskData> task = new TestSequenceTask(
                jsonDeserializer,
                jsonSerializer,
                CONTEXT_PROVIDER,
                new SubTask1(),
                new SubTask2() {
                    @Nonnull
                    @Override
                    public ExecutionResult run(PipelineContext context,
                                               SubTaskData2 data,
                                               Control<SubTaskData2> control) {
                        queue.add(context);
                        return super.run(context, data, control);
                    }
                }
        ) {
            @Override
            public void onSuccess(Void parentContext,
                                  PipelineContext context,
                                  DefaultSequenceTaskData status,
                                  Control<DefaultSequenceTaskData> control) {
                onSuccessLatch.countDown();
            }
        };

        startService(task);

        long taskId = service.submitTask(TASK_ID, null);

        assertTrue(
                onSuccessLatch.await(10, TimeUnit.SECONDS),
                "onSuccess() method has not been invoked"
        );

        service.stopGracefully();

        TaskInstanceInfo info = clusterTasksDAO.getTask(taskId);
        assertNotNull(info);

        SequenceTaskData data = jsonDeserializer.readObject(DefaultSequenceTaskData.class, info.getData());
        assertNotNull(data);

        Map<String, JsonNode> steps = data.getSteps();
        assertNotNull(steps);

        SubTaskData1 step1Data = jsonDeserializer.readObject(
                SubTaskData1.class,
                steps.get(SubTask1.class.getSimpleName()).toString()
        );

        assertNotNull(step1Data);
        assertEquals("task_1", step1Data.getFoo());

        SubTaskData2 step2Data = jsonDeserializer.readObject(
                SubTaskData2.class,
                steps.get(SubTask2.class.getSimpleName()).toString()
        );

        assertNotNull(step2Data);
        assertEquals("task_2", step2Data.getBar());

        PipelineContext context = queue.poll(10, TimeUnit.SECONDS);
        assertNotNull(context);
        assertEquals("default_value", context.getField());
    }

    /**
     * Таска начинает выполнение с того шага, который указан в её сохраненной информации
     */
    @Test
    void testContinuePipelineExecution() throws Exception {
        SubTaskData2 stepData = new SubTaskData2();
        stepData.setBar("bar");

        String secondStepId = SubTask2.class.getSimpleName();
        DefaultSequenceTaskData data = new DefaultSequenceTaskData();
        data.setCurrentStepId(secondStepId);
        data.setSteps(
                Collections.singletonMap(secondStepId, toNode(stepData))
        );

        TaskInstanceInfo taskInstanceInfo = new TaskInstanceInfo()
                .setTaskId(TASK_ID)
                .setStatus(TaskStatus.WAITING)
                .setData(jsonSerializer.writeObjectAsString(data));

        clusterTasksDAO.insertTask(taskInstanceInfo);

        CountDownLatch step1Latch = new CountDownLatch(1);
        BlockingQueue<SubTaskData2> queue = new ArrayBlockingQueue<>(1);

        Task<Void, DefaultSequenceTaskData> task = new TestSequenceTask(
                jsonDeserializer,
                jsonSerializer,
                CONTEXT_PROVIDER,
                new SubTask1() {
                    @Nonnull
                    @Override
                    public ExecutionResult run(PipelineContext context, SubTaskData1 data, Control<SubTaskData1> control) {
                        step1Latch.countDown();
                        return super.run(context, data, control);
                    }
                },
                new SubTask2() {
                    @Nonnull
                    @Override
                    public ExecutionResult run(PipelineContext context, SubTaskData2 data, Control<SubTaskData2> control) {
                        queue.add(data);
                        return super.run(context, data, control);
                    }
                }
        ) {};

        startService(task);

        assertFalse(
                step1Latch.await(2, TimeUnit.SECONDS),
                "First step has been executed"
        );

        SubTaskData2 dataFromInvocation = queue.poll(10, TimeUnit.SECONDS);
        assertNotNull(dataFromInvocation);
        assertEquals("bar", dataFromInvocation.getBar());
    }

    /**
     * При отмене таски вызывается метод onCancel() текущего шага
     */
    @Test
    void testIfTaskIsCancelledOnCancelMethodOfCurrentTaskIsCalled() throws Exception {
        SubTaskData2 stepData = new SubTaskData2();
        stepData.setBar("bar");

        String secondStepId = SubTask2.class.getSimpleName();
        DefaultSequenceTaskData data = new DefaultSequenceTaskData();
        data.setCurrentStepId(secondStepId);
        data.setSteps(
                Collections.singletonMap(secondStepId, toNode(stepData))
        );

        TaskInstanceInfo taskInstanceInfo = new TaskInstanceInfo()
                .setTaskId(TASK_ID)
                .setStatus(TaskStatus.WAITING)
                .setData(jsonSerializer.writeObjectAsString(data));

        long taskId = clusterTasksDAO.insertTask(taskInstanceInfo).getId();

        CountDownLatch step1OnCancelLatch = new CountDownLatch(1);
        CountDownLatch step2RunLatch = new CountDownLatch(1);

        BlockingQueue<PipelineContext> contextQueue = new ArrayBlockingQueue<>(1);
        BlockingQueue<SubTaskData2> dataQueue = new ArrayBlockingQueue<>(1);

        Task<Void, DefaultSequenceTaskData> task = new TestSequenceTask(
                jsonDeserializer,
                jsonSerializer,
                CONTEXT_PROVIDER,
                new SubTask1() {
                    @Override
                    public void onCancel(PipelineContext context,
                                         SubTaskData1 data,
                                         Control<SubTaskData1> control) {
                        step1OnCancelLatch.countDown();
                    }
                },
                new SubTask2() {
                    @Nonnull
                    @Override
                    public ExecutionResult run(PipelineContext context,
                                               SubTaskData2 data,
                                               Control<SubTaskData2> control) {
                        step2RunLatch.countDown();
                        return super.run(context, data, control);
                    }

                    @Override
                    public void onCancel(PipelineContext context,
                                         SubTaskData2 data,
                                         Control<SubTaskData2> control) {
                        contextQueue.add(context);
                        dataQueue.add(data);
                    }
                }
        ) {};

        service = serviceFactory.create(
                Collections.singleton(task)
        );

        service.cancelTask(taskId);

        service.start();

        assertFalse(
                step1OnCancelLatch.await(2, TimeUnit.SECONDS),
                "onCancel() of previous stop has been called"
        );

        assertFalse(
                step2RunLatch.await(2, TimeUnit.SECONDS),
                "run() of previous stop has been called"
        );

        PipelineContext context = contextQueue.poll(10, TimeUnit.SECONDS);
        assertNotNull(context);

        SubTaskData2 passedData = dataQueue.poll(10, TimeUnit.SECONDS);
        assertNotNull(passedData);
        assertEquals("bar", passedData.getBar());
    }

    /**
     * После прохождения каждого шага вызывается метод onStep()
     */
    @Test
    void testCallOnStepCompletedMethodAfterEachStep() throws Exception {
        BlockingQueue<ProgressInfo> queue = new ArrayBlockingQueue<>(2);

        Task<Void, DefaultSequenceTaskData> task = new TestSequenceTask(
                jsonDeserializer,
                jsonSerializer,
                CONTEXT_PROVIDER,
                new SubTask1(),
                new SubTask2()
        ) {
            @Override
            protected void onStep(Void parentContext, PipelineContext context, Task<? super PipelineContext, ?> task, int taskIndex, int totalTaskCount, DefaultSequenceTaskData data, Control<DefaultSequenceTaskData> control) {
                queue.add(
                        new ProgressInfo(task.getId(), taskIndex, totalTaskCount)
                );
            }
        };

        startService(task);
        service.submitTask(TASK_ID, null);

        ProgressInfo progressInfo = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(progressInfo);
        assertEquals(SubTask1.class.getSimpleName(), progressInfo.getStepId());
        assertEquals(0, progressInfo.getIndex());
        assertEquals(2, progressInfo.getTotal());

        progressInfo = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(progressInfo);
        assertEquals(SubTask2.class.getSimpleName(), progressInfo.getStepId());
        assertEquals(1, progressInfo.getIndex());
        assertEquals(2, progressInfo.getTotal());
    }

    /**
     * В случае если задачу остановили еще до начала её выполнения остановка происходит корректно
     *
     * https://st.yandex-team.ru/LILUCRM-2028
     */
    @Test
    void testPauseTaskWithNoSavedData() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);

        Task<Void, DefaultSequenceTaskData> task = new TestSequenceTask(
                jsonDeserializer,
                jsonSerializer,
                CONTEXT_PROVIDER,
                new SubTask1(),
                new SubTask2()
        ) {
            @Override
            protected void onPause(Void parentContext,
                                   PipelineContext context,
                                   DefaultSequenceTaskData data,
                                   Control<DefaultSequenceTaskData> control) throws Exception {
                barrier.await(10, TimeUnit.SECONDS);
            }
        };

        TaskInstanceInfo taskInstanceInfo = new TaskInstanceInfo()
                .setTaskId(TASK_ID)
                .setStatus(TaskStatus.WAITING);

        long taskId = clusterTasksDAO.insertTask(taskInstanceInfo).getId();

        service = serviceFactory.create(
                Collections.singleton(task)
        );

        service.pauseTask(taskId);

        service.start();

        barrier.await(10, TimeUnit.SECONDS);

        service.stopGracefully();

        TaskInstanceInfo currentInfo = clusterTasksDAO.getTask(taskId);
        assertNotNull(currentInfo);
        assertEquals(TaskStatus.PAUSED, currentInfo.getStatus());
        assertNull(currentInfo.getNodeKey());
    }

    @Test
    public void testCallOnSuccessMethodOfSubTask() throws Exception {
        CountDownLatch onSuccessLatch = new CountDownLatch(1);

        SubTask1 subTask = new SubTask1() {
            @Override
            public void onSuccess(PipelineContext context, SubTaskData1 data, Control<SubTaskData1> control) {
                onSuccessLatch.countDown();
            }
        };

        Task<Void, DefaultSequenceTaskData> task = new TestSequenceTask(
                jsonDeserializer,
                jsonSerializer,
                CONTEXT_PROVIDER,
                subTask
        ) {};

        startService(task);
        service.submitTask(TASK_ID, null);

        assertTrue(onSuccessLatch.await(10, TimeUnit.SECONDS));
    }

    /**
     * При возобновлении таски метод onRecover() должен вызываться только у текущей незавершенной подтаски
     */
    @Test
    void testCallOnRecoverMethodOfCurrentCancelledSubTask() throws Exception {
        SubTaskData2 stepData = new SubTaskData2();
        stepData.setBar("bar");

        String secondStepId = SubTask2.class.getSimpleName();
        DefaultSequenceTaskData data = new DefaultSequenceTaskData();
        data.setCurrentStepId(secondStepId);
        data.setSteps(
                Collections.singletonMap(secondStepId, toNode(stepData))
        );

        TaskInstanceInfo taskInstanceInfo = new TaskInstanceInfo()
                .setTaskId(TASK_ID)
                .setStatus(TaskStatus.WAITING)
                .setData(jsonSerializer.writeObjectAsString(data));

        long taskId = clusterTasksDAO.insertTask(taskInstanceInfo).getId();

        CountDownLatch step1OnRecoverLatch = new CountDownLatch(1);

        CountDownLatch step2RunLatch = new CountDownLatch(1);

        BlockingQueue<PipelineContext> contextQueue = new ArrayBlockingQueue<>(1);
        BlockingQueue<SubTaskData2> dataQueue = new ArrayBlockingQueue<>(1);

        Task<Void, DefaultSequenceTaskData> task = new TestSequenceTask(
                jsonDeserializer,
                jsonSerializer,
                CONTEXT_PROVIDER,
                new SubTask1() {
                    @Override
                    public void onRecover(PipelineContext context,
                                          SubTaskData1 data,
                                          Control<SubTaskData1> control) {
                        step1OnRecoverLatch.countDown();
                    }
                },
                new SubTask2() {
                    @Nonnull
                    @Override
                    public ExecutionResult run(PipelineContext context,
                                               SubTaskData2 data,
                                               Control<SubTaskData2> control) {
                        step2RunLatch.countDown();
                        return super.run(context, data, control);
                    }

                    @Override
                    public void onRecover(PipelineContext context,
                                          SubTaskData2 data,
                                          Control<SubTaskData2> control) {
                        contextQueue.add(context);
                        dataQueue.add(data);
                    }
                }
        ) {};

        service = serviceFactory.create(
                Collections.singleton(task)
        );

        service.cancelTask(taskId);

        service.start();

        assertFalse(
                step2RunLatch.await(2, TimeUnit.SECONDS),
                "run() of step has been called"
        );

        service.recoverTask(taskId);

        assertFalse(
                step1OnRecoverLatch.await(2, TimeUnit.SECONDS),
                "onRecover() of previous step has been called"
        );

        assertTrue(
                step2RunLatch.await(5, TimeUnit.SECONDS),
                "run() of step has not been called"
        );

        PipelineContext context = contextQueue.poll(10, TimeUnit.SECONDS);
        assertNotNull(context);

        SubTaskData2 passedData = dataQueue.poll(10, TimeUnit.SECONDS);
        assertNotNull(passedData);
        assertEquals("bar", passedData.getBar());
    }

    /**
     * Если в статусе таски в качестве текущего шага указан идентификатор неизвестной таски,
     * успешно выполняется метод onFail().
     */
    @Test
    void testExecuteOnFailMethodIfCurrentStepIdIsUnknown() throws InterruptedException {
        var latch = new CountDownLatch(1);

        var task = new TestSequenceTask(
                jsonDeserializer,
                jsonSerializer,
                CONTEXT_PROVIDER,
                new SubTask1()
        ) {
            @Override
            public void onFail(String message,
                               Void parentContext,
                               DefaultSequenceTaskData data,
                               Control<DefaultSequenceTaskData> control) throws Exception {
                super.onFail(message, parentContext, data, control);
                latch.countDown();
            }
        };

        var data = new DefaultSequenceTaskData();
        data.setCurrentStepId("unknown_step_id");

        var taskInstanceInfo = new TaskInstanceInfo()
                .setTaskId(TASK_ID)
                .setStatus(TaskStatus.WAITING)
                .setData(jsonSerializer.writeObjectAsString(data));

        clusterTasksDAO.insertTask(taskInstanceInfo);

        startService(task);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private void startService(Task<Void, ?> task) {
        service = serviceFactory.create(
                Collections.singleton(task)
        );
        service.start();
    }

    private JsonNode toNode(Object object) {
        String json = jsonSerializer.writeObjectAsString(object);
        return jsonDeserializer.readObject(JsonNode.class, json);
    }
}
