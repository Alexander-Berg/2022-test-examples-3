package ru.yandex.market.tsum.multitesting;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.config.MultitestingConfiguration;
import ru.yandex.market.tsum.config.ReleaseConfiguration;
import ru.yandex.market.tsum.dao.PipelinesDao;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.multitesting.exceptions.MultitestingEnvironmentCasFailedException;
import ru.yandex.market.tsum.multitesting.exceptions.MultitestingEnvironmentNotFoundException;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestConfig;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestData;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 18.06.2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class, PipeServicesConfig.class, MultitestingConfiguration.class,
    ReleaseConfiguration.class, MultitestingTestConfig.class, MockCuratorConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MultitestingDaoCasTest {
    private static final MultitestingEnvironment.Status NEW_STATUS = MultitestingEnvironment.Status.ARCHIVED;

    @Autowired
    private MultitestingDao multitestingDao;

    @Autowired
    private MultitestingService multitestingService;

    @Autowired
    private PipelinesDao pipelinesDao;

    @Autowired
    private ProjectsDao projectsDao;

    @Before
    public void setUp() {
        Mockito.when(projectsDao.get(Mockito.anyString())).thenReturn(new ProjectEntity());
    }

    @Test
    public void simpleUpdate() {
        MultitestingEnvironment environment = multitestingService.createEnvironment(
            MultitestingTestData.defaultEnvironment().build()
        );

        MultitestingEnvironment updatedEnvironment = multitestingDao.update(
            environment.getId(),
            e -> {
                e.setStatus(NEW_STATUS);
            }
        );

        assertEquals(NEW_STATUS, updatedEnvironment.getStatus());
        assertEquals(1, updatedEnvironment.getRevision());
    }

    @Test
    public void oneCasFailure() {
        MultitestingEnvironment environment = multitestingService.createEnvironment(
            MultitestingTestData.defaultEnvironment().build()
        );

        // Первая попытка зафейлится из-за несовпадения revision, вторая попытка будет успешной
        boolean[] isFirstAttempt = {true};
        MultitestingEnvironment updatedEnvironment = multitestingDao.update(
            environment.getId(),
            e -> {
                if (isFirstAttempt[0]) {
                    multitestingDao.update(environment.getId(), e2 -> {
                    });
                    isFirstAttempt[0] = false;
                }
                e.setStatus(NEW_STATUS);
            }
        );

        assertEquals(NEW_STATUS, updatedEnvironment.getStatus());
        assertEquals(2, updatedEnvironment.getRevision());
    }

    @Test(expected = MultitestingEnvironmentCasFailedException.class)
    public void tooManyCasFailures() {
        MultitestingEnvironment environment = multitestingService.createEnvironment(
            MultitestingTestData.defaultEnvironment().build()
        );

        // Все попытки зафейлятся из-за несовпадения revision
        multitestingDao.update(
            environment.getId(),
            e -> {
                multitestingDao.update(environment.getId(), e2 -> {
                });
            }
        );
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void updaterThrows() {
        MultitestingEnvironment environment = multitestingService.createEnvironment(
            MultitestingTestData.defaultEnvironment().build()
        );

        multitestingDao.update(
            environment.getId(),
            e -> {
                throw new IndexOutOfBoundsException();
            }
        );
    }

    @Test(expected = MultitestingEnvironmentNotFoundException.class)
    public void environmentDoesNotExist() {
        multitestingDao.update(
            "some--id",
            e -> {
            }
        );
    }
}
