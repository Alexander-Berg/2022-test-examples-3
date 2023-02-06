package ru.yandex.market.tsum.ui.web.multitesting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
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
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.config.TestZkConfig;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.multitesting.MultitestingService;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.config.MultitestingConfiguration;
import ru.yandex.market.tsum.config.ReleaseConfiguration;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeZkConfiguration;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.WaitingForInterruptOnceJob;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipelines.common.jobs.multitesting.MultitestingTags;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestConfig;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestData;
import ru.yandex.market.tsum.ui.config.ObjectMapperConfig;
import ru.yandex.market.tsum.ui.web.AuthenticationRule;
import ru.yandex.market.tsum.ui.web.multitesting.dto.CreateMultitestingEnvironmentRequestDto;

import java.util.Collections;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 07.06.2018
 */
@Ignore // Флапает, MARKETINFRA-3875
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
    MultitestingController.class, MultitestingConfiguration.class, ReleaseConfiguration.class,
    TestConfig.class, PipeServicesConfig.class, PipeZkConfiguration.class, TestZkConfig.class,
    TestMongo.class, MultitestingTestConfig.class,
    MultitestingControllerRestartBuildJobsTestZookeeperTest.Config.class,
    ObjectMapperConfig.class,
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MultitestingControllerRestartBuildJobsTestZookeeperTest {
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

    @Autowired
    private Semaphore semaphore;

    @Rule
    public final AuthenticationRule authenticationRule = new AuthenticationRule();

    @Before
    public void setup() {
        Mockito.when(projectsDao.get(Mockito.anyString())).thenReturn(new ProjectEntity());
        mockMvc = MockMvcBuilders.standaloneSetup(multitestingController).build();
    }

    @Test(timeout = 120000)
    public void whenEnvironmentExistsAndBuildJobIsRunning_interruptsAndRestartsBuildJob() throws Exception {
        MultitestingEnvironment environment = multitestingService.launchMultitestingEnvironment(
            multitestingService.createEnvironment(
                MultitestingTestData.defaultEnvironment("test", "n1")
                    .withPipelineId(Config.PIPE_WITH_INTERRUPTIBLE_BUILD_JOB)
                    .build()
            ),
            ResourceContainer.EMPTY,
            "user"
        );

        // Запускаем пайплайн в отдельном потоке чтобы иметь возможность что-то делать пока он работает.
        Thread thread = pipeTester.runScheduledJobsToCompletionAsync();

        // Ждём запуска buildJob. Первый запуск buildJob вызывает semaphore.release() и долго висит после этого.
        semaphore.acquire();

        // Пока buildJob работает, дёргаем /createOrRestart. Это убъёт buildJob и запустит её заново.
        mockMvc
            .perform(
                post("/api/multitestings/project/test/environments/n1/createOrRestart")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(new CreateMultitestingEnvironmentRequestDto(
                        "n1",
                        null,
                        false,
                        Config.PIPE_WITH_INTERRUPTIBLE_BUILD_JOB,
                        Collections.emptyMap()
                    )))
            )
            .andExpect(status().isOk());

        // Ждём завершения runPipeToCompletionAsync. Второй запуск buildJob завершится быстро и успешно.
        thread.join();

        // После всех этих махинаций у buildJob должно быть два запуска: один поинтеррапченный, второй успешный.
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(environment.getLastLaunch().getPipeLaunchId());

        assertThat(pipeLaunch.getJobState(Config.INTERRUPTIBLE_BUILD_JOB_ID).getLaunches())
            .extracting(JobLaunch::getLastStatusChangeType)
            .containsExactly(StatusChangeType.INTERRUPTED, StatusChangeType.SUCCESSFUL);
    }


    @Configuration
    public static class Config {
        static final String PIPE_WITHOUT_BUILD_JOB = "mt-test-without-build-job";
        static final String PIPE_WITH_INTERRUPTIBLE_BUILD_JOB = "mt-test-with-interruptible-build-job";
        static final String INTERRUPTIBLE_BUILD_JOB_ID = "INTERRUPTIBLE_BUILD_JOB_ID";

        @Bean(name = PIPE_WITHOUT_BUILD_JOB)
        public Pipeline pipeWithoutBuildJob() {
            PipelineBuilder builder = PipelineBuilder.create();
            builder.withJob(DummyJob.class);
            return builder.build();
        }

        @Bean(name = PIPE_WITH_INTERRUPTIBLE_BUILD_JOB)
        public Pipeline pipeWithInterruptibleBuildJob() {
            PipelineBuilder builder = PipelineBuilder.create();
            builder.withJob(WaitingForInterruptOnceJob.class)
                .withId(INTERRUPTIBLE_BUILD_JOB_ID)
                .withTags(MultitestingTags.BUILD);
            return builder.build();
        }

        @Bean
        public Semaphore semaphore() {
            return new Semaphore(0, true);
        }
    }
}
