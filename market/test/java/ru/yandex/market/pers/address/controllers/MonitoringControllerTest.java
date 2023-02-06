package ru.yandex.market.pers.address.controllers;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.market.pers.address.services.monitor.Monitor;
import ru.yandex.market.pers.address.util.BaseWebTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MonitoringControllerTest extends BaseWebTest {
    private static final String OK = "0;OK";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Monitor monitor;

    @Test
    void shouldBeOkByDefault() throws Exception {
        assertEquals(OK, regular());
        assertEquals(OK, extrenal());
    }

    @Test
    void shouldHandleRegularError() throws Exception {
        String errorMessage = "something";
        monitor.errorOccurred(Monitor.Type.MARKET_DATASYNC, errorMessage);

        assertThat(regular(), allOf(
            startsWith("2;"),
            containsString(Monitor.Type.MARKET_DATASYNC.getCode()),
            containsString(errorMessage)
        ));

        assertEquals(OK, extrenal());
    }

    @Test
    void shouldHandleExternalError() throws Exception {
        String errorMessage = "something";
        monitor.errorOccurred(Monitor.Type.GEO_CODER, new IllegalArgumentException(errorMessage));

        assertEquals(OK, regular());

        assertThat(extrenal(), allOf(
            startsWith("2;"),
            containsString(Monitor.Type.GEO_CODER.getCode()),
            containsString(errorMessage)
        ));

    }

    @NotNull
    private String regular() throws Exception {
        return mockMvc
            .perform(get("/monitoring/"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    @NotNull
    private String extrenal() throws Exception {
        return mockMvc
            .perform(get("/monitoring/external"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }
}
