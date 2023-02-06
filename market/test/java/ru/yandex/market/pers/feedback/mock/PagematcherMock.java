package ru.yandex.market.pers.feedback.mock;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.pers.feedback.helper.MockMvcAware;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class PagematcherMock extends MockMvcAware {

    public PagematcherMock(WebApplicationContext webApplicationContext) {
        super(webApplicationContext);
    }

    public String getPageMatcher() throws Exception {
        return getPageMatcher(status().is2xxSuccessful());
    }

    public String getPageMatcher(ResultMatcher resultMatcher) throws Exception {
        return mockMvc.perform(get("/pagematch").accept(MediaType.APPLICATION_JSON)).andExpect(resultMatcher)
            .andReturn().getResponse().getContentAsString();
    }
}
