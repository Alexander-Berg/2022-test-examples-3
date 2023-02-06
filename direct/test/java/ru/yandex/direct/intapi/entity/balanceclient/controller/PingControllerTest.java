package ru.yandex.direct.intapi.entity.balanceclient.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientResponseMatcher.ncAnswerOk;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PingControllerTest {

    @Autowired
    private BalanceClientController controller;

    private MockMvc mockMvc;

    @Before
    public void before() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }


    @Test
    public void checkPostPingController() throws Exception {
        mockMvc
                .perform(post(BalanceClientServiceConstants.PING_PREFIX))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(ncAnswerOk());
    }

    @Test
    public void checkGetPingController() throws Exception {
        mockMvc
                .perform(get(BalanceClientServiceConstants.PING_PREFIX))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(ncAnswerOk());
    }

    @Test
    public void checkPingControllerWithContent() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(BalanceClientServiceConstants.PING_PREFIX)
                .content("some data")
                .contentType(MediaType.TEXT_PLAIN_VALUE);
        mockMvc
                .perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(ncAnswerOk());
    }
}
