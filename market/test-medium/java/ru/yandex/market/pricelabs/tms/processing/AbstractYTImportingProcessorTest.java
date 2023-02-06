package ru.yandex.market.pricelabs.tms.processing;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.model.types.WithUpdatedAt;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.tms.quartz2.model.Executor;

public abstract class AbstractYTImportingProcessorTest<S, T extends WithUpdatedAt>
        extends AbstractSourceTargetProcessorConfiguration<S, T> {

    private AbstractYTImportingProcessor<S, T> processor;
    private Executor job;
    private YtSourceTargetScenarioExecutor<S, T> executor;

    @BeforeEach
    protected void init() {
        var now = getInstant();

        this.processor = getProcessor();
        this.job = getJob();
        this.executor = newExecutor();

        testControls.executeInParallel(
                () -> {
                    executor.removeSourceTables();
                    executor.clearSourceTable();
                },
                () -> executor.clearTargetTable(),
                () -> testControls.cleanupTasksService(),
                () -> testControls.cleanupTableRevisions());
    }

    @Test
    void testEmpty() {
        this.test(List.of(), List.of(), List.of());
    }

    @Test
    void testNewRows() {
        this.test(readSourceList(), List.of(), asUpdated(readTargetList()), getTargetUpdate());
    }

    @Test
    void testUpdateSameRows() {
        this.test(readSourceList(), readTargetList(), asUpdated(readTargetList()), getTargetUpdate());
    }

    @Test
    void testUpdate2() {
        this.test(readSourceList2(), List.of(), asUpdated(readTargetList2()), getTargetUpdate());
    }

    @Test
    void testOverwrite1to2() {
        this.test(readSourceList2(), readTargetList(),
                updateRows(readTargetList(), asUpdated(readTargetList2())), getTargetUpdate());
    }

    @Test
    void testUpdateSameRowsWithTimeout() {
        var rows = asUpdated(readTargetList());

        TimingUtils.addTime(1000);
        this.test(readSourceList(), rows, asUpdated(readTargetList()), getTargetUpdate());
    }

    @Test
    void testOverwrite1to2WithTimeout() {
        var expect = asUpdated(readTargetList());
        this.test(readSourceList(), List.of(), expect, getTargetUpdate());

        TimingUtils.addTime(1000);
        executor.insertSource(readSourceList2());
        beforeSync();
        processor.sync();
        executor.verify(updateRows(expect, asUpdated(readTargetList2())), getTargetUpdate());
    }

    @Test
    void testOverwrite1To2DifferentDays() {
        var expect = asUpdated(readTargetList());
        this.test(readSourceList(), List.of(), expect, getTargetUpdate());

        TimingUtils.addTime(1000);
        executors.setDay("2019-07-02");

        executor.insertSource(readSourceList2());
        beforeSync();
        processor.sync();
        executor.verify(updateRows(expect, asUpdated(readTargetList2())), getTargetUpdate());
    }

    @Test
    void testAlreadyProcessed() {
        this.test(readSourceList(), List.of(), asUpdated(readTargetList()), getTargetUpdate());

        executor.insert(List.of());
        beforeSync();
        processor.sync();

        if (processor.ignoreProcessingState()) {
            executor.verify(asUpdated(readTargetList()));
        } else {
            executor.verify(List.of());
        }
    }

    @Test
    void testRunJob() {
        executor.insert(readSourceList(), List.of());
        doJob();
        var task = testControls.startScheduledTask(getJobType());

        beforeSync();
        testControls.executeTask(task);
        testControls.checkNoScheduledTasks();

        executor.verify(asUpdated(readTargetList()), getTargetUpdate());
    }

    @Test
    void testRunJobAlreadyProcessed() {
        this.test(readSourceList(), List.of(), asUpdated(readTargetList()), getTargetUpdate());

        executor.insert(List.of());
        doJob();
        var task = testControls.startScheduledTask(getJobType());

        beforeSync();
        testControls.executeTask(task);
        testControls.checkNoScheduledTasks();

        if (processor.ignoreProcessingState()) {
            executor.verify(asUpdated(readTargetList()));
        } else {
            executor.verify(List.of());
        }
    }

    //

    protected void beforeSync() {
        //
    }

    protected List<T> updateRows(List<T> oldRows, List<T> newRows) {
        return newRows;
    }

    protected void test(List<S> newRows, List<T> existingRows, List<T> expectRows) {
        beforeSync();
        executor.test(() -> processor.sync(), newRows, existingRows, expectRows);
    }

    protected void test(List<S> newRows, List<T> existingRows, List<T> expectRows, Consumer<T> expectUpdate) {
        beforeSync();
        executor.test(() -> processor.sync(), newRows, existingRows, expectRows, expectUpdate);
    }

    public List<S> readSourceList2() {
        return readSourceList(getSourceCsv2());
    }

    public List<T> readTargetList2() {
        return readTargetList(getTargetCsv2());
    }

    protected List<T> asUpdated(List<T> list) {
        return executor.asUpdated(list);
    }

    protected void doJob() {
        testControls.executeJob(job);
    }

    protected void checkNoScheduledTasks() {
        testControls.checkNoScheduledTasks();
    }

    protected YtSourceTargetScenarioExecutor<S, T> getExecutor() {
        return executor;
    }

    protected abstract AbstractYTImportingProcessor<S, T> getProcessor();

    protected abstract YtSourceTargetScenarioExecutor<S, T> newExecutor();

    protected abstract Executor getJob();

    protected abstract JobType getJobType();

    protected abstract String getSourceCsv2();

    protected abstract String getTargetCsv2();

}
