package ru.yandex.market.crm.tasks.primitives;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
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
import ru.yandex.market.crm.tasks.services.ClusterTasksDAO;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceFactory;
import ru.yandex.market.crm.tasks.test.ClusterTasksServiceTestConfig;
import ru.yandex.market.mcrm.db.test.DbTestTool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ClusterTasksServiceTestConfig.class)
public class DynamicStepsSequenceTaskTest {

    private static final String TASK_ID = "task_id";

    private static final SequenceTask.ContextFactory<Void, PipelineContext, DefaultSequenceTaskData> CONTEXT_PROVIDER =
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
     * <p>
     * При этом:
     * 1. При выполнении каждого шага доступно сохранение статуса, относящегося исключительно
     * к этому шагу
     * 2. В каждый шаг передается контекст, сконструированный с использованием всей информации,
     * связанной с таской
     * 3. После выполнения всех шагов вызывается метод onSuccess() таски
     */
    @Test
    void testExecuteAllSubtasks() throws Exception {
        CountDownLatch onSuccessLatch = new CountDownLatch(1);
        BlockingQueue<PipelineContext> queue = new ArrayBlockingQueue<>(1);

        var subTask1 = new SubTask("task_1");
        var subTask2 = new SubTask("task_2") {
            @Nonnull
            @Override
            public ExecutionResult run(PipelineContext context,
                                       SubTaskData data,
                                       Control<SubTaskData> control) {
                queue.add(context);
                return super.run(context, data, control);
            }
        };

        Task<Void, DefaultSequenceTaskData> task = new TestSequenceTask(
                jsonDeserializer,
                jsonSerializer,
                CONTEXT_PROVIDER
        ) {
            @Override
            public void onSuccess(Void parentContext,
                                  PipelineContext context,
                                  DefaultSequenceTaskData status,
                                  Control<DefaultSequenceTaskData> control) {
                onSuccessLatch.countDown();
            }

            @Override
            protected List<SubTask> getSteps(DefaultSequenceTaskData data) {
                return List.of(subTask1, subTask2);
            }

            @Override
            protected int getTotalTaskCount(DefaultSequenceTaskData data) {
                return getSteps(data).size();
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

        SubTaskData step1Data = jsonDeserializer.readObject(
                SubTaskData.class,
                steps.get(subTask1.getId()).toString()
        );

        assertNotNull(step1Data);
        assertEquals("task_1", step1Data.getFoo());

        SubTaskData step2Data = jsonDeserializer.readObject(
                SubTaskData.class,
                steps.get(subTask2.getId()).toString()
        );

        assertNotNull(step2Data);
        assertEquals("task_2", step2Data.getFoo());

        PipelineContext context = queue.poll(10, TimeUnit.SECONDS);
        assertNotNull(context);
        assertEquals("default_value", context.getField());
    }

    private void startService(Task<Void, ?> task) {
        service = serviceFactory.create(
                Collections.singleton(task)
        );
        service.start();
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

    private abstract static class TestSequenceTask extends DynamicStepsSequenceTask<
            Void,
            PipelineContext,
            DefaultSequenceTaskData,
            SubTask> {

        TestSequenceTask(JsonDeserializer jsonDeserializer,
                         JsonSerializer jsonSerializer,
                         ContextFactory<Void, PipelineContext, DefaultSequenceTaskData> contextFactory) {
            super(jsonDeserializer, jsonSerializer, contextFactory);
        }

        @Override
        public String getId() {
            return TASK_ID;
        }
    }

    private static class SubTask implements Task<PipelineContext, SubTaskData> {

        private final String foo;

        public SubTask(String foo) {
            this.foo = foo;
        }

        @Nonnull
        @Override
        public ExecutionResult run(PipelineContext context, SubTaskData data, Control<SubTaskData> control) {
            data = new SubTaskData();
            data.setFoo(foo);
            control.saveData(data);

            return ExecutionResult.completed();
        }

        @Override
        public String getId() {
            return SubTask.class.getSimpleName() + "-" + foo;
        }
    }

    private static class SubTaskData {

        @JsonProperty("foo")
        private String foo;

        String getFoo() {
            return foo;
        }

        void setFoo(String foo) {
            this.foo = foo;
        }
    }
}
