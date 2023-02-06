package ru.yandex.market.tsum.pipe.engine.runtime.state.calculator;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRefContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.events.ForceSuccessTriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobExecutorFailedEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobExecutorSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobFailedEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobRunningEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.SubscribersSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.TriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.DummyFullJobIdFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobScheduler;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.simple.SimplePipe;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 07.03.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SimplePipeStateRecalcTest extends PipeStateCalculatorTest {
    private static final String JOB_ID = "dummy";

    @Test
    public void triggerJob() throws Exception {
        recalc(null);
        recalc(new TriggerEvent(SimplePipe.JOB_ID, USERNAME, false));

        String triggeredJobId = getTriggeredJobs().get(0).getJobLaunchId().getJobId();
        Assert.assertEquals(SimplePipe.JOB_ID, triggeredJobId);
        JobState jobState = getPipeLaunch().getJobs().get(SimplePipe.JOB_ID);

        Assert.assertEquals(
            StatusChangeType.QUEUED,
            jobState.getLastStatusChangeType()
        );
    }

    @Test
    public void jobRunningEvent() {
        recalc(null);
        recalc(new TriggerEvent(SimplePipe.JOB_ID, USERNAME, false));

        TestJobScheduler jobScheduler = new TestJobScheduler();
        recalc(new JobRunningEvent(SimplePipe.JOB_ID, 1, DummyFullJobIdFactory.create()));

        Assert.assertTrue(jobScheduler.getTriggeredJobs().isEmpty());

        JobState jobState = getPipeLaunch().getJobs().get(SimplePipe.JOB_ID);
        Assert.assertEquals(
            StatusChangeType.RUNNING,
            jobState.getLastStatusChangeType()
        );
    }

    @Test
    public void jobSucceededEvent() {
        recalc(null);
        recalc(new TriggerEvent(SimplePipe.JOB_ID, USERNAME, false));
        recalc(new JobRunningEvent(SimplePipe.JOB_ID, 1, DummyFullJobIdFactory.create()));

        TestJobScheduler jobScheduler = new TestJobScheduler();
        recalc(new JobExecutorSucceededEvent(SimplePipe.JOB_ID, 1));
        recalc(new SubscribersSucceededEvent(SimplePipe.JOB_ID, 1));
        recalc(new JobSucceededEvent(SimplePipe.JOB_ID, 1));

        Assert.assertTrue(jobScheduler.getTriggeredJobs().isEmpty());

        JobState jobState = getPipeLaunch().getJobs().get(SimplePipe.JOB_ID);
        Assert.assertEquals(
            StatusChangeType.SUCCESSFUL,
            jobState.getLastStatusChangeType()
        );
    }

    @Test
    public void jobForceSucceededEvent() {
        recalc(null);
        recalc(new TriggerEvent(SimplePipe.JOB_ID, USERNAME, false));
        recalc(new JobRunningEvent(SimplePipe.JOB_ID, 1, DummyFullJobIdFactory.create()));

        TestJobScheduler jobScheduler = new TestJobScheduler();
        recalc(new JobExecutorFailedEvent(SimplePipe.JOB_ID, 1, ResourceRefContainer.empty(), new RuntimeException()));
        recalc(new SubscribersSucceededEvent(SimplePipe.JOB_ID, 1));
        recalc(new JobFailedEvent(SimplePipe.JOB_ID, 1));
        Assert.assertEquals(
            StatusChangeType.FAILED,
            getPipeLaunch().getJobs().get(SimplePipe.JOB_ID).getLastStatusChangeType()
        );

        recalc(new ForceSuccessTriggerEvent(SimplePipe.JOB_ID, "jenkl"));

        Assert.assertTrue(jobScheduler.getTriggeredJobs().isEmpty());

        JobState jobState = getPipeLaunch().getJobs().get(SimplePipe.JOB_ID);

        Assert.assertEquals(
            StatusChangeType.FORCED_EXECUTOR_SUCCEEDED,
            jobState.getLastStatusChangeType()
        );

        Assert.assertEquals(jobState.getLastLaunch().getForceSuccessTriggeredBy(), "jenkl");
    }

    @Override
    protected Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        builder.withJob(DummyJob.class)
            .withManualTrigger()
            .withId(JOB_ID);

        return builder.build();
    }
}