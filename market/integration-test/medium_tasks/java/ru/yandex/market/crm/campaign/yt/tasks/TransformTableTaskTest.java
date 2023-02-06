package ru.yandex.market.crm.campaign.yt.tasks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.object.FieldsBindingStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.NullSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.market.crm.campaign.test.AbstractServiceMediumTest;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.util.Exceptions.TrashFunction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author apershukov
 */
public class TransformTableTaskTest extends AbstractServiceMediumTest {

    @YTreeObject(
            bindingStrategy = FieldsBindingStrategy.ANNOTATED_ONLY,
            nullSerializationStrategy = NullSerializationStrategy.IGNORE_NULL_FIELDS
    )
    private static class InputClass {

        @YTreeField(key = "column_1")
        private String column;

        InputClass(String column) {
            this.column = column;
        }

        String getColumn() {
            return column;
        }
    }

    @YTreeObject(
            bindingStrategy = FieldsBindingStrategy.ANNOTATED_ONLY,
            nullSerializationStrategy = NullSerializationStrategy.IGNORE_NULL_FIELDS
    )
    private static class OutputClass {

        @YTreeField(key = "column_2")
        private String column;

        OutputClass(String column) {
            this.column = column;
        }

        String getColumn() {
            return column;
        }
    }

    public static class TestTaskData implements ReadYtTableTaskData {

        @JsonProperty("rowsProcessed")
        private long rowsProcessed;

        @JsonProperty("totalRows")
        private long totalRows;

        TestTaskData(int rowsProcessed, int totalRows) {
            this.rowsProcessed = rowsProcessed;
            this.totalRows = totalRows;
        }

        @SuppressWarnings("unused")
        public TestTaskData() {
        }

        @Override
        public long getRowsProcessed() {
            return rowsProcessed;
        }

        @Override
        public void setRowsProcessed(long rowsProcessed) {
            this.rowsProcessed = rowsProcessed;
        }

        @Override
        public long getTotalRows() {
            return totalRows;
        }

        @Override
        public void setTotalRows(long totalRows) {
            this.totalRows = totalRows;
        }
    }

    private static abstract class DefaultTestTaskImpl extends TransformTableTask<Void, TestTaskData, InputClass,
            OutputClass> {

        DefaultTestTaskImpl(YtClient ytClient) {
            super(ytClient, 1, InputClass.class, OutputClass.class);
        }

        @Override
        protected YPath getInputTable(Void context, TestTaskData data) {
            return INPUT_TABLE;
        }

        @Override
        protected YPath getOutputTable(Void context, TestTaskData data) {
            return OUTPUT_TABLE;
        }
    }

    private static class One2OneTestTaskImpl extends DefaultTestTaskImpl {

        private final TrashFunction<InputClass, OutputClass> mapper;

        One2OneTestTaskImpl(YtClient ytClient,
                            TrashFunction<InputClass, OutputClass> mapper) {
            super(ytClient);
            this.mapper = mapper;
        }

        @Nonnull
        @Override
        protected List<OutputClass> transform(InputClass row, Void context, TestTaskData data) {
            return Collections.singletonList(mapper.apply(row));
        }
    }

    private static class ControlStub implements Control<TestTaskData> {

        private volatile TestTaskData saved;

        @Override
        public void saveData(TestTaskData data) {
            saved = data;
        }

        TestTaskData getSaved() {
            return saved;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(TransformTableTaskTest.class);

    private static final YPath DIR = YPath.cypressRoot().child(TransformTableTaskTest.class.getSimpleName());
    private static final YPath INPUT_TABLE = DIR.child("input_table");
    private static final YPath OUTPUT_TABLE = DIR.child("output_table");

    @Inject
    private YtClient ytClient;

    @AfterEach
    public void tearDown() {
        ytClient.remove(INPUT_TABLE);
        ytClient.remove(OUTPUT_TABLE);
    }

    /**
     * При отсутствии данных таски таблица трансформируется с первой строки в полном объеме
     */
    @Test
    public void testTransformFullTable() throws Exception {
        ytClient.write(INPUT_TABLE, InputClass.class, Arrays.asList(
                new InputClass("value_1"),
                new InputClass("value_2"),
                new InputClass("value_3")
        ));

        One2OneTestTaskImpl task = createTask(row -> new OutputClass(row.getColumn() + " [altered]"));

        ControlStub control = new ControlStub();
        ExecutionResult result = task.run(null, null, control);
        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETING, result.getNextStatus());

        TestTaskData data = control.getSaved();
        assertNotNull(data);
        assertEquals(3, data.getTotalRows());
        assertEquals(3, data.getRowsProcessed());

        List<OutputClass> output = ytClient.read(OUTPUT_TABLE, OutputClass.class);
        assertEquals(3, output.size());
        assertEquals("value_1 [altered]", output.get(0).getColumn());
        assertEquals("value_2 [altered]", output.get(1).getColumn());
        assertEquals("value_3 [altered]", output.get(2).getColumn());
    }

