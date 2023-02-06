package ru.yandex.direct.web.configuration.swagger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.direct.web.configuration.TestingDirectWebAppConfiguration;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        TestingDirectWebAppConfiguration.class
})
@WebAppConfiguration
public class SwaggerTestingConfTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestAuthHelper testAuthHelper;

    @Autowired
    WebApplicationContext context;

    @Before
    public void setUp() {
        testAuthHelper.createDefaultUser();
    }

    @Test
    public void emptyPath() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/")).andExpect(status().isNotFound());

    }

    @Test
    public void swaggerHtml() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/docs/swagger-ui.html")).andExpect(status().isOk());
    }

    @Test
    public void docs() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/docs")).andExpect(status().isFound());
    }

    @Test
    public void docsWithSlash() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/docs/")).andExpect(status().isFound());
    }

    @Test
    public void api() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/docs/api")).andExpect(status().isOk());
    }

    @Test
    public void resources() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/docs/swagger-resources")).andExpect(status().isOk());
    }
}
