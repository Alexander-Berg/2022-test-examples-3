package ru.yandex.market.hrms.tms.health;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(schema = "public")
public class HrmsHealthControllerTest extends AbstractTmsTest {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DbUnitDataSet(before = "CheckWmsLogsPresence.csv")
    void checkWmsLogsRelevanceIsAlive() throws Exception {
        mockClock(LocalDateTime.parse("2021-02-28 19:00:00", DATE_TIME_FORMATTER));

        mockMvc.perform(MockMvcRequestBuilders.get("/health/check-wms-logs-relevance"))
                .andExpect(status().isOk())
                .andExpect(content().string("2;No wms actions for more than 210 minutes in warehouses RST, SOF"));
    }

    @Test
    @DbUnitDataSet(before = "CheckWmsLogsPresence.csv")
    void checkScLogsRelevanceIsAlive() throws Exception {
        mockClock(LocalDateTime.parse("2021-02-28 19:00:00", DATE_TIME_FORMATTER));

        mockMvc.perform(MockMvcRequestBuilders.get("/health/check-sc-logs-relevance"))
                .andExpect(status().isOk())
                .andExpect(content().string("2;No sc actions for more than 210 minutes in warehouses SOF"));
    }

    @Test
    @DbUnitDataSet(before = "CheckWmsLogsPresence.csv")
    void checkWmsLogsFromClickhouseRelevanceIsAlive() throws Exception {
        mockClock(LocalDateTime.parse("2021-02-28 19:00:00", DATE_TIME_FORMATTER));

        mockMvc.perform(MockMvcRequestBuilders.get("/health/check-wms-clickhouse-logs-relevance"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "1;No wms actions from ClickHouse for more than 180 minutes in warehouses RST, SOF"));
    }

    @Test
    @DbUnitDataSet(before = "CheckWmsLogsPresence.FilteredDomains.csv")
    void checkWmsLogsRelevanceIsAliveWithFilteredDomains() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/health/check-wms-logs-relevance"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;OK"));
    }

    @Test
    @DbUnitDataSet(before = "CheckWmsLogsPresence.FilteredDomains.csv")
    void checkWmsClickhouseLogsRelevanceIsAliveWithFilteredDomains() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/health/check-wms-clickhouse-logs-relevance"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;OK"));
    }

    @Test
    @DbUnitDataSet(before = "CheckWmsLogsPresence.FilteredDomains.csv")
    void checkScLogsRelevanceIsAliveWithFilteredDomains() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/health/check-sc-logs-relevance"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;OK"));
    }
}
