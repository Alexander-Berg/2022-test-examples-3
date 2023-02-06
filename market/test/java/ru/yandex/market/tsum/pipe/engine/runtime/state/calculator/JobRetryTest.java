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
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobScheduler;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs.ProduceRes1AndFail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Nikolay Firov
 * @date 14.12.2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JobRetryTest extends PipeStateCalculatorTestBase {
    private static final String FIRST_JOB = "first_job";

    @Test
    public void schedulesRetry() {
        String launchId = pipeTester.activateLaunch(pipeline(), Collections.emptyList());

        pipeTester.raiseJobExecuteEventsChain(launchId, FIRST_JOB);

        List<TestJobScheduler.TriggeredJob> queuedCommands = new ArrayList<>(testJobScheduler.getTriggeredJobs());

        Assert.assertEquals(1, queuedCommands.size());
        Assert.assertEquals(FIRST_JOB, queuedCommands.get(0).getJobLaunchId().getJobId());
    }

    @Test
    public void runsThreeTimes() throws InterruptedException {
        String launchId = pipeTester.runPipeToCompletion(pipeline());

        PipeLaunch pipeLaunch = pipeLaunchDao.getById(launchId);
        JobState jobState = pipeLaunch.getJobState(FIRST_JOB);
        Assert.assertEquals(3, jobState.getLaunches().size());
    }


    static Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        builder.withJob(ProduceRes1AndFail.class)
            .withRetries(2)
            .withId(FIRST_JOB);

        return builder.build();
    }
}