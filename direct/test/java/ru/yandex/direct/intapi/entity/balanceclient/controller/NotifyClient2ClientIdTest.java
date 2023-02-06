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
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyClientParameters;

import static java.util.Collections.singletonList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class NotifyClient2ClientIdTest {

    @Autowired
    private BalanceClientController controller;

    private MockMvc mockMvc;

    private MockHttpServletRequestBuilder requestBuilder;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        requestBuilder = post(BalanceClientServiceConstants.NOTIFY_CLIENT2_PREFIX)
                .contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    public void absentClientIdTest() throws Exception {
        requestBuilder.content("[{ }]");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void nullClientIdTest() throws Exception {
        NotifyClientParameters params = new NotifyClientParameters().withClientId(null);
        requestBuilder.content(toJson(singletonList(params)));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void negativeClientIdTest() throws Exception {
        NotifyClientParameters params = new NotifyClientParameters().withClientId(-1L);
        requestBuilder.content(toJson(singletonList(params)));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isInternalServerError());
    }
}