    /**
     * В случае если при старте таски уже есть данные об её прошлом выполнении
     * обработка начинается с первое необработанной строки
     */
    @Test
    public void testStartReadingFromUnprocessedRow() throws Exception {
        ytClient.write(INPUT_TABLE, InputClass.class, Arrays.asList(
                new InputClass("value_1"),
                new InputClass("value_2"),
                new InputClass("value_3")
        ));

        ytClient.write(OUTPUT_TABLE, OutputClass.class, Arrays.asList(
                new OutputClass("value_1"),
                new OutputClass("value_2")
        ));

        var task = createTask(row -> new OutputClass(row.getColumn() + " [altered]"));

        TestTaskData data = new TestTaskData(2, 3);
        ControlStub control = new ControlStub();
        ExecutionResult result = task.run(null, data, control);

        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETING, result.getNextStatus());

        TestTaskData savedData = control.getSaved();
        assertNotNull(savedData);
        assertEquals(3, savedData.getTotalRows());
        assertEquals(3, savedData.getRowsProcessed());

        List<OutputClass> output = ytClient.read(OUTPUT_TABLE, OutputClass.class);
        assertEquals(3, output.size());
        assertEquals("value_1", output.get(0).getColumn());
        assertEquals("value_2", output.get(1).getColumn());
        assertEquals("value_3 [altered]", output.get(2).getColumn());
    }

    /**
     * В случае если при выполнении таски её поток прерывается она переходит в состояние
     * WAITING. Имеющиеся результаты обработки при этом сохраняются.
     */
    @Test
    public void testStopReadingOnThreadInterruption() throws Exception {
        ytClient.write(INPUT_TABLE, InputClass.class, Arrays.asList(
                new InputClass("value_1"),
                new InputClass("value_2"),
                new InputClass("value_3")
        ));


        CyclicBarrier barrier = new CyclicBarrier(2);
        var task = createTask(row -> {
            var column = row.getColumn();
            if ("value_3".equals(column)) {
                barrier.await(10, TimeUnit.SECONDS);
                new CountDownLatch(1).await();
            }
            return new OutputClass(column + " [altered]");
        });

        ControlStub control = new ControlStub();

        AtomicReference<ExecutionResult> result = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            try {
                result.set(task.run(null, null, control));
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });

        thread.start();

        barrier.await(10, TimeUnit.SECONDS);

        thread.interrupt();
        thread.join();

        ExecutionResult executionResult = result.get();
        assertNotNull(executionResult);
        assertEquals(TaskStatus.WAITING, executionResult.getNextStatus());

        TestTaskData data = control.getSaved();
        assertNotNull(data);
        assertEquals(3, data.getTotalRows());

        var rowsProcessed = data.getRowsProcessed();
        assertThat(rowsProcessed, lessThan(3L));

        List<OutputClass> output = ytClient.read(OUTPUT_TABLE, OutputClass.class);
        assertEquals(rowsProcessed, output.size());

