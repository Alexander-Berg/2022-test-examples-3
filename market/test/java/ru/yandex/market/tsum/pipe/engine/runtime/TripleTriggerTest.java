package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.events.TriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.BeanRegistrar;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.DummyFullJobIdFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobScheduler;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PrepareLaunchParameters;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;

import java.util.Queue;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 09.06.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TripleTriggerTest {
    private static final String USERNAME = "user42";
    public static final String JOB_ID = "job";

    @Autowired
    private PipeStateService pipeStateService;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private TestJobScheduler testJobScheduler;

    @Autowired
    private GenericApplicationContext applicationContext;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Test
    public void test() throws Exception {
        String pipeId = BeanRegistrar.registerNamedBean(getPipeline(), applicationContext);

        String pipeLaunchId = pipeStateService.activateLaunch(
            PrepareLaunchParameters.builder()
                .withPipeId(pipeId)
                .withTriggeredBy(USERNAME)
                .withProjectId("prj")
                .build()
        ).getId().toString();

        pipeStateService.recalc(pipeLaunchId, null);
        pipeStateService.recalc(pipeLaunchId, new TriggerEvent(JOB_ID, USERNAME, false));
        pipeStateService.recalc(pipeLaunchId, new TriggerEvent(JOB_ID, USERNAME, false));

        Queue<TestJobScheduler.TriggeredJob> triggerCommands = testJobScheduler.getTriggeredJobs();
        while (!triggerCommands.isEmpty()) {
            TestJobScheduler.TriggeredJob triggeredJob = triggerCommands.poll();
            System.out.println(triggeredJob);
            jobLauncher.launchJob(triggeredJob.getJobLaunchId(), DummyFullJobIdFactory.create());
        }

        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        JobState jobState = pipeLaunch.getJobState(JOB_ID);
        Assert.assertEquals(1, jobState.getLaunches().size());

        Assert.assertEquals(
            StatusChangeType.SUCCESSFUL,
            jobState.getLaunches().get(0).getLastStatusChange().getType()
        );
    }

    private Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(DummyJob.class).withId(JOB_ID);
        return builder.build();
    }
}
