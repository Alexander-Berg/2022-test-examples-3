package ru.yandex.market.global.partner.quartz;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.global.partner.executor.DefaultTmsLogsCleanupExecutor;
import ru.yandex.market.javaframework.quartz.test.AbstractQuartzTest;

@ContextConfiguration(classes = DefaultTmsLogsCleanupExecutor.class)
public class TmsTaskTest extends AbstractQuartzTest {

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public DefaultTmsLogsCleanupExecutor defaultTmsLogsCleanupExecutor;

    @Test
    public void testTask() {
        defaultTmsLogsCleanupExecutor.doJob(mockContext());
        jdbcTemplate.query("select * from tms.qrtz_job_details", (rs) -> {
            System.out.println(rs.getString("JOB_NAME"));
        });
    }

}