        for (int i = 0; i < rowsProcessed; ++i) {
            assertEquals("value_%d [altered]".formatted(i + 1), output.get(i).getColumn());
        }
    }

    /**
     * В случае если до начала трансформации таблицы на пути результата что-то есть
     * старая таблица удаляется.
     */
    @Test
    public void testOverrideOutputTableIfTransformingFromBegining() throws Exception {
        ytClient.write(INPUT_TABLE, InputClass.class, Arrays.asList(
                new InputClass("value_1"),
                new InputClass("value_2"),
                new InputClass("value_3")
        ));

        ytClient.write(OUTPUT_TABLE, OutputClass.class, Arrays.asList(
                new OutputClass("value_1"),
                new OutputClass("value_2")
        ));

        One2OneTestTaskImpl task = createTask(row -> new OutputClass(row.getColumn() + " [altered]"));

        ControlStub control = new ControlStub();
        task.run(null, null, control);

        List<OutputClass> output = ytClient.read(OUTPUT_TABLE, OutputClass.class);
        assertEquals(3, output.size());
        assertEquals("value_1 [altered]", output.get(0).getColumn());
        assertEquals("value_2 [altered]", output.get(1).getColumn());
        assertEquals("value_3 [altered]", output.get(2).getColumn());
    }

    /**
     * Если последняя строка трансформируется в пустой список строк задача заввершается
     * корректно
     */
    @Test
    public void testFinishTransformingWhenLastRowsTransformsToEmptyList() throws Exception {
        ytClient.write(INPUT_TABLE, InputClass.class, Arrays.asList(
                new InputClass("value_1"),
                new InputClass("value_2"),
                new InputClass("value_3")
        ));

        DefaultTestTaskImpl task = new DefaultTestTaskImpl(ytClient) {

            @Nonnull
            @Override
            protected List<OutputClass> transform(InputClass row, Void context, TestTaskData data) {
                return "value_3".equals(row.getColumn())
                        ? List.of()
                        : List.of(new OutputClass(row.getColumn() + " [altered]"));
            }
        };

        ControlStub control = new ControlStub();
        ExecutionResult result = task.run(null, null, control);

        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETING, result.getNextStatus());

        TestTaskData data = control.getSaved();
        assertEquals(3, data.getTotalRows());
        assertEquals(3, data.getRowsProcessed());

        List<OutputClass> output = ytClient.read(OUTPUT_TABLE, OutputClass.class);
        assertEquals(2, output.size());
        assertEquals("value_1 [altered]", output.get(0).getColumn());
        assertEquals("value_2 [altered]", output.get(1).getColumn());
    }

    /**
     * В случае если строка трансформируется в несколько строк в данные таски
     * записывается что была обработана только одна строка
     */
    @Test
    public void testWhenRowTransformsIntoMultipleRowsProccessedRowCounterShowsSingleRow() throws Exception {
        ytClient.write(INPUT_TABLE, InputClass.class, List.of(
                new InputClass("value_1")
        ));

        DefaultTestTaskImpl task = new DefaultTestTaskImpl(ytClient) {

            @Nonnull
            @Override
            protected List<OutputClass> transform(InputClass row, Void context, TestTaskData data) {
                return IntStream.range(0, 3)
                        .mapToObj(i -> new OutputClass(row.getColumn() + " [" + i + "]"))
                        .collect(Collectors.toList());
            }
        };

        ControlStub control = new ControlStub();
        ExecutionResult result = task.run(null, null, control);

        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETING, result.getNextStatus());

        TestTaskData data = control.getSaved();
        assertEquals(1, data.getTotalRows());
        assertEquals(1, data.getRowsProcessed());
    }

    @Test
    public void testWhenRowTransformsIntoMutipleRowsAllRowsWillFlushAfterInterruption() throws Exception {
        ytClient.write(INPUT_TABLE, InputClass.class, List.of(
                new InputClass("value_1"),
                new InputClass("value_2")
        ));

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger processedCount = new AtomicInteger(0);

        DefaultTestTaskImpl task = new DefaultTestTaskImpl(ytClient) {

            @Nonnull
            @Override
            protected List<OutputClass> transform(InputClass row, Void context, TestTaskData data) throws Exception {
                if (processedCount.getAndIncrement() == 1) {
                    barrier.await(10, TimeUnit.SECONDS);
                    new CountDownLatch(1).await();
                }

                return IntStream.range(0, 3)
                        .mapToObj(i -> new OutputClass(row.getColumn() + " [" + i + "]"))
                        .collect(Collectors.toList());
            }
        };

        ControlStub control = new ControlStub();
        AtomicReference<ExecutionResult> result = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            try {
                result.set(task.run(null, null, control));
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });

        thread.start();

        barrier.await(10, TimeUnit.SECONDS);

        thread.interrupt();
        thread.join();

        assertNotNull(result);
        assertEquals(TaskStatus.WAITING, result.get().getNextStatus());

        TestTaskData data = control.getSaved();
        assertEquals(2, data.getTotalRows());
        assertEquals(1, data.getRowsProcessed());

        List<OutputClass> output = ytClient.read(OUTPUT_TABLE, OutputClass.class);
        assertEquals(3, output.size());
        assertEquals("value_1 [0]", output.get(0).getColumn());
        assertEquals("value_1 [1]", output.get(1).getColumn());
        assertEquals("value_1 [2]", output.get(2).getColumn());
    }

    private One2OneTestTaskImpl createTask(TrashFunction<InputClass, OutputClass> mapper) {
        return new One2OneTestTaskImpl(ytClient, mapper);
    }
}
