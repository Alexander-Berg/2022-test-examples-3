package ru.yandex.market.transferact.quartz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.javaframework.quartz.test.AbstractQuartzTest;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.transferact.quartz.task.TmsTasks;

@ContextConfiguration(classes = TmsTasks.class)
public class TmsTaskTest extends AbstractQuartzTest {

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("tmsLogsCleanupExecutor")
    public Executor tmsLogsCleanupExecutor;

    public void testTask() {
        tmsLogsCleanupExecutor.doJob(mockContext());
        jdbcTemplate.query("select * from qrtz_job_details", (rs) -> {
            System.out.println(rs.getString("JOB_NAME"));
        });
    }

}
