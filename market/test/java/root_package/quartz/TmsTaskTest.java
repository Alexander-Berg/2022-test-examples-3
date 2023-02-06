package {root_package}.quartz;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.tms.quartz2.model.Executor;

public class TmsTaskTest extends AbstractQuartzTest {

    @Autowired
    public NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("tmsLogsCleanupExecutor")
    public Executor tmsLogsCleanupExecutor;

    @Test
    public void testTask() {
        tmsLogsCleanupExecutor.doJob(mockContext());
        jdbcTemplate.query("select * from qrtz_job_details", (rs) -> {
            System.out.println(rs.getString("JOB_NAME"));
        });
    }

}
