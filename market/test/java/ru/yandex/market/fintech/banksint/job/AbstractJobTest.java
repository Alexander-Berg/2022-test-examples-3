package ru.yandex.market.fintech.banksint.job;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.fintech.banksint.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractJobTest extends FunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestJobImpl job;


    @Test
    public void testJobDisable() {
        JobExecutionContext mockContext = Mockito.mock(JobExecutionContext.class);
        job.resetExecuted();

        jdbcTemplate.execute("delete from common_property where name = 'TestJobImpl_enabled'");

        job.doJob(mockContext);
        assertTrue(job.isExecuted());

        jdbcTemplate.execute("insert into common_property values ('TestJobImpl_enabled', 'false')");
        job.resetExecuted();

        job.doJob(mockContext);
        assertFalse(job.isExecuted());

        jdbcTemplate.execute("update common_property set value = 'true' where name = 'TestJobImpl_enabled'");
        job.doJob(mockContext);
        assertTrue(job.isExecuted());

    }

}
