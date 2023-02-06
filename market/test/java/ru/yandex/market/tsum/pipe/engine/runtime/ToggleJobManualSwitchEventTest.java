package ru.yandex.market.tsum.pipe.engine.runtime;

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
import ru.yandex.market.tsum.pipe.engine.runtime.events.ToggleJobManualSwitchEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.BeanRegistrar;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.DummyFullJobIdFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobScheduler;
import ru.yandex.market.tsum.pipe.engine.runtime.state.calculator.PipeStateCalculatorTestBase;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikolay Firov
 * @date 20.12.2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ToggleJobManualSwitchEventTest extends PipeStateCalculatorTestBase {
    private static final String FIRST_JOB = "first";
    private static final String SECOND_JOB = "second";

    @Test
    public void blocksExecution() {
        String pipeId = BeanRegistrar.registerNamedBean(pipeline(false), applicationContext);
        String launchId = activateLaunch(pipeId);

        pipeStateService.recalc(launchId, new JobRunningEvent(FIRST_JOB, 1, DummyFullJobIdFactory.create()));
        pipeStateService.recalc(launchId, new JobExecutorSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new SubscribersSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new ToggleJobManualSwitchEvent(SECOND_JOB, USERNAME, Instant.now()));
        pipeStateService.recalc(launchId, new JobSucceededEvent(FIRST_JOB, 1));

        List<TestJobScheduler.TriggeredJob> queuedCommands = new ArrayList<>(testJobScheduler.getTriggeredJobs());
        Assert.assertEquals(1, queuedCommands.size());
    }

    @Test
    public void enablesExecution() {
        String pipeId = BeanRegistrar.registerNamedBean(pipeline(true), applicationContext);
        String launchId = activateLaunch(pipeId);

        pipeStateService.recalc(launchId, new JobRunningEvent(FIRST_JOB, 1, DummyFullJobIdFactory.create()));
        pipeStateService.recalc(launchId, new JobExecutorSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new SubscribersSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new ToggleJobManualSwitchEvent(SECOND_JOB, USERNAME, Instant.now()));
        pipeStateService.recalc(launchId, new JobSucceededEvent(FIRST_JOB, 1));

        List<TestJobScheduler.TriggeredJob> queuedCommands = new ArrayList<>(testJobScheduler.getTriggeredJobs());
        Assert.assertEquals(2, queuedCommands.size());
    }

    static Pipeline pipeline(boolean withManualTrigger) {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(DummyJob.class)
            .withId(FIRST_JOB);

        JobBuilder second = builder.withJob(DummyJob.class)
            .withUpstreams(first)
            .withId(SECOND_JOB);

        if (withManualTrigger)
            second.withManualTrigger();

        return builder.build();
    }

}