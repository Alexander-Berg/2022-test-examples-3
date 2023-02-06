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
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestConfig;
import ru.yandex.market.tsum.pipelines.wood.resources.Wood;
import ru.yandex.market.tsum.ui.config.ObjectMapperConfig;
import ru.yandex.market.tsum.ui.web.ApiErrorResponse;
import ru.yandex.market.tsum.ui.web.AuthenticationRule;
import ru.yandex.market.tsum.ui.web.multitesting.dto.CreateMultitestingEnvironmentRequestDto;
import ru.yandex.market.tsum.ui.web.multitesting.dto.MultitestingEnvironmentDto;

import java.util.Collections;

import static org.junit.Assert.*;
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
public class MultitestingControllerCreateTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private MockMvc mockMvc;

    private Wood wood = new Wood(0, false);

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
    public void returnsOkAndCreatesEnvironment_whenCalledWithValidBody() throws Exception {
        MultitestingEnvironmentDto responseFromCreate = OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post("/api/multitestings/project/test/environments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new CreateMultitestingEnvironmentRequestDto(
                            "n1",
                            "title1",
                            false,
                            MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
                            Collections.singletonMap(wood.getSourceCodeId(), new Wood(0, false))
                        )))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            MultitestingEnvironmentDto.class
        );
        assertEquals("test--n1", responseFromCreate.getId());
        assertEquals(MultitestingEnvironment.Status.IDLE, responseFromCreate.getStatus());
        assertTrue(multitestingService.environmentExists("test", "n1"));
    }

    @Test
    public void doesNotReturnError_whenManualResourcesListIsNull() throws Exception {
        MultitestingEnvironmentDto responseFromCreate = OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post("/api/multitestings/project/test/environments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new CreateMultitestingEnvironmentRequestDto(
                            "n1",
                            "title1",
                            false,
                            MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
                            null
                        )))
                )
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            MultitestingEnvironmentDto.class
        );
        assertTrue(responseFromCreate.getDefaultPipelineResources().isEmpty());
    }

    @Test
    public void returnsBadRequestAndErrorMessage_whenThereAreValidationErrors() throws Exception {
        ApiErrorResponse response = OBJECT_MAPPER.readValue(
            mockMvc
                .perform(
                    post("/api/multitestings/project/test/environments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new CreateMultitestingEnvironmentRequestDto(
                            "my--env",
                            "title1",
                            false,
                            MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
                            Collections.emptyMap()
                        )))
                )
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString(),
            ApiErrorResponse.class
        );

        assertFalse(multitestingService.environmentExists("p1", "my--env"));

        assertEquals(1, response.getErrors().size());
    }
}
