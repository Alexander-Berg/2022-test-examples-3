package ru.yandex.market.tsum.multitesting;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.common.ValidationFailedException;
import ru.yandex.market.tsum.config.MultitestingConfiguration;
import ru.yandex.market.tsum.config.ReleaseConfiguration;
import ru.yandex.market.tsum.dao.PipelinesDao;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipelines.common.jobs.balancer.CreateBalancerJobConfig;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestConfig;
import ru.yandex.market.tsum.pipelines.wood.resources.Wood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestData.defaultEnvironment;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 27.12.2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, ReleaseConfiguration.class,
    MultitestingConfiguration.class, MultitestingTestConfig.class, MockCuratorConfig.class,

})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MultitestingServiceTest {
    private static final String ALREADY_TAKEN_PROJECT = "test";
    private static final String ALREADY_TAKEN_NAME = "already-taken-name";
    private static final String TEST_PROJECT_NAME = "test";

    @Autowired
    private MultitestingService sut;

    @Autowired
    private ProjectsDao projectsDao;

    @Autowired
    private PipelinesDao pipelinesDao;

    @Autowired
    private MultitestingService multitestingService;

    @Autowired
    private PipeTester pipeTester;

    @Before
    public void setUp() {
        when(projectsDao.get(Mockito.anyString())).thenReturn(new ProjectEntity());
        sut.createEnvironment(defaultEnvironment(ALREADY_TAKEN_PROJECT, ALREADY_TAKEN_NAME).build());
    }

    @Test
    public void createsEnvironment_whenCalledWithValidParameters() {
        sut.createEnvironment(new CreateEnvironmentRequest.Builder()
            .withProject(TEST_PROJECT_NAME)
            .withName("n1")
            .withTitle("title1")
            .withType(MultitestingEnvironment.Type.USE_EXISTING_PIPELINE)
            .withAuthor("author1")
            .withPipelineId(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID)
            .withDefaultPipelineResources(Collections.singletonList(new MultitestingEnvironment.DefaultResource(
                new Wood(0, false)
            )))
            .build());

        MultitestingEnvironment environment = sut.getEnvironment(TEST_PROJECT_NAME, "n1");
        assertEquals("test--n1", environment.getId());
        assertEquals(TEST_PROJECT_NAME, environment.getProject());
        assertEquals("n1", environment.getName());
        assertEquals("title1", environment.getTitle());
        assertEquals(MultitestingEnvironment.Type.USE_EXISTING_PIPELINE, environment.getType());
        assertEquals(MultitestingEnvironment.Status.IDLE, environment.getStatus());
        assertEquals(0, environment.getLaunches().size());
        assertEquals("author1", environment.getAuthor());
        assertEquals(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID, environment.getPipelineId());
        assertEquals(1, environment.getDefaultPipelineResources().size());
        assertEquals(Wood.class.getName(), environment.getDefaultPipelineResources().get(0).getClassName());
        assertEquals(
            "{treeCount=0, isConiferous=false}", environment.getDefaultPipelineResources().get(0).getValue().toString()
        );
    }

    @Test
    public void throwsValidationFailed_whenProjectIsInvalid() {
        when(projectsDao.get(null)).thenReturn(null);
        when(projectsDao.get("")).thenReturn(null);
        when(projectsDao.get("project that doesn't exist")).thenReturn(null);
        when(projectsDao.get(TEST_PROJECT_NAME)).thenReturn(new ProjectEntity());
        assertOneValidationError(defaultEnvironment().withProject(null).build());
        assertOneValidationError(defaultEnvironment().withProject("").build());
        assertOneValidationError(defaultEnvironment().withProject("project that doesn't exist").build());
        assertNoValidationErrors(defaultEnvironment().withProject(TEST_PROJECT_NAME).build());
    }

    @Test
    public void throwsValidationFailed_whenNameIsInvalid() {
        assertOneValidationError(defaultEnvironment().withName(null).build());
        assertOneValidationError(defaultEnvironment().withName("").build());

        assertOneValidationError(defaultEnvironment().withName("...").build());
        assertOneValidationError(defaultEnvironment().withName("my--env").build());
        assertOneValidationError(defaultEnvironment().withName(" ").build());
        assertOneValidationError(defaultEnvironment().withName("42abd").build());
        assertOneValidationError(defaultEnvironment().withName("a1-").build());
        assertNoValidationErrors(defaultEnvironment().withName("a1").build());

        int maxNameLength = CreateBalancerJobConfig.MAX_DNS_LABEL_WITHOUT_VHOST_LENGTH
            - MultitestingEnvironment.toId(TEST_PROJECT_NAME, "").length();
        assertOneValidationError(
            defaultEnvironment()
                .withProject(TEST_PROJECT_NAME)
                .withName(StringUtils.repeat("a", maxNameLength + 1))
                .build()
        );
        assertNoValidationErrors(
            defaultEnvironment()
                .withProject(TEST_PROJECT_NAME)
                .withName(StringUtils.repeat("a", maxNameLength))
                .build()
        );

        assertOneValidationError(defaultEnvironment().withName(ALREADY_TAKEN_NAME).build());
    }

    @Test
    public void throwsValidationFailed_whenTypeIsInvalid() {
        assertOneValidationError(defaultEnvironment().withType(null).build());
    }

    @Test
    public void throwsValidationFailed_whenPipelineIdIsInvalid() {
        assertOneValidationError(
            defaultEnvironment()
                .withType(MultitestingEnvironment.Type.USE_EXISTING_PIPELINE)
                .withPipelineId(null)
                .build()
        );

        when(pipelinesDao.exists(Mockito.eq("pipeline-that-does-not-exist"))).thenReturn(false);
        assertOneValidationError(
            defaultEnvironment()
                .withType(MultitestingEnvironment.Type.USE_EXISTING_PIPELINE)
                .withPipelineId("pipeline-that-does-not-exist")
                .build()
        );
        assertOneValidationError(
            defaultEnvironment()
                .withType(MultitestingEnvironment.Type.GENERATE_PIPELINE_FROM_LIST_OF_COMPONENTS)
                .withPipelineId(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID)
                .build()
        );
        assertNoValidationErrors(
            defaultEnvironment()
                .withType(MultitestingEnvironment.Type.USE_EXISTING_PIPELINE)
                .withPipelineId(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID)
                .build()
        );
    }

    @Test
    public void removesOldLaunches() {
        MultitestingEnvironment environment =
            multitestingService.createEnvironment(defaultEnvironment().build());
        for (int i = 0; i < MultitestingService.MAX_LAUNCHES_COUNT; i++) {
            environment = multitestingService.launchMultitestingEnvironment(environment, ResourceContainer.EMPTY,
                "user");
            pipeTester.runScheduledJobsToCompletion();
            environment = multitestingService.cleanup(environment.getId(), "user");
            pipeTester.runScheduledJobsToCompletion();
        }

        assertEquals(MultitestingService.MAX_LAUNCHES_COUNT, environment.getLaunches().size());

        environment = multitestingService.launchMultitestingEnvironment(environment, ResourceContainer.EMPTY, "user");
        pipeTester.runScheduledJobsToCompletion();
        environment = multitestingService.cleanup(environment.getId(), "user");
        pipeTester.runScheduledJobsToCompletion();

        assertEquals(MultitestingService.MAX_LAUNCHES_COUNT, environment.getLaunches().size());
        assertEquals(2, environment.getLaunches().get(0).getNumber());
        assertEquals(MultitestingService.MAX_LAUNCHES_COUNT + 1, environment.getLastLaunch().getNumber());

        environment = multitestingService.launchMultitestingEnvironment(environment, ResourceContainer.EMPTY, "user");

        assertEquals(MultitestingService.MAX_LAUNCHES_COUNT, environment.getLaunches().size());
        assertEquals(3, environment.getLaunches().get(0).getNumber());
        assertEquals(MultitestingService.MAX_LAUNCHES_COUNT + 2, environment.getLastLaunch().getNumber());

    }

    private void assertOneValidationError(CreateEnvironmentRequest request) {
        try {
            sut.createEnvironment(request);
            fail("Expected " + ValidationFailedException.class.getName());
        } catch (ValidationFailedException e) {
            assertEquals(1, e.getValidationErrors().getFieldErrors().size());
        }
    }

    private void assertNoValidationErrors(CreateEnvironmentRequest request) {
        sut.createEnvironment(request);
    }

    /*
        TODO:
            - launchMultitestingEnvironment:
                - добавление cleanup-джобы
                - добавление PipelineEnvironment в ручные ресурсы
                - вытаскивание тикетов из TicketsList
                - дизейбл и прятание джобы очистки (и автодобавленной, и вручную добавленной)
                - добавление MultitestingEnvironmentLaunch в launches
                - активация пайплайна
                - пайплайн не запускается если среды нет или есть, но не IDLE

            - cleanup:
                - ничего не делает если среды нет или есть, но в неправильном статусе
                - если есть джобы очистки, то раздизейбливает, показывает и запускает их и ставит статус CLEANUP
                - если нет джоб очистки, то ставит статус IDLE
     */
}
