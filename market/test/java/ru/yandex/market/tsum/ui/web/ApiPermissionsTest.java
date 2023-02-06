package ru.yandex.market.tsum.ui.web;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.tsum.core.auth.TsumUser;
import ru.yandex.market.tsum.dao.PipelinesDao;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.revision.PipeStateRevisionService;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.dao.ReleaseDao;
import ru.yandex.market.tsum.release.dao.ReleaseService;
import ru.yandex.market.tsum.ui.auth.TsumAuthentication;
import ru.yandex.market.tsum.ui.web.pipeline_launch.PipelineLaunchController;
import ru.yandex.market.tsum.ui.web.release.ReleaseActionsController;
import ru.yandex.market.tsum.ui.web.release.ReleaseController;
import ru.yandex.market.tsum.ui.web.release.model.CancelReleaseInput;
import ru.yandex.market.tsum.ui.web.release.model.DangerousReleaseInput;
import ru.yandex.market.tsum.ui.web.release.model.LaunchReleaseInput;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tsum.ui.auth.TsumRolesService.PUBLIC_PROJECTS_ACCESS_ROLE;

@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Ignore
public class ApiPermissionsTest {

    private static final String TEST_RELEASE_ID = "testReleaseId";
    private static final String TEST_PROJECT_ID = "testProjectId";
    private static final String TEST_PIPELINE_ID = "testPipelineId";
    private static final String TEST_PIPE_LAUNCH_ID = "testPipeLaunchId";
    private static final String TEST_JOB_ID = "testJobId";

