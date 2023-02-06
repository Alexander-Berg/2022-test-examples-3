package ru.yandex.market.tsum.pipe.engine.runtime.state.calculator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobExecutorSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobRunningEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.SubscribersSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.DummyFullJobIdFactory;
import ru.yandex.misc.test.Assert;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 23.03.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TwoNodeManualTriggerTest extends PipeStateCalculatorTest {
    private static final String START_JOB = "start";
    private static final String END_JOB = "end";

    @Override
    protected Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder start = builder.withJob(DummyJob.class)
            .withId(START_JOB);

        builder.withJob(DummyJob.class)
            .withId(END_JOB)
            .withUpstreams(start)
            .withManualTrigger();

        return builder.build();
    }

    @Test
    public void manualJobNotTriggeredAutomatically() {
        recalc(null);
        getTriggeredJobs().clear();

        Assert.assertFalse(getPipeLaunch().getJobState(END_JOB).isReadyToRun());

        recalc(new JobRunningEvent(START_JOB, 1, DummyFullJobIdFactory.create()));
        recalc(new JobExecutorSucceededEvent(START_JOB, 1));
        recalc(new SubscribersSucceededEvent(START_JOB, 1));
        recalc(new JobSucceededEvent(START_JOB, 1));

        Assert.assertEmpty(getTriggeredJobs());
        Assert.assertTrue(getPipeLaunch().getJobState(END_JOB).isReadyToRun());
    }

}
