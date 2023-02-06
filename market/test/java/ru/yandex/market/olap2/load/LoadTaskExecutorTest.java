package ru.yandex.market.olap2.load;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.graphite.Graphite;
import ru.yandex.market.olap2.leader.LeaderElector;
import ru.yandex.market.olap2.leader.Shutdowner;
import ru.yandex.market.olap2.load.tasks.ClickhouseLoadTask;
import ru.yandex.market.olap2.load.tasks.Task;
import ru.yandex.market.olap2.load.tasks.TaskPriority;
import ru.yandex.market.olap2.model.TaskFinishedDuringExecutionException;
import ru.yandex.market.olap2.services.JugglerEventsSender;
import ru.yandex.market.olap2.util.NamedLock;
import ru.yandex.market.olap2.yt.YtClusterLiveliness;
import ru.yandex.market.olap2.yt.YtTableService;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

public class LoadTaskExecutorTest {
    private static final int SLEEP_MS = 200;
    private static final int WAIT_MS = SLEEP_MS * 4;
    private static final String TABLE = "sometable_1";
    private final int highPriorityLoadPoolSize = 15;
    private final int defaultPriorityLoadPoolSize = 10;
    private final int lowPriorityLoadPoolSize = 7;
    private final String lowPriorityStepEventId = "low_priority_test_step_event_id";
    private final String highPriorityStepEventId = "high_priority_test_step_event_id";
    private final MetadataDao metadataDao = Mockito.mock(MetadataDao.class);
    private final YtTableService ytTableService = Mockito.mock(YtTableService.class);
    private final Shutdowner shutdowner = Mockito.mock(Shutdowner.class);
    private final LeaderElector leaderElector = Mockito.mock(LeaderElector.class);
    private final JugglerEventsSender jugglerNotifyer = Mockito.mock(JugglerEventsSender.class);
    private final YtClusterLiveliness clusterLiveliness = Mockito.mock(YtClusterLiveliness.class);
    @Autowired
    private Graphite graphite;

    private ThreadPoolExecutor highExecutor, defaultExecutor, lowExecutor;
    private LoadTaskExecutor executor;

    @Before
    public void prepare() {
        highExecutor = fixedThreadPoolWithoutQueue(highPriorityLoadPoolSize);
        defaultExecutor = fixedThreadPoolWithoutQueue(defaultPriorityLoadPoolSize);
        lowExecutor = fixedThreadPoolWithoutQueue(lowPriorityLoadPoolSize);
        executor = new LoadTaskExecutor(
                metadataDao, shutdowner, highExecutor, defaultExecutor, lowExecutor, jugglerNotifyer, leaderElector,
                clusterLiveliness, graphite
        );
    }

    @Test
    public void choosePool() throws Exception {
        Mockito.when(ytTableService.getTableSize(Mockito.any(), Mockito.any())).thenReturn(100 * 1024 * 1024 * 1024L);

        ExecutorService e1 = executor.choosePool(
                new TestClickhouseLoadTask("e1", "//some/yt/test/path").getPriority()
        );
        assertThat(e1, is(executor.chLowPriorityExecutor));

        Mockito.when(ytTableService.getTableSize(Mockito.any(), Mockito.any())).thenReturn(1L);
        ExecutorService e2 = executor.choosePool(
                new TestClickhouseLoadTask("e2", "//some/yt/test/path").getPriority()
        );
        assertThat(e2, is(executor.chLowPriorityExecutor));

        ThreadPoolExecutor execMock = Mockito.mock(ThreadPoolExecutor.class);
        Mockito.when(execMock.getActiveCount()).thenReturn(lowPriorityLoadPoolSize);
        Field execF = executor.getClass().getDeclaredField("chLowPriorityExecutor");
        execF.setAccessible(true);
        //execF.setInt(execF, execF.getModifiers() & ~Modifier.FINAL);
        execF.set(executor, execMock);
        ExecutorService e3 = executor.choosePool(
                new TestClickhouseLoadTask("e2", "//some/yt/test/path").getPriority()
        );
        assertThat(e3, is(executor.chLowPriorityExecutor));
    }

    @Test
    public void mustSilentlySkipTasksIfPoolFull() throws InterruptedException {
        int mustBeExecutedCount = lowPriorityLoadPoolSize;
        int mustBeSkippedCount = 42;
        AtomicInteger actuallyExecuted = new AtomicInteger(0);

        for (int i = 0; i < mustBeExecutedCount + mustBeSkippedCount; i++) {
            executor.putTask(sleepingTask(lowPriorityStepEventId, i, actuallyExecuted::incrementAndGet, TaskPriority.LOW));
        }
        waitExec();

        assertThat(actuallyExecuted.get(), is(mustBeExecutedCount));
    }

