package ru.yandex.direct.intapi.entity.connect.controller;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ConnectIdmControllerBase {
    private MockMvc mockMvc;

    @Autowired
    private ConnectIdmRolesController controller;

    protected void initTest() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .defaultResponseCharacterEncoding(UTF_8)
                .build();
    }

    protected String doRequest(MockHttpServletRequestBuilder request, String query) throws Exception {
        request.accept(MediaType.APPLICATION_JSON);
        if (isNotEmpty(query)) {
            List<NameValuePair> valuePairs = URLEncodedUtils.parse(query, UTF_8);
            for (NameValuePair pair : valuePairs) {
                request.param(pair.getName(), pair.getValue());
            }
        }
        ResultActions perform = mockMvc.perform(request);
        perform.andExpect(status().isOk());
        return perform.andReturn().getResponse().getContentAsString();
    }

}
