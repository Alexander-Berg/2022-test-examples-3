package ru.yandex.market.tsum.ui.web.multitesting;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.tsum.config.MultitestingConfiguration;
import ru.yandex.market.tsum.config.ReleaseConfiguration;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.multitesting.MultitestingService;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestConfig;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestData;
import ru.yandex.market.tsum.ui.config.ObjectMapperConfig;
import ru.yandex.market.tsum.ui.web.AuthenticationRule;
import ru.yandex.market.tsum.ui.web.multitesting.dto.CreateMultitestingEnvironmentRequestDto;
import ru.yandex.market.tsum.ui.web.multitesting.dto.MultitestingEnvironmentDto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 07.06.2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
    MultitestingController.class, MultitestingConfiguration.class, ReleaseConfiguration.class, TestConfig.class,
    PipeServicesConfig.class, TestMongo.class, MultitestingTestConfig.class, MockCuratorConfig.class,
    MultitestingControllerRestartBuildJobsTest.Config.class,
    ObjectMapperConfig.class,
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MultitestingControllerRestartBuildJobsTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private MockMvc mockMvc;

    @Autowired
    private MultitestingController multitestingController;

    @Autowired
    private MultitestingService multitestingService;

    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Autowired
    private ProjectsDao projectsDao;

    @Rule
    public final AuthenticationRule authenticationRule = new AuthenticationRule();

    @Before
    public void setup() {
        Mockito.when(projectsDao.get(Mockito.anyString())).thenReturn(new ProjectEntity());
        mockMvc = MockMvcBuilders.standaloneSetup(multitestingController).build();
    }

    @Test
    public void whenEnvironmentDoesNotExist_createsAndLaunches() throws Exception {
        MultitestingEnvironmentDto response = OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post("/api/multitestings/project/test/environments/n1/createOrRestart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new CreateMultitestingEnvironmentRequestDto(
                            "n1",
                            null,
                            false,
                            MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
                            Collections.emptyMap()
                        )))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            MultitestingEnvironmentDto.class
        );

        assertEquals("test--n1", response.getId());
        assertEquals(MultitestingEnvironment.Status.DEPLOYING, response.getStatus());
        assertTrue(multitestingService.environmentExists("test", "n1"));

        pipeTester.runScheduledJobsToCompletion();

        assertEquals(MultitestingEnvironment.Status.READY, multitestingService.getEnvironment(response.getId()).getStatus());
    }

    @Test
    public void whenEnvironmentIsIdle_launches() throws Exception {
        multitestingService.createEnvironment(MultitestingTestData.defaultEnvironment("test", "n1").build());

        MultitestingEnvironmentDto response = OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post("/api/multitestings/project/test/environments/n1/createOrRestart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new CreateMultitestingEnvironmentRequestDto(
                            "n1",
                            null,
                            false,
                            MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
                            Collections.emptyMap()
                        )))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            MultitestingEnvironmentDto.class
        );

        assertEquals("test--n1", response.getId());
        assertEquals(MultitestingEnvironment.Status.DEPLOYING, response.getStatus());
        assertTrue(multitestingService.environmentExists("test", "n1"));

        pipeTester.runScheduledJobsToCompletion();

        assertEquals(MultitestingEnvironment.Status.READY, multitestingService.getEnvironment(response.getId()).getStatus());
    }

    @Test
    public void whenEnvironmentIsArchived_unarchivesAndCreatesNewLaunch() throws Exception {
        multitestingService.createEnvironment(MultitestingTestData.defaultEnvironment("test", "n1").build());
        multitestingService.cleanupAndArchiveEnvironment("test", "n1", "user");

        MultitestingEnvironmentDto response = OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post("/api/multitestings/project/test/environments/n1/createOrRestart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new CreateMultitestingEnvironmentRequestDto(
                            "n1",
                            null,
                            false,
                            MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
                            Collections.emptyMap()
                        )))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            MultitestingEnvironmentDto.class
        );

        assertEquals("test--n1", response.getId());
        assertEquals(MultitestingEnvironment.Status.DEPLOYING, response.getStatus());
        assertTrue(multitestingService.environmentExists("test", "n1"));

        pipeTester.runScheduledJobsToCompletion();

        assertEquals(MultitestingEnvironment.Status.READY, multitestingService.getEnvironment(response.getId()).getStatus());
    }

    @Test
    public void whenEnvironmentExistsAndIsReady_restartsBuildJobs() throws Exception {
        multitestingService.launchMultitestingEnvironment(
            multitestingService.createEnvironment(MultitestingTestData.defaultEnvironment("test", "n1").build()),
            ResourceContainer.EMPTY,
            "user"
        );
        pipeTester.runScheduledJobsToCompletion();

        MultitestingEnvironmentDto response = OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post("/api/multitestings/project/test/environments/n1/createOrRestart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new CreateMultitestingEnvironmentRequestDto(
                            "n1",
                            null,
                            false,
                            MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
                            Collections.emptyMap()
                        )))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            MultitestingEnvironmentDto.class
        );

        assertEquals("test--n1", response.getId());
        assertEquals(MultitestingEnvironment.Status.DEPLOYING, response.getStatus());

        PipeLaunch pipeLaunch = pipeLaunchDao.getById(response.getLaunches().get(0).getPipeLaunchId());
        assertEquals(2, pipeLaunch.getJobs().get("build").getLaunches().size());
    }

    @Test
    public void whenEnvironmentExistsAndBuildJobIsNotRunningYet_returns409() throws Exception {
        multitestingService.launchMultitestingEnvironment(
            multitestingService.createEnvironment(MultitestingTestData.defaultEnvironment("test", "n1").build()),
            ResourceContainer.EMPTY,
            "user"
        );

        mockMvc
            .perform(
                post("/api/multitestings/project/test/environments/n1/createOrRestart")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(new CreateMultitestingEnvironmentRequestDto(
                        "n1",
                        null,
                        false,
                        MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
                        Collections.emptyMap()
                    )))
            )
            .andExpect(status().isConflict());
    }

    @Test
    public void whenEnvironmentExistsAndDoesNotHaveBuildJobs_returns500() throws Exception {
        multitestingService.launchMultitestingEnvironment(
            multitestingService.createEnvironment(
                MultitestingTestData.defaultEnvironment("test", "n1")
                    .withPipelineId(Config.PIPE_WITHOUT_BUILD_JOB)
                    .build()
            ),
            ResourceContainer.EMPTY,
            "user"
        );

        mockMvc
            .perform(
                post("/api/multitestings/project/test/environments/n1/createOrRestart")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(new CreateMultitestingEnvironmentRequestDto(
                        "n1",
                        null,
                        false,
                        Config.PIPE_WITHOUT_BUILD_JOB,
                        Collections.emptyMap()
                    )))
            )
            .andExpect(status().isInternalServerError());
    }

    @Test
    public void whenDifferentUrlAndBodyNames_returns400() throws Exception {
        mockMvc
            .perform(
                post("/api/multitestings/project/test/environments/n1/createOrRestart")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(new CreateMultitestingEnvironmentRequestDto(
                        "n2",
                        null,
                        false,
                        MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
                        Collections.emptyMap()
                    )))
            )
            .andExpect(status().isBadRequest());
    }


    @Configuration
    public static class Config {
        static final String PIPE_WITHOUT_BUILD_JOB = "mt-test-without-build-job";

        @Bean(name = PIPE_WITHOUT_BUILD_JOB)
        public Pipeline mtTestCustomCleanupPipeline() {
            PipelineBuilder builder = PipelineBuilder.create();
            JobBuilder deployJob = builder.withJob(DummyJob.class);
            return builder.build();
        }
    }
}
