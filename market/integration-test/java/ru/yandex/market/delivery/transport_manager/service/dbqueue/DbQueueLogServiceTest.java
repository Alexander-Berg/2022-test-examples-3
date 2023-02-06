package ru.yandex.market.delivery.transport_manager.service.dbqueue;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.TaskType;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.cancel.CancelMovementDto;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnectionDbQueue"})
public class DbQueueLogServiceTest extends AbstractContextualTest {
    @Autowired
    private DbQueueLogService logService;

    @Test
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_create_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateNewLog() {
        updateSequence("dbqueue.task_log", 1);
        logService.createNewLog(1L, TaskType.CANCEL_MOVEMENT, new CancelMovementDto().setTransportationId(1L));
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/after/after_create_new.xml")
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testStarted() {
        clock.setFixed(Instant.parse("2021-09-02T10:20:00.00Z"), ZoneOffset.UTC);
        logService.logStart(1L, "CANCEL_MOVEMENT", null);
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/after/after_start.xml")
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testFailed() {
        clock.setFixed(Instant.parse("2021-09-02T10:21:00.00Z"), ZoneOffset.UTC);
        logService.logFail(1L, "CANCEL_MOVEMENT", null, "error");
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/after/after_start.xml")
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_ok.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testFinishedOk() {
        clock.setFixed(Instant.parse("2021-09-02T10:21:00.00Z"), ZoneOffset.UTC);
        logService.logFinish(1L, "CANCEL_MOVEMENT", null, 1);
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/after/after_start.xml")
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_finally_failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testFinishedFailed() {
        clock.setFixed(Instant.parse("2021-09-02T10:21:00.00Z"), ZoneOffset.UTC);
        logService.logFinish(1L, "CANCEL_MOVEMENT", null, 100);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_start.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testNewWithPayload() {
        updateSequence("dbqueue.task_log", 1);
        clock.setFixed(Instant.parse("2021-09-02T10:20:00.00Z"), ZoneOffset.UTC);
        logService.logStart(1L, "CANCEL_MOVEMENT", "{\"transportationId\":1}");
    }
}
