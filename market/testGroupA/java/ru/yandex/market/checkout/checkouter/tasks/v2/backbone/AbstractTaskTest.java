package ru.yandex.market.checkout.checkouter.tasks.v2.backbone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.checkouter.tasks.v2.BatchProcessingResult;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.common.web.PingHandler;

public class AbstractTaskTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTaskTest.class);

    private static boolean pingEnabled = false;

    @BeforeAll
    public static void beforeAll() {
        pingEnabled = PingHandler.isEnabled();
        PingHandler.setEnabled(true);
    }

    @AfterAll
    public static void afterAll() {
        PingHandler.setEnabled(pingEnabled);
    }

    @Test
    public void sumNumbersTask() {
        List<Integer> storage = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        var task = new SumNumbersTask(storage);
        task.run(TaskRunType.MULTIPLE_TIMES);

        Assertions.assertEquals((Integer) storage.stream().mapToInt(item -> item).sum(), task.getResult());
    }

    @Test
    public void sumNumbersBatchTask() {
        List<Integer> storage = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        var task = new SumNumbersBatchTask(storage);
        task.run(TaskRunType.MULTIPLE_TIMES);

        Assertions.assertEquals((Integer) storage.stream().mapToInt(item -> item).sum(), task.getResult());
    }

    @Test
    public void afterProcessItemWhenProcessItemWithException() {
        var task = new ExceptionInProcessingTask();
        task.run(TaskRunType.ONCE);
        Assertions.assertTrue(task.isAfterProcessItemTriggered());
    }

    @Test
    public void logLastExceptionAndRequestIds() {
        var task = new ExceptionInProcessingTask();
        var result = task.run(TaskRunType.ONCE);
        Assertions.assertTrue(task.isAfterProcessItemTriggered());
        Assertions.assertTrue(result.getLastItemException().isPresent());
        Assertions.assertEquals(result.getLastItemException().get().getClass(), RuntimeException.class);
        Assertions.assertTrue(result.getLastItemExceptionRequestId().isPresent());
        Assertions.assertTrue(result.getLastItemRequestId().isPresent());
        Assertions.assertNotNull(result.getLastItemRequestId());
        Assertions.assertNotEquals(result.getLastItemRequestId().get(), result.getLastItemExceptionRequestId().get());
    }

    @Test
    public void interruptNoItemTask() {
        var task = new NoItemTask();
        var result = task.run(TaskRunType.ONCE);
        Assertions.assertEquals(TaskStageType.CANCELLED, result.getStage());
    }

    @Test
    public void noNeedToStartProduceWellLogs() {
        var currentTimeMillis = System.currentTimeMillis();
        var task = new NoNeedToStartTask();
        var result = task.run(TaskRunType.ONCE);

        Assertions.assertEquals(TaskStageType.IDLE, result.getStage());
        Assertions.assertTrue(currentTimeMillis <= result.getStartTimeMs());
        Assertions.assertTrue(currentTimeMillis <= result.getEndTimeMs());
    }

    @Test
    public void noItemTaskWithCron() {
        var maxExecutionTimeMillis = 100L;
        var taskWithCron = new NoItemTaskWithCron(maxExecutionTimeMillis, "1 * * * * ? *");
        var periodBetweenRunsMillis = taskWithCron.getMaxExecutionTimeMillis();
        Assertions.assertTrue(periodBetweenRunsMillis >= maxExecutionTimeMillis);

        var taskWithoutCron = new NoItemTaskWithCron(maxExecutionTimeMillis, "");
        var periodBetweenRunsMillisWithoutCron = taskWithoutCron.getMaxExecutionTimeMillis();
        Assertions.assertEquals(maxExecutionTimeMillis, periodBetweenRunsMillisWithoutCron);
    }

    class SumNumbersTask extends AbstractItemTask<Integer> {

        private final List<Integer> storage;
        private Integer result = 0;
        private AtomicInteger lastProcessed = new AtomicInteger(0);
        private Integer batchSize = 3;

        SumNumbersTask(List<Integer> storage) {
            super(5000, null);
            this.storage = storage;
        }

        @Override
        protected Long countItemsToProcess() {
            return (long) storage.size();
        }

        @Override
        protected Collection<Integer> prepareBatch() {
            return storage
                    .stream()
                    .filter(item -> item > lastProcessed.get())
                    .limit(batchSize)
                    .collect(Collectors.toList());
        }

        @Override
        protected void processItem(Integer item) {
            result += item;
            LOG.info("{} was processed", item);
        }

        @Override
        protected void afterProcessItem(Integer item) {
            lastProcessed.set(item);
            LOG.info("{} after process event", item);
        }

        public Integer getResult() {
            return result;
        }

        @Override
        public Map<String, Object> getParameters() {
            return Map.of();
        }
    }

    class SumNumbersBatchTask extends AbstractBatchTask<Integer> {

        private final List<Integer> storage;
        private Integer result = 0;
        private AtomicInteger lastProcessed = new AtomicInteger(0);
        private Integer batchSize = 3;

        SumNumbersBatchTask(List<Integer> storage) {
            super(1000, null);
            this.storage = storage;
        }

        @Override
        protected BatchProcessingResult processEntireBatch(Collection<Integer> batch) throws Exception {
            result += batch.stream().mapToInt(item -> item).sum();

            return new BatchProcessingResult(batch.size(), 0);
        }

        @Override
        protected Long countItemsToProcess() {
            return (long) storage.size();
        }

        @Override
        protected Collection<Integer> prepareBatch() {
            return storage
                    .stream()
                    .filter(item -> item > lastProcessed.get())
                    .limit(batchSize)
                    .collect(Collectors.toList());
        }

        @Override
        protected void afterProcessBatch(Collection<Integer> batch) {
            if (!batch.isEmpty()) {
                var list = new ArrayList<>(batch);
                lastProcessed.set(list.get(list.size() - 1));
            }
        }

        public Integer getResult() {
            return result;
        }

        @Override
        public Map<String, Object> getParameters() {
            return Map.of();
        }
    }

    class ExceptionInProcessingTask extends AbstractItemTask<Integer> {

        boolean afterProcessItemTriggered;

        ExceptionInProcessingTask() {
            super(1000, null);
        }

        @Override
        protected Long countItemsToProcess() {
            return 1L;
        }

        @Override
        protected Collection<Integer> prepareBatch() {
            return List.of(1, 2, 3);
        }

        @Override
        protected void processItem(Integer item) throws Exception {
            if (item == 1) {
                throw new Exception();
            } else if (item == 2) {
                throw new RuntimeException();
            }
        }

        @Override
        protected void afterProcessItem(Integer item) {
            afterProcessItemTriggered = true;
        }

        public boolean isAfterProcessItemTriggered() {
            return afterProcessItemTriggered;
        }

        @Override
        public Map<String, Object> getParameters() {
            return Map.of();
        }
    }

    class NoItemTask extends AbstractNoItemTask {

        protected NoItemTask() {
            super(100L, "");
        }

        @Override
        protected void process() throws Exception {
            LOG.info("something async");
            Thread.sleep(200L);
        }
    }

    class NoItemTaskWithCron extends AbstractNoItemTask {

        protected NoItemTaskWithCron(long maxExecutionTimeMillis, String cronExpression) {
            super(maxExecutionTimeMillis, cronExpression);
        }

        @Override
        protected void process() throws Exception {
            //do nothing
        }
    }

    class NoNeedToStartTask extends AbstractBatchTask<Integer> {


        NoNeedToStartTask() {
            super(0, null);
        }

        @Override
        protected boolean noNeedToStart() {
            return true;
        }

        @Override
        protected Long countItemsToProcess() {
            return null;
        }

        @Override
        protected Collection<Integer> prepareBatch() {
            return null;
        }

        @Override
        protected BatchProcessingResult processEntireBatch(Collection<Integer> batch) throws Exception {
            return null;
        }

        @Override
        public Map<String, Object> getParameters() {
            return Map.of();
        }
    }
}
