package ru.yandex.direct.intapi.entity.metrika.model.objectinfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.direct.core.testing.MockMvcCreator;
import ru.yandex.direct.core.testing.steps.PpcdictSteps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.metrika.controller.MetrikaExportObjectInfoController;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaExportObjectInfoIncorrectParametersTest {
    private static final String OBJECT_INFO_CAMPAIGNS = "/metrika-export/object_info/campaigns";

    @Autowired
    private MetrikaExportObjectInfoController controller;

    @Autowired
    private MockMvcCreator mockMvcCreator;

    @Autowired
    private PpcdictSteps ppcdictSteps;

    private MockMvc mockMvc;
    private MockHttpServletRequestBuilder requestBuilder;
    private String lastChangeTime;

    @Before
    public void buildMockMvc() {
        mockMvc = mockMvcCreator.setup(controller).build();
    }

    @Before
    public void prepareRequest() {
        lastChangeTime = ppcdictSteps.getTimestamp()
                .toLocalDateTime()
                .minusMinutes(5)
                .toString();
        requestBuilder = MockMvcRequestBuilders.get(OBJECT_INFO_CAMPAIGNS).contentType(MediaType.APPLICATION_JSON);
        requestBuilder.param("limit", "1");
    }

    @Test
    public void objectInfoForCampaignsNoCidInLastChangeTest() throws Exception {
        requestBuilder.param("time_token", lastChangeTime);
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void objectInfoForCampaignsNegativeCidInLastChangeTest() throws Exception {
        requestBuilder.param("time_token", lastChangeTime + "/-1");
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

}
