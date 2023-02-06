package ru.yandex.market.olap2.load;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.SneakyThrows;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.load.tasks.ClickhouseLoadTask;
import ru.yandex.market.olap2.load.tasks.Task;
import ru.yandex.market.olap2.load.tasks.TaskPriority;
import ru.yandex.market.olap2.model.TaskFinishedDuringExecutionException;
import ru.yandex.market.olap2.util.NamedLock;
import ru.yandex.market.olap2.ytreflect.YtTableService;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LoadTaskExecutorTest {
    private static final int SLEEP_MS = 200;
    private static final int WAIT_MS = SLEEP_MS * 4;
    private static final String TABLE = "sometable_1";
    private final int highPriorityLoadPoolSize=15;
    private final int defaultPriorityLoadPoolSize=10;
    private final int lowPriorityLoadPoolSize=7;
    private final MetadataDao metadataDao = Mockito.mock(MetadataDao.class);
    private final YtTableService ytTableService = Mockito.mock(YtTableService.class);
    private final LoadTaskExecutor executor = new LoadTaskExecutor(metadataDao,
            highPriorityLoadPoolSize,
            defaultPriorityLoadPoolSize,
            lowPriorityLoadPoolSize);


    @Test
    public void choosePool() throws Exception {
        Mockito.when(ytTableService.getTableSize(Mockito.any())).thenReturn(100*1024*1024*1024L);

        ExecutorService e1 = executor.choosePool(new TestVerticaLoadTask("e1", "//some/yt/test/path"));
        assertThat(e1, is(executor.verticaLowPriorityExecutor));

        Mockito.when(ytTableService.getTableSize(Mockito.any())).thenReturn(1L);
        ExecutorService e2 = executor.choosePool(new TestVerticaLoadTask("e2", "//some/yt/test/path"));
        assertThat(e2, is(executor.verticaLowPriorityExecutor));

        ThreadPoolExecutor execMock = Mockito.mock(ThreadPoolExecutor.class);
        Mockito.when(execMock.getActiveCount()).thenReturn(lowPriorityLoadPoolSize);
        Field execF = executor.getClass().getDeclaredField("verticaLowPriorityExecutor");
        execF.setAccessible(true);
        //execF.setInt(execF, execF.getModifiers() & ~Modifier.FINAL);
        execF.set(executor, execMock);
        ExecutorService e3 = executor.choosePool(new TestVerticaLoadTask("e2", "//some/yt/test/path"));
        assertThat(e3, is(executor.verticaLowPriorityExecutor));
    }

    @Test
    public void mustSilentlySkipTasksIfPoolFull() throws InterruptedException {
        int mustBeExecutedCount = lowPriorityLoadPoolSize;
        int mustBeSkippedCount = 42;
        AtomicInteger actuallyExecuted = new AtomicInteger(0);

        for (int i = 0; i < mustBeExecutedCount + mustBeSkippedCount; i++) {
            executor.putTask(sleepingTask(i, actuallyExecuted::incrementAndGet));
        }
        waitExec();

        assertThat(actuallyExecuted.get(), is(mustBeExecutedCount));
    }

    @Test
    public void mustSkipIfPreviousRunning() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);

        executor.putTask(sleepingTask(1, actuallyExecuted::incrementAndGet));
        executor.putTask(sleepingTask(1, actuallyExecuted::incrementAndGet));
        waitExec();

        assertThat(actuallyExecuted.get(), is(1));
    }

    @Test
    public void mustRunIfPreviousRunningTooLong() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        executor.tblPartToStartTime.put(TABLE + "@" + 1,
                System.currentTimeMillis() - LoadTaskExecutor.MAX_EXEC_TIME_MS - 1);
        executor.putTask(sleepingTask(1, actuallyExecuted::incrementAndGet));
        waitExec();

        assertThat(actuallyExecuted.get(), is(1));
    }

    @Test
    public void mustStopIfFinished() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        AtomicInteger methodsMustNotRun = new AtomicInteger(0);
        Task t = sleepingTask(1, actuallyExecuted::incrementAndGet);
        Mockito.doThrow(new TaskFinishedDuringExecutionException())
                .when(metadataDao)
                .checkIsFinished(Mockito.any());

        Mockito.doAnswer(voidAnswer(methodsMustNotRun::incrementAndGet))
                .when(metadataDao).updateEventLoadedSuccessfully(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doAnswer(voidAnswer(methodsMustNotRun::incrementAndGet))
                .when(metadataDao).updateTaskFailure(Mockito.any(), Mockito.any());

        Mockito.doAnswer(voidAnswer(methodsMustNotRun::incrementAndGet))
                .when(metadataDao).rejectStepEvent(Mockito.any(), Mockito.any(Exception.class));

        executor.putTask(t);
        waitExec();

        assertThat(actuallyExecuted.get(), is(0));
        assertThat(methodsMustNotRun.get(), is(0));
    }

    private Answer<Void> voidAnswer(NamedLock.ThrowingRunnable callback) {
        return invocation -> {
            callback.run();
            return null;
        };
    }

    @SneakyThrows
    private void waitExec() {
        executor.chLowPriorityExecutor.shutdown();
        executor.chLowPriorityExecutor.awaitTermination(WAIT_MS, TimeUnit.MILLISECONDS);
    }

    @SneakyThrows
    private ClickhouseLoadTask sleepingTask(int partition, Runnable callback) {
        ClickhouseLoadTask t = Mockito.mock(ClickhouseLoadTask.class);
        Mockito.when(t.getTable()).thenReturn(TABLE);
        Mockito.when(t.getPartition()).thenReturn(partition);
        Mockito.when(t.getPriority()).thenReturn(TaskPriority.LOW);

        Mockito.doAnswer(voidAnswer(() -> {
            Thread.sleep(SLEEP_MS);
            callback.run();
        })).when(t).copyTable();

        return t;
    }

}
