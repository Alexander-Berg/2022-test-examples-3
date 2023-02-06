package ru.yandex.direct.web.entity.log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.direct.web.configuration.DirectWebTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@DirectWebTest
public class LogControllerTest {
    MockMvc mockMvc;

    @Autowired
    WebApplicationContext context;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void frontendTimings() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/log/frontendTimings")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("{\"timings\": {},"
                        + "\"interfaceEvents\": {\"isDataActual\":false},"
                        + "\"deviceType\":\"desktop\"}")
        )
                .andExpect(status().isOk());
    }
}