    private static final String USER = "someUser";
    private static final String WITHOUT_PUBLIC_PROJECTS_ACCESS_ROLE = "withoutPublicProjectAccess";

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ReleaseActionsController releaseActionsController;
    @Autowired
    private PipelineLaunchController pipelineLaunchController;
    @Autowired
    private ReleaseDao releaseDao;
    @Autowired
    private ProjectsDao projectsDao;
    @Autowired
    private ReleaseService releaseService;
    @Autowired
    private PipeLaunchDao pipeLaunchDao;
    @Autowired
    private PipeStateService pipeStateService;
    @Autowired
    private PipelinesDao pipelinesDao;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(
            releaseActionsController,
            pipelineLaunchController
        ).build();
        setupAuthentication();
    }

    @Test
    public void checkApiMethods_checkNotFoundResponse() throws Exception {
        setupReleaseDaoMock(null);
        setupPipeLaunchDaoMock(null);

        checkAllMethodsResponsesWithStatus(status().isNotFound());

        Mockito.verify(releaseDao, Mockito.times(3)).getRelease(TEST_RELEASE_ID);
        Mockito.verify(projectsDao, Mockito.times(1)).get(TEST_PROJECT_ID);
        Mockito.verify(pipeLaunchDao, Mockito.times(5)).getById(TEST_PIPE_LAUNCH_ID);
    }

    @Test
    public void checkApiMethods_expectOkWithPublicAccess() throws Exception {
        List<String> allowedRoles = Collections.singletonList(PUBLIC_PROJECTS_ACCESS_ROLE);
        setupProjectMock(allowedRoles);
        setupPipeLaunchMock(allowedRoles);

        checkAllMethodsResponsesWithStatus(status().isOk());
    }

    void setupProjectMock(List<String> allowedRoles) {
        ProjectEntity projectMock = Mockito.mock(ProjectEntity.class);

        Mockito.when(projectMock.getAllowedRoles())
            .thenReturn(allowedRoles);

        Mockito.when(pipelinesDao.getReleasePipelineIdsByProjectId(TEST_PROJECT_ID))
            .thenReturn(Collections.singletonList(TEST_PIPELINE_ID));

        Release releaseMock = Mockito.mock(Release.class);

        Mockito
            .when(releaseMock.getProjectId())
            .thenReturn(TEST_PROJECT_ID);

        setupReleaseDaoMock(releaseMock);

        Mockito
            .when(projectsDao.get(TEST_PROJECT_ID))
            .thenReturn(projectMock);

        Mockito
            .when(releaseService.launchRelease(any()))
            .thenReturn(releaseMock);
    }

    private void setupPipeLaunchMock(List<String> allowedRoles) {
        PipeLaunch pipeLaunch = Mockito.mock(PipeLaunch.class);
        JobState jobState = Mockito.mock(JobState.class);
        ObjectId pipeLaunchObjectId = Mockito.mock(ObjectId.class);

        Mockito
            .when(pipeLaunchObjectId.toString())
            .thenReturn(TEST_PIPE_LAUNCH_ID);

        Mockito
            .when(pipeLaunch.getId())
            .thenReturn(pipeLaunchObjectId);

        Mockito
            .when(pipeLaunch.getPipeId())
            .thenReturn(TEST_PIPELINE_ID);

        Mockito
            .when(pipeLaunch.getProjectId())
            .thenReturn(TEST_PROJECT_ID);

        Mockito
            .when(jobState.getJobId())
            .thenReturn(TEST_JOB_ID);

        Mockito
            .when(pipeLaunch.getRoles())
            .thenReturn(allowedRoles);

        Mockito
            .when(pipeLaunch.getJobState(TEST_JOB_ID))
            .thenReturn(jobState);

        Mockito
            .when(pipeLaunch.getJobs())
            .thenReturn(ImmutableMap.of(
                TEST_JOB_ID,
                jobState
            ));

        Mockito.when(pipeStateService.recalc(eq(TEST_PIPE_LAUNCH_ID), any()))
            .thenReturn(pipeLaunch);

        setupPipeLaunchDaoMock(pipeLaunch);
    }

    private void setupPipeLaunchDaoMock(PipeLaunch pipeLaunch) {
        Mockito
            .when(pipeLaunchDao.getById(TEST_PIPE_LAUNCH_ID))
            .thenReturn(pipeLaunch);
    }

    private void setupReleaseDaoMock(Release releaseMock) {
        Mockito
            .when(releaseDao.getRelease(TEST_RELEASE_ID))
            .thenReturn(releaseMock);
    }


    void checkAllMethodsResponsesWithStatus(ResultMatcher expectedStatus) throws Exception {
        Stream.of(
            completeReleaseManually(),
            cancelReleaseManually(),
            launchReleaseManually(),
            markAsDangerous(),
            getPipeLaunch(),
            forceSuccess(),
            launchJob(),
            toggleManualTrigger(),
            toggleScheduler()
        )
            .forEach(resultActions -> {
                try {
                    resultActions.andExpect(expectedStatus);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
    }

    private ResultActions toggleScheduler() throws Exception {
        return mockMvc.perform(
            post("/api/pipe/launch/" + TEST_PIPE_LAUNCH_ID + "/job/"
                + TEST_JOB_ID + "/toggleSchedulerConstraint"));
    }

    private ResultActions toggleManualTrigger() throws Exception {
        return mockMvc.perform(
            post("/api/pipe/launch/" + TEST_PIPE_LAUNCH_ID + "/job/" + TEST_JOB_ID + "/toggleManualTrigger"));
    }

    private ResultActions launchJob() throws Exception {
        return mockMvc.perform(
            post("/api/pipe/launch/" + TEST_PIPE_LAUNCH_ID + "/job/" + TEST_JOB_ID + "/launch"));
    }

    private ResultActions getPipeLaunch() throws Exception {
        return mockMvc.perform(
            get("/api/pipe/launch/" + TEST_PIPE_LAUNCH_ID));
    }

    private ResultActions forceSuccess() throws Exception {
        return mockMvc.perform(
            post("/api/pipe/launch/" + TEST_PIPE_LAUNCH_ID + "/job/" + TEST_JOB_ID + "/forceSuccess"));
    }

    ResultActions markAsDangerous() throws Exception {
        return mockMvc.perform(
            post("/api/projects/*/releases/" + TEST_RELEASE_ID + "/dangerous")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DangerousReleaseInput())));
    }

    ResultActions completeReleaseManually() throws Exception {
        return mockMvc.perform(
            post("/api/projects/*/releases/" + TEST_RELEASE_ID + "/complete"));
    }

    ResultActions launchReleaseManually() throws Exception {
        LaunchReleaseInput cancelReleaseInput = new LaunchReleaseInput();
        return mockMvc.perform(
            post("/api/projects/" + TEST_PROJECT_ID + "/releases/launch/" + TEST_PIPELINE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelReleaseInput))
        );
    }

    ResultActions cancelReleaseManually() throws Exception {
        CancelReleaseInput cancelReleaseInput = new CancelReleaseInput();
        return mockMvc.perform(
            post("/api/projects/*/releases/" + TEST_RELEASE_ID + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelReleaseInput))
        );
    }


    void setupAuthentication() {
        Authentication authentication = Mockito.mock(TsumAuthentication.class);
        TsumUser tsumUserMock = Mockito.mock(TsumUser.class);
        Mockito
            .when(authentication.getName())
            .thenReturn(USER);
        Mockito
            .when(authentication.getDetails())
            .thenReturn(tsumUserMock);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito
            .when(securityContext.getAuthentication())
            .thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Configuration
    public static class Config {

        @Bean
        ProjectsDao projectsDao() {
            return Mockito.mock(ProjectsDao.class);
        }

        @Bean
        ReleaseDao releaseDao() {
            return Mockito.mock(ReleaseDao.class);
        }

        @Bean
        PipeLaunchDao pipeLaunchDao() {
            return Mockito.mock(PipeLaunchDao.class);
        }

        @Bean
        AuthorizedEntitiesProvider authorizedEntitiesProvider(ProjectsDao projectsDao,
                                                              ReleaseDao releaseDao,
                                                              PipeLaunchDao pipeLaunchDao) {
            return new AuthorizedEntitiesProvider(projectsDao, releaseDao, pipeLaunchDao);
        }

        @Bean
        ReleaseService releaseService() {
            return Mockito.mock(ReleaseService.class);
        }

        @Primary
        @Bean
        ReleaseController mockedReleaseController() {
            return Mockito.mock(ReleaseController.class);
        }

        @Bean
        SourceCodeService sourceCodeService() {
            return Mockito.mock(SourceCodeService.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        PipeStateService pipeStateService() {
            return Mockito.mock(PipeStateService.class);
        }

        @Bean
        PipeStateRevisionService pipeStateRevisionService() {
            return Mockito.mock(PipeStateRevisionService.class);
        }

        @Bean
        PipelineLaunchController pipelineLaunchController(PipeLaunchDao pipeLaunchDao,
                                                          PipeStateService pipeStateService,
                                                          PipeStateRevisionService stateRevisionService,
                                                          AuthorizedEntitiesProvider entitiesProvider) {
            return new PipelineLaunchController(
                60,
                pipeLaunchDao,
                pipeStateService,
                stateRevisionService,
                entitiesProvider
            );
        }

        @Bean
        PipelinesDao pipelinesDao() {
            return Mockito.mock(PipelinesDao.class);
        }

        @Bean
        ReleaseActionsController releaseActionsController(
            AuthorizedEntitiesProvider entitiesProvider,
            ReleaseService releaseService,
            PipelinesDao pipelinesDao,
            @Qualifier("mockedReleaseController") ReleaseController releaseController,
            SourceCodeService sourceCodeService,
            ObjectMapper objectMapper
        ) {
            return new ReleaseActionsController(
                entitiesProvider,
                releaseService,
                pipelinesDao,
                releaseController,
                sourceCodeService,
                objectMapper
            );
        }

    }
}
