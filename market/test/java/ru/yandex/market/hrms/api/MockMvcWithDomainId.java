package ru.yandex.market.hrms.api;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class MockMvcWithDomainId {

    public MockMvc mockMvc;

    public ResultActions perform(MockHttpServletRequestBuilder mockHttpServletRequestBuilder) throws Exception {


        mockHttpServletRequestBuilder.header("X-Admin-Roles","ROLE_FUNC_BE_MULTIDOMAIN");


        return mockMvc.perform(mockHttpServletRequestBuilder);
    }
}
