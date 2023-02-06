package ru.yandex.market.delivery.transport_manager.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.health.JobLog;
import ru.yandex.market.delivery.transport_manager.repository.mappers.JobDataMapper;

@DbUnitConfiguration(
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionQrtz"}
)
public class JobDataMapperTest extends AbstractContextualTest {

    @Autowired
    private JobDataMapper mapper;

    @Test
    @DatabaseSetup(
        value = "/repository/quartz/qrtz_log.xml",
        connection = "dbUnitDatabaseConnectionQrtz",
        type = DatabaseOperation.CLEAN_INSERT
    )
    void getLastFinishedJob() {
        JobLog lastFinishedJob = mapper.getLastFinishedJob("getMovementConfiguration");
        JobLog expected = new JobLog()
            .setJobName("getMovementConfiguration")
            .setHostName("vla2-8748-b61-vla-market-prod--7d1-25414")
            .setTriggerFireTime(LocalDateTime.parse("2021-06-10T04:39:25.011000"))
            .setJobFinishedTime(LocalDateTime.parse("2021-06-10T04:41:25.011000"))
            .setJobStatus("OK");

        assertThatModelEquals(expected, lastFinishedJob);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/quartz/qrtz_log.xml",
        connection = "dbUnitDatabaseConnectionQrtz",
        type = DatabaseOperation.CLEAN_INSERT
    )
    void getFinishedBetween() {
        List<JobLog> logs = mapper.getJobDataFinishedBetween(
            Set.of("getMovementConfiguration", "refreshTransportationsByConfig"),
            LocalDateTime.parse("2021-06-10T04:37:00"),
            LocalDateTime.parse("2021-06-10T04:38:00")
        );

        JobLog first = new JobLog()
            .setJobName("getMovementConfiguration")
            .setHostName("vla2-8748-b61-vla-market-prod--7d1-25414")
            .setTriggerFireTime(LocalDateTime.parse("2021-06-10T04:34:25.011000"))
            .setJobFinishedTime(LocalDateTime.parse("2021-06-10T04:37:25.011000"))
            .setJobStatus("OK");

        JobLog second = new JobLog()
            .setJobName("refreshTransportationsByConfig")
            .setHostName("vla2-8748-b61-vla-market-prod--7d1-25414")
            .setTriggerFireTime(LocalDateTime.parse("2021-06-10T04:35:25.011000"))
            .setJobFinishedTime(LocalDateTime.parse("2021-06-10T04:37:55.011000"))
            .setJobStatus("OK");

        assertContainsExactlyInAnyOrder(logs, first, second);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/quartz/qrtz_log.xml",
        connection = "dbUnitDatabaseConnectionQrtz",
        type = DatabaseOperation.CLEAN_INSERT
    )
    void getJobData() {
        List<JobLog> logs = mapper.getJobData("getMovementConfiguration", 1);

        JobLog expected = new JobLog()
            .setJobName("getMovementConfiguration")
            .setHostName("vla2-8748-b61-vla-market-prod--7d1-25414")
            .setTriggerFireTime(LocalDateTime.parse("2021-06-10T04:31:25.011000"));
        assertContainsExactlyInAnyOrder(logs, expected);
    }
}
