package ru.yandex.market.ff.controller.health;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.health.cache.ReturnsProblemCache;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReturnsProblemControllerTest  extends MvcIntegrationTest {
    @Autowired
    private ReturnsProblemCache cache;

    @AfterEach
    public void invalidateCache() {
        cache.invalidateCache();
    }

    @Test
    @DatabaseSetup("classpath:controller/returns-problem/empty-simple-transactions.xml")
    public void logbrokerReadingMonitoringForEmptyTable() throws Exception {
        mockMvc.perform(
                        get("/health/returns/logbroker-reading")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("0;ok"));
    }

    @Test
    @DatabaseSetup("classpath:controller/returns-problem/logbroker-reader-correct-before.xml")
    public void logbrokerReadingMonitoringWhenEverythingIsOk() throws Exception {
        mockMvc.perform(
                        get("/health/returns/logbroker-reading")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("0;ok"));
    }

    @Test
    @DatabaseSetup("classpath:controller/returns-problem/logbroker-reader-incorrect-before.xml")
    public void logbrokerReadingMonitoringWhenThereIsAnError() throws Exception {
        mockMvc.perform(
                        get("/health/returns/logbroker-reading")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("2;Last successful reading from Logbroker for lrm-return-event was " +
                        "at 2018-01-01T06:09:38.575643, more than 240 minutes ago"));
    }

}
