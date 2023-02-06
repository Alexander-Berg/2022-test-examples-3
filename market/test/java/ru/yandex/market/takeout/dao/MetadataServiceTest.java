package ru.yandex.market.takeout.dao;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.takeout.config.DaoTestConfiguration;
import ru.yandex.market.takeout.domain.job.Job;
import ru.yandex.market.takeout.domain.task.Task;

import static org.junit.Assert.*;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DaoTestConfiguration.class})
public class MetadataServiceTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = "classpath:sql/jobs.sql")
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = "classpath:sql/last_successful_jobs.sql")
    public void getLastSuccessfulJobs() {
        MetadataService metadataService = new MetadataService(jdbcTemplate, jdbcTemplate);
        List<Job> jobs = metadataService.getLastSuccessfulJobs(1000);
        assertEquals(1, jobs.size());
    }

    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = "classpath:sql/jobs.sql")
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = "classpath:sql/tasks.sql")
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = "classpath:sql/failed_jobs.sql")
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = "classpath:sql/last_tasks.sql")
    public void getLastTasks() {
        MetadataService metadataService = new MetadataService(jdbcTemplate, jdbcTemplate);
        List<Task> tasks = metadataService.getLastTasks(2000, 200_000);
        assertEquals(1, tasks.size());
        assertEquals("pers_qa", tasks.get(0).getApiName());
    }
}
