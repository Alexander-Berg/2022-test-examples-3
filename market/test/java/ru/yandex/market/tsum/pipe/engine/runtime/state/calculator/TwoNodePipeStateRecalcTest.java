package ru.yandex.market.tsum.pipe.engine.runtime.state.calculator;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobExecutorSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobRunningEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.SubscribersSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.TriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.DummyFullJobIdFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 09.03.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TwoNodePipeStateRecalcTest extends PipeStateCalculatorTest {
    private static final String FIRST_JOB_ID = "first";
    private static final String SECOND_JOB_ID = "second";

    @Override
    protected Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(DummyJob.class)
            .withManualTrigger()
            .withId(FIRST_JOB_ID);

        builder.withJob(DummyJob.class)
            .withId(SECOND_JOB_ID)
            .withUpstreams(first);

        return builder.build();
    }

    @Test
    public void triggerFirstJob() throws Exception {
        recalc(null);
        recalc(new TriggerEvent(FIRST_JOB_ID, USERNAME, false));

        Assert.assertEquals(1, getTriggeredJobs().size());
        Assert.assertEquals(FIRST_JOB_ID, getTriggeredJobs().get(0).getJobLaunchId().getJobId());

        JobState firstJobState = getPipeLaunch().getJobs().get(FIRST_JOB_ID);

        Assert.assertEquals(
            StatusChangeType.QUEUED,
            firstJobState.getLastStatusChangeType()
        );
    }

    @Test
    public void secondJobAutoTrigger() throws Exception {
        recalc(null);
        recalc(new TriggerEvent(FIRST_JOB_ID, USERNAME, false));

        getTriggeredJobs().clear();
        recalc(new JobRunningEvent(FIRST_JOB_ID, 1, DummyFullJobIdFactory.create()));
        recalc(new JobExecutorSucceededEvent(FIRST_JOB_ID, 1));
        recalc(new SubscribersSucceededEvent(FIRST_JOB_ID, 1));
        recalc(new JobSucceededEvent(FIRST_JOB_ID, 1));

        Assert.assertTrue(getPipeLaunch().getJobState(FIRST_JOB_ID).isReadyToRun());
        Assert.assertEquals(1, getTriggeredJobs().size());
        Assert.assertEquals(SECOND_JOB_ID, getTriggeredJobs().get(0).getJobLaunchId().getJobId());

        Assert.assertEquals(1, getTriggeredJobs().size());
        Assert.assertEquals(SECOND_JOB_ID, getTriggeredJobs().get(0).getJobLaunchId().getJobId());

        JobState firstJobState = getPipeLaunch().getJobState(SECOND_JOB_ID);

        Assert.assertEquals(
            StatusChangeType.QUEUED,
            firstJobState.getLastStatusChangeType()
        );
    }
}
