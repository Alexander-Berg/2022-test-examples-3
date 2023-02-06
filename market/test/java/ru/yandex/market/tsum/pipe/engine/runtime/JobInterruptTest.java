package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.curator.CuratorFactory;
import ru.yandex.market.tsum.pipe.engine.curator.CuratorValueObservable;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.job.InterruptMethod;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.events.ExecutorInterruptingEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.BadInterruptJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.SleepyJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.StuckJob;

import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class, JobInterruptTest.Config.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JobInterruptTest {
    public static final String JOB_ID = "ID";

    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private CuratorFactory factory;

    private ArgumentCaptor<Consumer<byte[]>> captor;

    @Autowired
    private Semaphore semaphore;

    @Configuration
    public static class Config {
        @Bean
        public Semaphore semaphore() {
            return new Semaphore(0, true);
        }
    }

    @Before
    public void setup() {
        CuratorValueObservable nodeObservable = Mockito.mock(CuratorValueObservable.class);
        when(factory.createValueObservable(Mockito.anyString())).thenReturn(nodeObservable);

        //noinspection unchecked
        captor = ArgumentCaptor.forClass(Consumer.class);
        doNothing().when(nodeObservable).observe(captor.capture());
    }

    @Test
    public void terminatesJob() throws Exception {
        String pipeLaunchId = pipeTester.activateLaunch(
            createPipeline(SleepyJob.class),
            Collections.emptyList()
        );

        Thread thread = pipeTester.runScheduledJobsToCompletionAsync();
        tryAcquire(thread);

        pipeTester.recalcPipeLaunch(pipeLaunchId, new ExecutorInterruptingEvent(JOB_ID, 1, "me"));

        Consumer<byte[]> terminateHandler = captor.getValue();
        terminateHandler.accept(getInterruptData());
        thread.join();

        JobLaunch job = pipeTester.getJobLastLaunch(pipeLaunchId, JOB_ID);
        Assert.assertEquals(StatusChangeType.INTERRUPTED, job.getLastStatusChange().getType());
        Assert.assertEquals("Interrupting", job.getStatusText());
    }

    @Test
    public void killsJob() throws Exception {
        String pipeLaunchId = pipeTester.activateLaunch(
            createPipeline(SleepyJob.class),
            Collections.emptyList()
        );

        Thread thread = pipeTester.runScheduledJobsToCompletionAsync();
        tryAcquire(thread);

        pipeTester.recalcPipeLaunch(pipeLaunchId, new ExecutorInterruptingEvent(JOB_ID, 1, "me"));

        captor.getValue().accept(getKillData());

        thread.join();

        JobLaunch job = pipeTester.getJobLastLaunch(pipeLaunchId, JOB_ID);
        Assert.assertEquals(StatusChangeType.KILLED, job.getLastStatusChange().getType());
        Assert.assertEquals("Sleeping", job.getStatusText());
    }

    @Test
    public void jobWithBadInterruptContinues() throws Exception {
        String pipeLaunchId = pipeTester.activateLaunch(
            createPipeline(BadInterruptJob.class),
            Collections.emptyList()
        );

        Thread thread = pipeTester.runScheduledJobsToCompletionAsync();
        tryAcquire(thread);

        pipeTester.recalcPipeLaunch(pipeLaunchId, new ExecutorInterruptingEvent(JOB_ID, 1, "me"));

        captor.getValue().accept(getInterruptData());

        thread.join();

        JobLaunch job = pipeTester.getJobLastLaunch(pipeLaunchId, JOB_ID);
        Assert.assertEquals(StatusChangeType.FAILED, job.getLastStatusChange().getType());
        Assert.assertEquals("Woke up", job.getStatusText());
    }

    @Test
    public void killsStuckJob() throws Exception {
        String pipeLaunchId = pipeTester.activateLaunch(
            createPipeline(StuckJob.class),
            Collections.emptyList()
        );

        Thread thread = pipeTester.runScheduledJobsToCompletionAsync();
        tryAcquire(thread);

        pipeTester.recalcPipeLaunch(pipeLaunchId, new ExecutorInterruptingEvent(JOB_ID, 1, "me"));

        Consumer<byte[]> terminateHandler = captor.getValue();

        terminateHandler.accept(getInterruptData());
        tryAcquire(thread);

        terminateHandler.accept(getKillData());

        thread.join();

        JobLaunch job = pipeTester.getJobLastLaunch(pipeLaunchId, JOB_ID);
        Assert.assertEquals(StatusChangeType.INTERRUPTED, job.getLastStatusChange().getType());
        Assert.assertEquals("Interrupting", job.getStatusText());
    }

    private byte[] getKillData() {
        return InterruptMethod.KILL.name().getBytes();
    }

    private byte[] getInterruptData() {
        return InterruptMethod.INTERRUPT.name().getBytes();
    }

    private Pipeline createPipeline(Class<? extends JobExecutor> executorClass) {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(executorClass).withId(JOB_ID);

        return builder.build();
    }

    private void tryAcquire(Thread thread) throws InterruptedException {
        boolean acquired = semaphore.tryAcquire(20, TimeUnit.SECONDS);
        if (!acquired) {
            if (thread.isAlive()) {
                throw new InterruptedException("acquire timeout");
            } else {
                throw new RuntimeException("Target thread exited before releasing semaphore");
            }
        }
    }
}
