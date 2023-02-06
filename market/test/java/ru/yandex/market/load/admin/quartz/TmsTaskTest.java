package ru.yandex.market.load.admin.quartz;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;


import ru.yandex.market.javaframework.quartz.test.AbstractQuartzTest;
import ru.yandex.market.load.admin.jobs.TmsLogsCleanupExecutor;

@ContextConfiguration(classes = TmsLogsCleanupExecutor.class)
public class TmsTaskTest extends AbstractQuartzTest {

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public TmsLogsCleanupExecutor tmsLogsCleanupExecutor;

    @Test
    public void testTask() {
        tmsLogsCleanupExecutor.execute(mockContext());
        jdbcTemplate.query("select * from qrtz_job_details", (rs) -> {
            System.out.println(rs.getString("JOB_NAME"));
        });
    }

}
