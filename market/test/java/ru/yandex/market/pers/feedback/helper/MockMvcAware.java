package ru.yandex.market.pers.feedback.helper;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

public abstract class MockMvcAware implements InitializingBean {
    private final WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;

    public MockMvcAware(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    protected ResultActions performRequest(RequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder)
                .andDo(log());
    }
}
