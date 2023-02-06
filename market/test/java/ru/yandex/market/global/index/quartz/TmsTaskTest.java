package ru.yandex.market.global.index.quartz;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.global.index.BaseFunctionalTest;
import ru.yandex.market.global.index.config.TmsJobsConfig;
import ru.yandex.market.tms.quartz2.model.Executor;

@ContextConfiguration(classes = TmsJobsConfig.class)
public class TmsTaskTest extends BaseFunctionalTest {

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("tmsLogsCleanupExecutor")
    public Executor tmsLogsCleanupExecutor;

    protected JobExecutionContext mockContext() {
        final JobExecutionContext mockContext = Mockito.mock(JobExecutionContext.class);
        final JobDetail mockJobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
        Mockito.when(mockJobDetail.getKey()).thenReturn(new JobKey("test"));
        return mockContext;
    }

    @Test
    public void testTask() {
        tmsLogsCleanupExecutor.doJob(mockContext());
        jdbcTemplate.query("select * from content_tms.qrtz_job_details", (rs) -> {
            System.out.println(rs.getString("JOB_NAME"));
        });
    }

}