    @Test
    public void mustSkipIfPreviousRunning() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);

        executor.putTask(sleepingTask(lowPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.LOW));
        executor.putTask(sleepingTask(lowPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.LOW));
        waitExec();

        assertThat(actuallyExecuted.get(), is(1));
    }

    @Test
    public void mustSkipAndDelayIfRetried() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        prepareMockDelayable(lowPriorityStepEventId, null, 0, 2, TaskPriority.LOW);
        Task task = sleepingTask(lowPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.LOW);
        executor.putTask(task);
        waitExec();

        assertThat(actuallyExecuted.get(), is(0));
        Mockito.verify(metadataDao, times(1)).updateNotLoadUntil(lowPriorityStepEventId, 10);
    }

    @Test
    public void mustExecAndDelayIfRetriedSubsequently() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        prepareMockDelayable(lowPriorityStepEventId, LocalDateTime.now(), 0, 3, TaskPriority.LOW);

        Task task = sleepingTask(lowPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.LOW);
        executor.putTask(task);
        waitExec();

        assertThat(actuallyExecuted.get(), is(1));
        Mockito.verify(metadataDao, times(1)).updateNotLoadUntil(lowPriorityStepEventId, 20);
    }

    @Test
    public void mustNotSkipAndDelayIfRetriedHighPrio() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        prepareMockDelayable(highPriorityStepEventId, null, 0, 4, TaskPriority.HIGH);

        Task task = sleepingTask(highPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.HIGH);
        executor.putTask(task);
        waitExec(TaskPriority.HIGH);

        assertThat(actuallyExecuted.get(), is(1));
        metadataDao.getEventRetriesCount("");
        Mockito.verify(metadataDao, never()).updateNotLoadUntil(eq(highPriorityStepEventId), Mockito.anyInt());
    }

    @Test
    public void mustSkipAndDelayIfManyLoads() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        prepareMockDelayable(lowPriorityStepEventId, null, 5, 0, TaskPriority.LOW);

        Task task = sleepingTask(lowPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.LOW);
        executor.putTask(task);
        waitExec();

        assertThat(actuallyExecuted.get(), is(0));
        Mockito.verify(metadataDao, times(1)).updateNotLoadUntil(lowPriorityStepEventId, 5);
    }

    @Test
    public void mustNotSkipAndNotDelayIfManyLowPrioLoadsButGetHighPrio() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        prepareMockDelayable(lowPriorityStepEventId, null, 5, 0, TaskPriority.LOW);
        prepareMockDelayable(highPriorityStepEventId, null, 0, 0, TaskPriority.HIGH);

        Task task = sleepingTask(highPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.HIGH);
        executor.putTask(task);
        waitExec(TaskPriority.HIGH);

        assertThat(actuallyExecuted.get(), is(1));
        Mockito.verify(metadataDao, never()).updateNotLoadUntil(eq(highPriorityStepEventId), Mockito.anyInt());
    }

    @Test
    public void mustSkipAndDelayIfEvenMoreLoads() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        prepareMockDelayable(lowPriorityStepEventId, null, 20, 0, TaskPriority.LOW);

        Task task = sleepingTask(lowPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.LOW);
        executor.putTask(task);
        waitExec();

        assertThat(actuallyExecuted.get(), is(0));
        Mockito.verify(metadataDao, times(1)).updateNotLoadUntil(lowPriorityStepEventId, 60);
    }

    @Test
    public void mustSkipAndDelayIfManyLoadsHighPrio() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        prepareMockDelayable(highPriorityStepEventId, null, 5, 0, TaskPriority.HIGH);

        Task task = sleepingTask(highPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.HIGH);

        executor.putTask(task);
        waitExec(TaskPriority.HIGH);

        assertThat(actuallyExecuted.get(), is(0));
        Mockito.verify(metadataDao, times(1)).updateNotLoadUntil(highPriorityStepEventId, 5);
    }

    @Test
    public void mustDelayIfManyLoadsAndRetries() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        prepareMockDelayable(lowPriorityStepEventId, LocalDateTime.now(), 5, 3, TaskPriority.LOW);

        Task task = sleepingTask(lowPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.LOW);

        executor.putTask(task);
        waitExec();

        assertThat(actuallyExecuted.get(), is(1));
        Mockito.verify(metadataDao, times(1)).updateNotLoadUntil(lowPriorityStepEventId, 20);
    }

    @Test
    public void mustDelayMoreIfManyLoadsAndRetries() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        prepareMockDelayable(lowPriorityStepEventId, LocalDateTime.now(), 20, 2, TaskPriority.LOW);

        Task task = sleepingTask(lowPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.LOW);

        executor.putTask(task);
        waitExec();

        assertThat(actuallyExecuted.get(), is(1));
        Mockito.verify(metadataDao, times(1)).updateNotLoadUntil(lowPriorityStepEventId, 10);
    }
