package ru.yandex.market.logistics.dbqueue.solomon;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.logistics.dbqueue.SolomonMonitoringController;
import ru.yandex.market.logistics.dbqueue.base.AbstractContextualTest;
import ru.yandex.market.logistics.dbqueue.impl.DbQueueTaskType;
import ru.yandex.market.logistics.dbqueue.metrics.DbQueueMetricsCollector;
import ru.yandex.market.logistics.dbqueue.repository.DbQueueRepository;
import ru.yandex.market.logistics.dbqueue.time.DateTimeServiceInterface;
import ru.yandex.market.logistics.dbqueue.util.FileContentUtils;


@DbUnitConfiguration(databaseConnection = "dbqueueDatabaseConnection")
public class SolomonMonitoringControllerTest extends AbstractContextualTest {


    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DateTimeServiceInterface dateTimeService;

    @Autowired
    DbQueueRepository dbQueueRepository;

    private void insert(DbQueueTaskType queue, String payload, int attempt) {
        jdbcTemplate.update("insert into dbqueue.task (queue_name, payload, created_at, next_process_at, attempt," +
                "total_attempt) " +
                "values (?, ?, now(), now(), ?, ?)", queue.name(), payload, attempt, attempt);
    }

    @Test
    @AutoConfigureMockMvc
    @DatabaseSetup(value = "classpath:fixtures/dbqueue/empty.xml", connection = "dbqueueDatabaseConnection")
    public void test() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new SolomonMonitoringController(new DbQueueMetricsCollector(dbQueueRepository, dateTimeService)))
                .build();
        insert(DbQueueTaskType.TEST_EVENT, "{\"task\":123}", 0);
        insert(DbQueueTaskType.TEST_EVENT, "{\"task\":124}", 11);
        insert(DbQueueTaskType.TEST_EVENT, "{\"task\":125}", 16);
        insert(DbQueueTaskType.TEST_EVENT, "{\"task\":126}", 20);

        MvcResult result = mockMvc.perform(
                MockMvcRequestBuilders.get("/health/solomon/db-queue")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
        )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String expectedJson =
                FileContentUtils.getFileContent("fixtures/solomon/dbqueue-solomon-monitoring.json");

        JSONAssert
                .assertEquals(expectedJson, result.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }
}
