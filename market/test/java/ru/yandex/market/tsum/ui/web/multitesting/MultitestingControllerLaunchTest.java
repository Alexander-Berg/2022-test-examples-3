package ru.yandex.market.tsum.ui.web.multitesting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestConfig;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestData;
import ru.yandex.market.tsum.ui.config.ObjectMapperConfig;
import ru.yandex.market.tsum.ui.web.AuthenticationRule;
import ru.yandex.market.tsum.ui.web.multitesting.dto.MultitestingEnvironmentDto;
import ru.yandex.market.tsum.ui.web.release.model.LaunchReleaseInput;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 27.12.2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
    MultitestingController.class, MultitestingConfiguration.class, ReleaseConfiguration.class, TestConfig.class,
    PipeServicesConfig.class, TestMongo.class, MultitestingTestConfig.class, MockCuratorConfig.class,
    ObjectMapperConfig.class,
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MultitestingControllerLaunchTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private MockMvc mockMvc;

    @Autowired
    private MultitestingController multitestingController;

    @Autowired
    private MultitestingService multitestingService;

    @Rule
    public final AuthenticationRule authenticationRule = new AuthenticationRule();

    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private ProjectsDao projectsDao;

    @Before
    public void setup() {
        Mockito.when(projectsDao.get(Mockito.anyString())).thenReturn(new ProjectEntity());
        mockMvc = MockMvcBuilders.standaloneSetup(multitestingController).build();
    }

    @Test
    public void changesStatusToDeployingAndCreatesLaunch() throws Exception {
        multitestingService.createEnvironment(MultitestingTestData.defaultEnvironment("test", "n1").build());

        MultitestingEnvironmentDto response = OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post("/api/multitestings/project/test/environments/n1/launch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new LaunchReleaseInput()))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            MultitestingEnvironmentDto.class
        );

        assertEquals("test--n1", response.getId());
        assertEquals(MultitestingEnvironment.Status.DEPLOYING, response.getStatus());
        assertEquals(1, response.getLaunches().size());
        assertEquals(1, response.getLaunches().get(0).getNumber().intValue());
        assertEquals("user", response.getLaunches().get(0).getTriggeredBy());

        assertEquals(
            MultitestingEnvironment.Status.DEPLOYING,
            multitestingService.getEnvironment("test--n1").getStatus()
        );
    }


    @Test
    public void twoLaunches() throws Exception {
        MultitestingEnvironment environment = multitestingService.launchMultitestingEnvironment(
            multitestingService.createEnvironment(MultitestingTestData.defaultEnvironment("test", "n1").build()),
            ResourceContainer.EMPTY,
            "user"
        );
        pipeTester.runScheduledJobsToCompletion();
        multitestingService.cleanup(environment.getId(), "user");
        pipeTester.runScheduledJobsToCompletion();

        MultitestingEnvironmentDto response = OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post("/api/multitestings/project/test/environments/n1/launch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new LaunchReleaseInput()))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            MultitestingEnvironmentDto.class
        );

        assertEquals(2, response.getLaunches().get(1).getNumber().intValue());
    }


    @Test
    public void returnsConflict_whenThereIsAnActiveLaunchAlready() throws Exception {
        multitestingService.launchMultitestingEnvironment(
            multitestingService.createEnvironment(MultitestingTestData.defaultEnvironment("test", "n1").build()),
            ResourceContainer.EMPTY,
            "user"
        );

        mockMvc
            .perform(
                post("/api/multitestings/project/test/environments/n1/launch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(new LaunchReleaseInput()))
            )
            .andExpect(status().isConflict());
    }


    @Test
    public void returnsNotFound_whenEnvironmentDoesNotExist() throws Exception {
        mockMvc
            .perform(
                post("/api/multitestings/project/test/environments/n1/launch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(new LaunchReleaseInput()))
            )
            .andExpect(status().isNotFound());
    }
}
