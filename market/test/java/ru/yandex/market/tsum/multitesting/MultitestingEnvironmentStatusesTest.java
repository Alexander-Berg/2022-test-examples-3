package ru.yandex.market.tsum.multitesting;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.config.MultitestingConfiguration;
import ru.yandex.market.tsum.config.ReleaseConfiguration;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestConfig;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 15.11.2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MultitestingConfiguration.class,
    ReleaseConfiguration.class, MultitestingTestConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MultitestingEnvironmentStatusesTest {
    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private MultitestingService sut;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Autowired
    private ProjectsDao projectsDao;

    @Autowired
    private GenericApplicationContext applicationContext;

    @Before
    public void setup() {
        Mockito.when(projectsDao.get(Mockito.anyString())).thenReturn(new ProjectEntity());
    }

    @Test
    public void test() throws InterruptedException {
        MultitestingEnvironment environment = sut.createEnvironment(
            MultitestingTestData.defaultEnvironment()
                .withType(MultitestingEnvironment.Type.USE_EXISTING_PIPELINE)
                .withPipelineId(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID)
                .build()
        );

        sut.launchMultitestingEnvironment(
            sut.getEnvironment(environment.getId()),
            ResourceContainer.EMPTY,
            "someUser"
        );
        assertTrue(
            pipeLaunchDao.getById(sut.getEnvironment(environment.getId()).getLastLaunch().getPipeLaunchId())
                .getJobState("cleanup").isDisabled()
        );
        assertEquals(MultitestingEnvironment.Status.DEPLOYING, sut.getEnvironment(environment.getId()).getStatus());

        pipeTester.runScheduledJobsToCompletion();
        assertEquals(MultitestingEnvironment.Status.READY, sut.getEnvironment(environment.getId()).getStatus());

        sut.cleanup(environment.getId(), "user42");
        assertEquals(MultitestingEnvironment.Status.CLEANUP_TO_IDLE,
            sut.getEnvironment(environment.getId()).getStatus());

        sut.cleanup(environment.getId(), "user43");
        assertEquals(MultitestingEnvironment.Status.CLEANUP_TO_IDLE,
            sut.getEnvironment(environment.getId()).getStatus());

        pipeTester.runScheduledJobsToCompletion();
        assertEquals(MultitestingEnvironment.Status.IDLE, sut.getEnvironment(environment.getId()).getStatus());
    }
}