//  Вариант с prepareMockDelayable(null, >=5, >0); не проверяю, т.к. он невозможен
//  после первого же запуска >5й за день таблички она заимеет себе not_load_until и он будет только увеличиваться на ретраях

    @Test
    public void mustRunIfPreviousRunningTooLong() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        executor.tblPartToStartTime.put(TABLE + "@" + 1,
                System.currentTimeMillis() - LoadTaskExecutor.MAX_EXEC_TIME_MS - 1);
        executor.putTask(sleepingTask(lowPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.LOW));
        waitExec();

        assertThat(actuallyExecuted.get(), is(1));
    }

    @Test
    public void mustStopIfFinished() {
        AtomicInteger actuallyExecuted = new AtomicInteger(0);
        AtomicInteger methodsMustNotRun = new AtomicInteger(0);
        Task t = sleepingTask(lowPriorityStepEventId, 1, actuallyExecuted::incrementAndGet, TaskPriority.LOW);
        Mockito.doThrow(new TaskFinishedDuringExecutionException())
                .when(metadataDao)
                .checkIsFinished(Mockito.any());

        Mockito.doAnswer(voidAnswer(methodsMustNotRun::incrementAndGet))
                .when(metadataDao).updateEventLoadedSuccessfully(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doAnswer(voidAnswer(methodsMustNotRun::incrementAndGet))
                .when(metadataDao).updateTaskFailure(Mockito.any(), Mockito.any());

        Mockito.doAnswer(voidAnswer(methodsMustNotRun::incrementAndGet))
                .when(metadataDao).rejectStepEvent(Mockito.any(), Mockito.any());

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
        waitExec(TaskPriority.LOW);
    }

    @SneakyThrows
    private void waitExec(TaskPriority priority) {
        ThreadPoolExecutor threadPoolExecutor = executor.chLowPriorityExecutor;
        if (priority == TaskPriority.DEFAULT) {
            threadPoolExecutor = executor.chDefaultPriorityExecutor;
        } else if (priority == TaskPriority.HIGH) {
            threadPoolExecutor = executor.chHighPriorityExecutor;
        }
        threadPoolExecutor.shutdown();
        threadPoolExecutor.awaitTermination(WAIT_MS, TimeUnit.MILLISECONDS);
    }

    @SneakyThrows
    private ClickhouseLoadTask sleepingTask(String stepEventId, int partition, Runnable callback, TaskPriority priority) {
        ClickhouseLoadTask t = Mockito.mock(ClickhouseLoadTask.class);
        Mockito.when(t.getTable()).thenReturn(TABLE);
        Mockito.when(t.getPartition()).thenReturn(partition);
        Mockito.when(t.getPriority()).thenReturn(priority);
        Mockito.when(t.getStepEventId()).thenReturn(stepEventId);

        Mockito.doAnswer(voidAnswer(() -> {
            Thread.sleep(SLEEP_MS);
            callback.run();
        })).when(t).copyTable();

        return t;
    }

    private static ThreadPoolExecutor fixedThreadPoolWithoutQueue(int maxConcurentTasks) {
        return new ThreadPoolExecutor(maxConcurentTasks, maxConcurentTasks, 0, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.DiscardPolicy());
    }


    private void prepareMockDelayable(String stepEventId, LocalDateTime notLoadUntil, int todaysLoads,
                                      int retryCount, TaskPriority taskPriority) {
        Mockito.when(metadataDao.getNotLoadUntil(eq(stepEventId))).thenReturn(notLoadUntil);
        Mockito.when(metadataDao.getTodaysLoadsCountByTableAndPriority(Mockito.any(), eq(taskPriority))).thenReturn(todaysLoads);
        Mockito.when(metadataDao.getEventRetriesCount(eq(stepEventId))).thenReturn(retryCount);
    }

}
