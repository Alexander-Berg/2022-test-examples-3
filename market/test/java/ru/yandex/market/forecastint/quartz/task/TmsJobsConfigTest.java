package ru.yandex.market.forecastint.quartz.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.forecastint.AbstractFunctionalTest;
import ru.yandex.market.tms.quartz2.model.Executor;

public class TmsJobsConfigTest extends AbstractFunctionalTest {

    @Autowired
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    @Qualifier("tmsLogsCleanupExecutor")
    public Executor tmsLogsCleanupExecutor;

    @Test
    public void testTask() {
        tmsLogsCleanupExecutor.doJob(mockContext());
        namedParameterJdbcTemplate.query("select * from qrtz_job_details", (rs) -> {
            System.out.println(rs.getString("JOB_NAME"));
        });
    }


}
