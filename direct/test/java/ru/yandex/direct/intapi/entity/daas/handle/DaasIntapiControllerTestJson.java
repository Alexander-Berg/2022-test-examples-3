package ru.yandex.direct.intapi.entity.daas.handle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.intapi.configuration.IntApiTest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DaasIntapiControllerTestJson {

    @Autowired
    private DaasIntapiController daasIntapiController;

    private MockMvc mockMvc;

    @Before
    public void before() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(daasIntapiController)
                .setMessageConverters(new ProtobufHttpMessageConverter(), new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    public void organizationsRequestTestJsonSuccess() throws Exception {
        mockMvc
                .perform(post("/daas/get_client_item_statuses/json/organizations")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content("{\"external_ids\": [\"id1\"], \"uid\": 123}")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id1")));
    }

    @Test
    public void collectionsJsonRequestTestJsonSuccess() throws Exception {
        mockMvc
                .perform(post("/daas/get_client_item_statuses/json/collections")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content("{\"external_ids\": [\"id1\"], \"uid\": 123}")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id1")));
    }

    @Test
    public void zaboryJsonRequestTestJsonError() throws Exception {
        mockMvc
                .perform(post("/daas/get_client_item_statuses/json/zabory")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content("{\"external_ids\": [\"id1\"], \"uid\": 123}")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }
}
