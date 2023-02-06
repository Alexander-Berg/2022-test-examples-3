package ru.yandex.market.adv.content.manager.quartz.task;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.tms.quartz2.model.Executor;

public class QuartzTmsTasksTest extends AbstractContentManagerTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("tmsLogsCleanupExecutor")
    private Executor tmsLogsCleanupExecutor;

    @DisplayName("Проверка работоспособности job tmsLogsCleanupExecutor.")
    @Test
    public void tmsLogsCleanupExecutor_writeJobDetails_oneRow() {
        tmsLogsCleanupExecutor.doJob(mockContext());
        jdbcTemplate.query("select * from quartz_job_details",
                (rs) -> {
                    Assertions.assertThat(rs.getString("JOB_NAME"))
                            .isNotNull();
                }
        );
    }
}
