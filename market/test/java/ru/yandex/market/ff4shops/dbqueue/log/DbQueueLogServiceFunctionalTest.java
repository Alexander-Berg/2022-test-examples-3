package ru.yandex.market.ff4shops.dbqueue.log;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.dbqueue.TaskType;
import ru.yandex.market.ff4shops.dbqueue.dto.SendStrategyToNesuPayload;

public class DbQueueLogServiceFunctionalTest extends FunctionalTest {

    @Autowired
    private DbQueueLogService dbQueueLogService;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    public void init() {
        clock.setFixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneId.systemDefault());
    }

    @Test
    @DbUnitDataSet(after = "DbQueueLogServiceFunctionalTest.testCreateNewLog.after.csv")
    public void testCreateNewLog() {
        dbQueueLogService.createNewLog(
                1L,
                TaskType.SEND_STRATEGY_TO_NESU,
                new SendStrategyToNesuPayload(1L)
        );
    }

    @Test
    @DbUnitDataSet(before = "DbQueueLogServiceFunctionalTest.testLogStart.before.csv",
        after = "DbQueueLogServiceFunctionalTest.testLogStart.after.csv")
    public void testLogStart() {
        dbQueueLogService.logStart(1L, TaskType.SEND_STRATEGY_TO_NESU.name(),
               "payload");
    }

    @Test
    @DbUnitDataSet(before = "DbQueueLogServiceFunctionalTest.testLogFail.before.csv",
            after = "DbQueueLogServiceFunctionalTest.testLogFail.after.csv")
    public void testLogFail() {
        dbQueueLogService.logFail(1L, TaskType.SEND_STRATEGY_TO_NESU.name(),
                "payload", "error message");
    }

    @Test
    @DbUnitDataSet(before = "DbQueueLogServiceFunctionalTest.testLogFinish.before.csv",
            after = "DbQueueLogServiceFunctionalTest.testLogFinish.after.csv")
    public void testLogFinish() {
        dbQueueLogService.logFinish(1L, TaskType.SEND_STRATEGY_TO_NESU.name(),
                "payload", 1);
    }

    @Test
    @DbUnitDataSet(before = "DbQueueLogServiceFunctionalTest.testLogFinallyFailed.before.csv",
            after = "DbQueueLogServiceFunctionalTest.testLogFinallyFailed.after.csv")
    public void testLogFinallyFailed() {
        dbQueueLogService.logFinish(1L, TaskType.SEND_STRATEGY_TO_NESU.name(),
                "payload", 10);
    }

    @Test
    @DbUnitDataSet(before = "DbQueueLogServiceFunctionalTest.testCleanLogs.before.csv",
            after = "DbQueueLogServiceFunctionalTest.testCleanLogs.after.csv")
    public void testCleanLogs() {
        dbQueueLogService.cleanTooOldLogs();
    }

}
