package ru.yandex.market.tsum.ui.web.multitesting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.multitesting.MultitestingService;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.config.MultitestingConfiguration;
import ru.yandex.market.tsum.config.ReleaseConfiguration;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestConfig;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestData;
import ru.yandex.market.tsum.ui.config.ObjectMapperConfig;
import ru.yandex.market.tsum.ui.web.AuthenticationRule;
import ru.yandex.market.tsum.ui.web.multitesting.dto.MultitestingEnvironmentDto;

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
public class MultitestingControllerUnarchiveTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private MockMvc mockMvc;

    @Autowired
    private MultitestingController multitestingController;

    @Autowired
    private MultitestingService multitestingService;

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
    public void returnsOkAndUnarchivesEnvironment() throws Exception {
        multitestingService.cleanupAndArchiveEnvironment(
            multitestingService.createEnvironment(
                MultitestingTestData.defaultEnvironment("test", "n1").build()
            ).getId(),
            "user42"
        );

        MultitestingEnvironmentDto response = OBJECT_MAPPER.readValue(
            mockMvc
                .perform(post("/api/multitestings/project/test/environments/n1/unarchive"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            MultitestingEnvironmentDto.class
        );

        assertEquals(MultitestingEnvironment.Status.IDLE, response.getStatus());
    }

    @Test
    public void returnsNotFound_whenEnvironmentDoesNotExist() throws Exception {
        mockMvc
            .perform(post("/api/multitestings/project/test/environments/n1/unarchive"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void returnsBadRequest_whenEnvironmentIsNotArchived() throws Exception {
        multitestingService.createEnvironment(
            MultitestingTestData.defaultEnvironment("test", "n1").build()
        );

        mockMvc
            .perform(post("/api/multitestings/project/test/environments/n1/unarchive"))
            .andExpect(status().isBadRequest());
    }
}
