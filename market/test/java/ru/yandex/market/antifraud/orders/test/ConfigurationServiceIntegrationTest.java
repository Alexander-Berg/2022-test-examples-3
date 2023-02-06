package ru.yandex.market.antifraud.orders.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.test.annotations.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author dzvyagin
 */
@IntegrationTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConfigurationServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testMockUidsConfiguration() throws Exception {
        mockMvc.perform(
                get("/configuration/find?config=TEST_UIDS_MOCK")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        mockMvc.perform(
                post("/configuration/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parameter\": \"TEST_UIDS_MOCK\", \"config\": {\"enabled\": true, \"endpoints\": [\"/antifraud/detect\"]}}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"parameter\": \"TEST_UIDS_MOCK\", \"config\": {\"enabled\": true, \"endpoints\": [\"/antifraud/detect\"]}}"));
        mockMvc.perform(
                get("/configuration/find?config=TEST_UIDS_MOCK")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"parameter\": \"TEST_UIDS_MOCK\", \"config\": {\"enabled\": true, \"endpoints\": [\"/antifraud/detect\"]}}"));
    }
}
