package ru.yandex.market.tsum.pipe.engine.runtime.state.calculator;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.subscriber.PipeSubscriber;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.runtime.FullJobLaunchId;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRefContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.events.ForceSuccessTriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.BeanRegistrar;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchParameters;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunchRefImpl;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.Resource451;

import java.util.Collections;
import java.util.UUID;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 05.12.17
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JobForceSucceededTest {
    private static final String FIRST_JOB_ID = "first";
    private static final String SECOND_JOB_ID = "second";

    @Autowired
    PipeTester pipeTester;
    @Autowired
    private PipeLaunchFactory pipeLaunchFactory;
    @Autowired
    private PipeStateService pipeStateService;
    @Autowired
    private GenericApplicationContext applicationContext;

    private String pipeId;

    @Test
    public void forceSuccessJobWithoutProducedResources() throws Exception {
        preparePipeLaunch(pipelineWithFailedJob());

        String pipeLunchId = pipeTester.runPipeToCompletion(pipeId, Collections.emptyList());
        JobLaunch firstJobLastLaunch = pipeTester.getJobLastLaunch(pipeLunchId, FIRST_JOB_ID);
        Assert.assertEquals(firstJobLastLaunch.getLastStatusChange().getType(), StatusChangeType.FAILED);

        pipeStateService.recalc(pipeLunchId, new ForceSuccessTriggerEvent(FIRST_JOB_ID, "jenkl"));
        pipeTester.runScheduledJobsToCompletion();

        firstJobLastLaunch = pipeTester.getJobLastLaunch(pipeLunchId, FIRST_JOB_ID);
        Assert.assertEquals(firstJobLastLaunch.getLastStatusChange().getType(), StatusChangeType.SUCCESSFUL);

        JobLaunch secondJobLastLaunch = pipeTester.getJobLastLaunch(pipeLunchId, SECOND_JOB_ID);
        Assert.assertEquals(secondJobLastLaunch.getLastStatusChange().getType(), StatusChangeType.SUCCESSFUL);

    }

    @Test
    public void forceSuccessJobWithFailedSubscriber() throws Exception {
        preparePipeLaunch(pipelineWithFailedSubscribers());

        String pipeLunchId = pipeTester.runPipeToCompletion(pipeId, Collections.emptyList());
        JobLaunch firstJobLastLaunch = pipeTester.getJobLastLaunch(pipeLunchId, FIRST_JOB_ID);
        Assert.assertEquals(firstJobLastLaunch.getLastStatusChange().getType(), StatusChangeType.SUBSCRIBERS_FAILED);

        pipeStateService.recalc(pipeLunchId, new ForceSuccessTriggerEvent(FIRST_JOB_ID, "jenkl"));
        pipeTester.runScheduledJobsToCompletion();

        firstJobLastLaunch = pipeTester.getJobLastLaunch(pipeLunchId, FIRST_JOB_ID);
        Assert.assertEquals(firstJobLastLaunch.getLastStatusChange().getType(), StatusChangeType.SUBSCRIBERS_FAILED);

        Assert.assertNull(pipeTester.getJobLastLaunch(pipeLunchId, SECOND_JOB_ID));

    }

    private void preparePipeLaunch(Pipeline pipeline) {
        pipeId = BeanRegistrar.registerNamedBean(pipeline, applicationContext);
        PipeLaunch pipeLaunch = pipeLaunchFactory.create(
            PipeLaunchParameters.builder()
                .withLaunchRef(PipeLaunchRefImpl.create(pipeId))
                .withManualResources(ResourceRefContainer.empty())
                .withTriggeredBy("user42")
                .withProjectId("prj")
                .build()
        );
    }

    private Pipeline pipelineWithFailedJob() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(FailedJob.class)
            .withId(FIRST_JOB_ID);

        builder.withJob(DummyJob.class)
            .withId(SECOND_JOB_ID)
            .withUpstreams(first);

        return builder.build();
    }

    private Pipeline pipelineWithProducesResourceFailedJob() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(ProducesResourceFailedJob.class)
            .withId(FIRST_JOB_ID);

        builder.withJob(DummyJob.class)
            .withId(SECOND_JOB_ID)
            .withUpstreams(first);

        return builder.build();
    }

    private Pipeline pipelineWithFailedSubscribers() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(FailedJob.class)
            .withId(FIRST_JOB_ID);

        builder.withSubscriber(FailedSubscriber.class);

        builder.withJob(DummyJob.class)
            .withId(SECOND_JOB_ID)
            .withUpstreams(first);

        return builder.build();
    }

    private static class FailedJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("c6489565-bad8-4743-9fd7-40410fe9fd0d");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            throw new RuntimeException();
        }
    }

    @Produces(single = {Resource451.class})
    private static class ProducesResourceFailedJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("e3f703ae-7589-4786-a656-a47fc1cbce50");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Resource451(451));
            throw new RuntimeException();
        }
    }

    public static class FailedSubscriber implements PipeSubscriber {
        @Override
        public void jobExecutorHasFinished(FullJobLaunchId fullJobLaunchId, PipeLaunch pipeLaunch) {
            throw new RuntimeException();
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("042c62de-983f-4996-bbae-0f854c9c3437");
        }
    }
}
