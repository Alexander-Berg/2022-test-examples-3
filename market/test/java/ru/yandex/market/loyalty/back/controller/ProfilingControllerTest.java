package ru.yandex.market.loyalty.back.controller;

import com.jamonapi.MonitorFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 05.04.17
 */
@TestFor(ProfilingController.class)
public class ProfilingControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BeanWithProfiling classWithProfiling;

    @Test
    public void testProfilingResult() throws Exception {
        MonitorFactory.reset();

        assertThat(getProfile(), not(containsString("<td>BeanWithProfiling.emptyMethod, ms.</td>")));

        classWithProfiling.emptyMethod();

        assertThat(getProfile(), containsString("<td>BeanWithProfiling.emptyMethod, ms.</td>"));
    }

    private String getProfile() throws Exception {
        return mockMvc
                .perform(get("/profiling"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

}
