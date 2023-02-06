package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobExecutorSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobRunningEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.SubscribersSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.SomeJobsAreRunningException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.DummyFullJobIdFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PrepareLaunchParameters;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.independent.IndependentPipe;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.simple.SimplePipe;

import java.util.stream.IntStream;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 09.03.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PipeStateServiceTest {
    private static final String USERNAME = "user42";

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Autowired
    private PipeStateService sut;

    @Test
    public void saveAndRead() {
        String launchId = sut.activateLaunch(
            PrepareLaunchParameters.builder()
                .withPipeId(SimplePipe.PIPE_ID)
                .withTriggeredBy(USERNAME)
                .withProjectId("prj")
                .build()
        ).getId().toString();

        sut.recalc(launchId, new JobRunningEvent(SimplePipe.JOB_ID, 1, DummyFullJobIdFactory.create()));
        sut.recalc(launchId, new JobExecutorSucceededEvent(SimplePipe.JOB_ID, 1));
        sut.recalc(launchId, new SubscribersSucceededEvent(SimplePipe.JOB_ID, 1));
        sut.recalc(launchId, new JobSucceededEvent(SimplePipe.JOB_ID, 1));

        PipeLaunch launch = pipeLaunchDao.getById(launchId);

        Assert.assertEquals(
            5,
            launch.getJobs().get(SimplePipe.JOB_ID).getLaunches().get(0).getStatusHistory().size()
        );
    }

    @Test(expected = SomeJobsAreRunningException.class)
    public void disableRunningPipeline() {
        String launchId = sut.activateLaunch(
            PrepareLaunchParameters.builder()
                .withPipeId(SimplePipe.PIPE_ID)
                .withTriggeredBy(USERNAME)
                .withProjectId("prj")
                .build()
        ).getId().toString();
        sut.disableLaunch(launchId, true);
    }

    @Ignore // Локально проходит, на ТС нет
    @Test
    public void concurrentModification() {
        String launchId = sut.activateLaunch(
            PrepareLaunchParameters.builder()
                .withPipeId(IndependentPipe.PIPE_NAME)
                .withTriggeredBy(USERNAME)
                .withProjectId("prj")
                .build()
        ).getId().toString();

        IntStream.range(0, IndependentPipe.JOB_COUNT)
            .parallel()
            .forEach(i -> {
                String jobId = IndependentPipe.JOB_PREFIX + i;

                System.out.println("job running: " + jobId);
                sut.recalc(launchId, new JobRunningEvent(jobId, 1, DummyFullJobIdFactory.create()));
                sut.recalc(launchId, new JobExecutorSucceededEvent(jobId, 1));
                sut.recalc(launchId, new SubscribersSucceededEvent(jobId, 1));
                System.out.println("job succeeded: " + jobId);
                sut.recalc(launchId, new JobSucceededEvent(jobId, 1));
            });

        PipeLaunch launch = pipeLaunchDao.getById(launchId);

        Assert.assertEquals(200, launch.getRevision());
        Assert.assertTrue(launch.getJobs().values().stream().allMatch(this::isSuccessful));
    }

    private boolean isSuccessful(JobState state) {
        return state.getLastLaunch()
            .getLastStatusChange()
            .getType()
            .equals(StatusChangeType.SUCCESSFUL);
    }
}