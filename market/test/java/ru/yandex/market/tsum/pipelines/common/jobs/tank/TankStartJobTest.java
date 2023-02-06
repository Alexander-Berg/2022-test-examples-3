package ru.yandex.market.tsum.pipelines.common.jobs.tank;

import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.tankapi.StartJobResponse;
import ru.yandex.market.tsum.clients.tankapi.StatusCode;
import ru.yandex.market.tsum.clients.tankapi.StatusJobResponse;
import ru.yandex.market.tsum.clients.tankapi.StopJobResponse;
import ru.yandex.market.tsum.clients.tankapi.TankApiClient;
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
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class})
public class TankStartJobTest {
    public static final String JOB_ID = "ID";

    @Autowired
    private CuratorFactory factory;

    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Autowired
    private TankApiClient tankApiClient;

    private ArgumentCaptor<Consumer<byte[]>> captor;

    @Before
    public void setUp() throws Exception {
        CuratorValueObservable nodeObservable = Mockito.mock(CuratorValueObservable.class);
        when(factory.createValueObservable(Mockito.anyString())).thenReturn(nodeObservable);

        captor = ArgumentCaptor.forClass(Consumer.class);
        doNothing().when(nodeObservable).observe(captor.capture());
    }

    @Test
    @Ignore // https://st.yandex-team.ru/MARKETINFRA-3610 заигнорен в ожидание правок от автора
    public void shouldInterruptTankStartJob() throws InterruptedException {
        Mockito.when(tankApiClient.start(Mockito.nullable(String.class), Mockito.nullable(String.class)))
            .thenReturn(new StartJobResponse());

        StatusJobResponse value = new StatusJobResponse();
        value.setStatusCode(StatusCode.RUNNING);

        Mockito.when(tankApiClient.status(Mockito.nullable(String.class), Mockito.nullable(String.class)))
            .thenReturn(value);

        StopJobResponse stopJobResponse = new StopJobResponse();
        stopJobResponse.setSuccess(true);

        Mockito.when(tankApiClient.stop(Mockito.nullable(String.class)))
            .thenReturn(stopJobResponse);

        String pipeLaunchId = pipeTester.activateLaunch(createPipeline(TankStartJob.class),
            new TankApiJobConfig()
        );

        Thread thread = pipeTester.runScheduledJobsToCompletionAsync();

        JobState state;
        do {
            state = pipeTester.getPipeLaunch(pipeLaunchId).getJobState(JOB_ID);
            Thread.sleep(10);
        } while (state.getLastLaunch().getLastStatusChange().getType() != StatusChangeType.RUNNING ||
            !state.getLastLaunch().getInterruptAllowed());

        StatusJobResponse cancelled = new StatusJobResponse();
        cancelled.setExitCode(1);
        cancelled.setStatusCode(StatusCode.FINISHED);

        Mockito.when(tankApiClient.status(Mockito.nullable(String.class), Mockito.nullable(String.class)))
            .thenReturn(cancelled);

        pipeTester.recalcPipeLaunch(pipeLaunchId, new ExecutorInterruptingEvent(JOB_ID, 1, "me"));

        Consumer<byte[]> terminateHandler = captor.getValue();
        terminateHandler.accept(getInterruptData());

        thread.join();


        JobLaunch job = pipeTester.getJobLastLaunch(pipeLaunchId, JOB_ID);
        Assert.assertEquals(StatusChangeType.INTERRUPTED, job.getLastStatusChange().getType());
        Assert.assertEquals("Стрельба завершена неудачно", job.getStatusText());
    }

    private Pipeline createPipeline(Class<? extends JobExecutor> executorClass) {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(executorClass).withId(JOB_ID);
        return builder.build();
    }

    private byte[] getInterruptData() {
        return InterruptMethod.INTERRUPT.name().getBytes();
    }


}
