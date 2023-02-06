package ru.yandex.market.logistics.utilizer.controller;

import java.time.Clock;
import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CustomMonitoringControllerTest extends AbstractContextualTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/empty.xml")
    public void logbrokerReadingWhenNotOk() throws Exception {
        clearPartitionOffset();
        insertPartitionOffset(11, "1");
        mockMvc.perform(get("/health/logbroker-reading"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "2;Last successful reading from Logbroker was at 2020-12-14T16:49, more than 10 minutes ago"));
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/empty.xml")
    public void logbrokerReadingWhenOk() throws Exception {
        clearPartitionOffset();
        insertPartitionOffset(11, "1");
        insertPartitionOffset(9, "2");
        mockMvc.perform(get("/health/logbroker-reading"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;ok"));
    }

    private void clearPartitionOffset() {
        jdbcTemplate.update("delete from logbroker.partition_offset");
    }

    private void insertPartitionOffset(int minutesFromNow, String entitySuffix) {
        LocalDateTime localDateTime = LocalDateTime.now(clock).minusMinutes(minutesFromNow);
        jdbcTemplate.update("insert into logbroker.partition_offset " +
                "(entity, updated, partition, lb_offset) values " +
                "(?, ?, 'partition', 123)", "entity" + entitySuffix, localDateTime);
    }
}
