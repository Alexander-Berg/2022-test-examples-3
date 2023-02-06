package ru.yandex.market.tsum.api.registry;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.tsum.core.registry.v2.model.SpokResource;
import ru.yandex.market.tsum.core.registry.v2.model.SpokResourcesDao;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class SpokResourceControllerTest {

    private static final Gson GSON = new GsonBuilder().create();

    private static final String RESOURCE_DIRECTORY_PATH = "registry/spokresource/";

    private static final String TEST_RESOURCE_ID = "mdb1rue3tq3jsrsp9nt1";

    @Configuration
    static class TestConfiguration {

        @Bean
        public SpokResourcesDao spokResourcesDao() throws IOException {
            String jsonResource = getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "resource.json");
            SpokResource spokResource = GSON.fromJson(jsonResource, SpokResource.class);

            SpokResourcesDao spokResourcesDao = Mockito.mock(SpokResourcesDao.class);
            Mockito.doReturn(spokResource).when(spokResourcesDao).get("mdb1rue3tq3jsrsp9nt1");

            return spokResourcesDao;
        }
    }

    private MockMvc mockMvc;

    @Autowired
    private SpokResourcesDao spokResourcesDao;

    private SpokResourceController controller;

    @Before
    public void init() {
        controller = new SpokResourceController(spokResourcesDao);
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .build();
    }

    @Test
    public void getResourceOk() throws Exception {
        String jsonResource = getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "resource.json");
        MvcResult result = mockMvc.perform(get("/resource/" + TEST_RESOURCE_ID))
            .andExpect(status().isOk())
            .andReturn();

        String responseResource = result.getResponse().getContentAsString();

        JSONAssert.assertEquals(jsonResource, responseResource, JSONCompareMode.LENIENT);
    }

    @Test
    public void saveResourceOk() throws Exception {
        String jsonResource = getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "resource.json");
        mockMvc.perform(post("/resource")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("utf-8")
                .content(jsonResource))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    public void deleteResourceOk() throws Exception {
        mockMvc.perform(delete("/resource/" + TEST_RESOURCE_ID))
            .andExpect(status().isOk());
    }


    @Test
    public void deleteResourceNotFound() throws Exception {
        mockMvc.perform(delete("/resource/nosuchresource"))
            .andExpect(status().isNotFound());
    }
}
