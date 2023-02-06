package ru.yandex.market.crm.campaign.services.tasks.templates;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.crm.tasks.test.ClusterTasksServiceTestConfig;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.Task;
import ru.yandex.market.crm.tasks.primitives.PipelineStepTask;
import ru.yandex.market.crm.tasks.services.ClusterTasksDAO;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;
import ru.yandex.market.crm.tasks.services.TaskIncidentsDAO;
import ru.yandex.market.mcrm.tx.TxService;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.crm.tasks.utils.TaskFactory.pipelineTask;

/**
 * @author zloddey
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ClusterTasksServiceTestConfig.class)
@TestPropertySource("/mcrm_int_test.properties")
public class PipelineStepWrapperTest {
    protected static final Duration PING_INTERVAL = Duration.ofSeconds(1);
    @Inject
    private ClusterTasksDAO taskDao;
    @Inject
    private TaskIncidentsDAO incidentDao;
    @Inject
    private JsonDeserializer deserializer;
    @Inject
    private JsonSerializer serializer;
    @Inject
    private TxService txService;

    private static class TestData {
        @JsonProperty("counter")
        private int counter;

        public int getCounter() {
            return counter;
        }

        public void setCounter(int counter) {
            this.counter = counter;
        }
    }

    private static class OldFashionedPipelineTask implements PipelineStepTask<Void, TestData> {
        private final CountDownLatch latch;
        private final List<String> runLog;

        public OldFashionedPipelineTask(CountDownLatch latch, List<String> runLog) {
            this.latch = latch;
            this.runLog = runLog;
        }

        @Nonnull
        @Override
        public ExecutionResult run(Void context, TestData data, Control<TestData> control) throws Exception {
            runLog.add("old");
            return iterate(data, control, latch);
        }

        @Nonnull
        @Override
        public String getDescription() {
            return "Класс старого типа наследует PipelineStepTask напрямую";
        }

        @Override
        public String getId() {
            return PipelineStepWrapperTest.class.getName();
        }
    }

    private static class NewStylePipelineTask implements Task<Void, TestData> {
        private final CountDownLatch latch;
        private final List<String> runLog;

        public NewStylePipelineTask(CountDownLatch latch, List<String> runLog) {
            this.latch = latch;
            this.runLog = runLog;
        }

        @Nonnull
        @Override
        public ExecutionResult run(Void context, TestData data, Control<TestData> control) throws Exception {
            runLog.add("new");
            return iterate(data, control, latch);
        }

        @Override
        public String getId() {
            return PipelineStepWrapperTest.class.getName();
        }
    }

    @Nonnull
    private static ExecutionResult iterate(TestData data, Control<TestData> control, CountDownLatch latch) {
        if (data == null) {
            data = new TestData();
            data.setCounter(3);
        }
        if (data.getCounter() <= 0) {
            latch.countDown();
            return ExecutionResult.completed();
        }
        data.setCounter(data.getCounter() - 1);
        control.saveData(data);
        latch.countDown();
        return ExecutionResult.repeatIn(PING_INTERVAL);
    }

    /**
     * Таск нового типа должен подхватить данные запущенного старого таска (у них одинаковый идентификатор)
     * и отработать до конца без проблем.
     */
    @Test
    public void forwardMigration() throws InterruptedException {
        List<String> runLog = new ArrayList<>();

        runTask(1, true, latch -> new OldFashionedPipelineTask(latch, runLog));
        assertEquals(List.of("old"), runLog);

        runTask(2, false, latch ->
                pipelineTask("Класс нового типа", new NewStylePipelineTask(latch, runLog)));
        assertEquals(List.of("old", "new", "new"), runLog);
    }

    /**
     * Таск старого типа должен подхватить данные запущенного нового таска (у них одинаковый идентификатор)
     * и отработать до конца без проблем.
     * <p>
     * Тест нужен на случай, если придётся откатывать изменения обратно, и при этом часть новых тасков
     * уже успеет запуститься.
     */
    @Test
    public void backwardMigration() throws InterruptedException {
        List<String> runLog = new ArrayList<>();

        runTask(1, true, latch ->
                pipelineTask("Класс нового типа", new NewStylePipelineTask(latch, runLog)));
        assertEquals(List.of("new"), runLog);

        runTask(2, false, latch -> new OldFashionedPipelineTask(latch, runLog));
        assertEquals(List.of("new", "old", "old"), runLog);
    }

    private void runTask(int steps, boolean submit, Function<CountDownLatch, Task<Void, TestData>> taskFactory) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(steps);
        var task = taskFactory.apply(latch);
        ClusterTasksService taskRunner = makeClusterTaskService(task);
        taskRunner.start();
        if (submit) {
            taskRunner.submitTask(task.getId(), null);
        }

        latch.await(5, TimeUnit.SECONDS);
        taskRunner.stopGracefully();
    }

    private ClusterTasksService makeClusterTaskService(Task<Void, TestData> task) {
        return new ClusterTasksService(taskDao, incidentDao, deserializer, serializer, txService, List.of(task));
    }
}
